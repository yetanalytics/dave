(ns com.yetanalytics.dave.workbook.data.lrs.client
  (:require [clojure.core.async :as a #?@(:cljs [:include-macros true])]
            [clojure.core.async.impl.protocols]
            [clojure.spec.alpha :as s #?@(:cljs [:include-macros true])]
            [#?(:cljs cljs-http.client
                :clj clj-http.client) :as http]
            #?@(:clj [[clj-http.conn-mgr :as cm]
                      [clj-http.core :as clj-http]
                      [clojure.data.json :as json]])
            #?(:cljs [cljs-http.core :as cljs-http])
            [clojure.string :as cs]
            [xapi-schema.spec.resources :as xsr]
            [com.yetanalytics.dave.workbook.data.lrs :as lrs]
            [com.yetanalytics.dave.util.spec :as su]
            [com.yetanalytics.dave.util.log :as log]

            ))

(defn- channel?
  [x]
  (satisfies? clojure.core.async.impl.protocols/Channel x))

(s/fdef xapi-query->params
  :args (s/cat :xapi-query
               ::lrs/query)
  :ret map?)

(defn xapi-query->params
  "Coerce an xapi query to params for the http client"
  [xapi-query]
  (reduce-kv
   (fn [m k v]
     (assoc m
            (name k)
            ;; special json handling for agent
            (if (= k :agent)
              #?(:cljs (.stringify js/JSON (clj->js v))
                 :clj (json/write-str v))
              v)))
   {}
   xapi-query))

;; Some Special Wrapping for CLJS

#?(:cljs (defn wrap-request
           "Returns a batteries-included HTTP request function coresponding to the given
   core client. See client/request"
           [request]
           (-> request
               http/wrap-accept
               ;; http/wrap-form-params
               ;; http/wrap-multipart-params
               ;; wrap-edn-params
               ;; wrap-edn-response
               ;; wrap-transit-params
               ;; wrap-transit-response
               http/wrap-json-params
               ;; wrap-json-response
               http/wrap-content-type
               http/wrap-query-params
               http/wrap-basic-auth
               ;; wrap-oauth TODO: Support?
               http/wrap-method
               http/wrap-url
               http/wrap-channel-from-request-map
               http/wrap-default-headers)))

#?(:cljs (def request
           (wrap-request cljs-http/request)))
#?(:cljs (defn http-get
           [url & [req]]
           (request (merge req {:method :get :url url}))))

;; Query should take LRS endpoint, auth and query, and return a channel where
;; item is either [:result <statement result>] or [:exception (ex-info ...)].
;;
;; We'd like to make it like a lazy seq where it doesn't try to query until the
;; caller is ready for more

(s/def ::out-chan
  channel?)

(s/def :retry/error-codes
  (s/every pos-int?
           :into #{}
           :kind set?))

(s/def :retry/wait-min
  pos-int?)

(s/def :retry/wait-max
  pos-int?)

(s/def :retry/wait-rate
  pos?)

(s/def :retry/tries
  int?)

(s/def :retry/tries-max
  int?)

(s/def ::retry
  (s/keys :opt-un
          [:retry/error-codes
           :retry/wait
           :retry/wait-min
           :retry/wait-max
           :retry/wait-rate
           :retry/tries
           :retry/tries-max]))

(s/def ::statement-idx
  su/index-spec)

(s/fdef query
  :args
  (s/cat
   :lrs-spec
   (s/keys :req-un [::lrs/endpoint]
           :opt-un [::lrs/auth
                    ::lrs/more
                    ::lrs/query])
   :options (s/keys* :opt-un [::out-chan
                              ::retry
                              ::statement-idx]))
  :ret channel?)

(defn- update-retry
  "Update the retry map for a retry attempt"
  [{:keys [wait
           wait-rate
           wait-max]
    :as retry}]
  (-> retry
      (update :tries inc)
      (assoc :wait
             (let [next-wait (int (* wait-rate
                                     wait))]
               (if (<= next-wait wait-max)
                 next-wait
                 wait-max)))))

(defn- reset-retry
  "Reset the retry to base values"
  [{:keys [wait-min]
    :as retry}]
  (assoc retry
         :wait wait-min
         :tries 0))

(defn query
  "Given an LRS data specification, request statements from an LRS.
  Returns a channel that will recieve [:result <statement result obj>] for every
  successful batch of statements, and [:exception <ex-info>] on any error. Will
  continue pulling from the LRS until there is no more link provided in the
  response. A request that returns no statements will not generate a result"
  [{xapi-query :query
    :keys [endpoint
           auth
           more]
    :as lrs-spec}
   & {:keys [out-chan
             retry
             #?@(:clj [conn-mgr
                       http-client
                       keep-conn?])
             statement-idx]
      :as options
      :or {statement-idx 0
           #?@(:clj [keep-conn? false])}
      }]
  (let [{:keys [error-codes
                wait
                wait-min
                wait-max
                wait-rate
                tries
                tries-max]
         :as retry}
        (merge
         {:error-codes
          ;; default to retrying on transient/gateway errors
          #{408 420 429
            502 503 504}
          :wait 200
          :wait-min 200
          :wait-max 5000
          :wait-rate 1.5
          :tries 0
          :tries-max 10}
         retry)
        out-chan (or out-chan (a/chan))
        url (str endpoint (or more "/xapi/statements"))
        #?@(:clj [conn-mgr (or conn-mgr (cm/make-reuseable-async-conn-manager {}))
                  http-client (or http-client
                                  (clj-http/build-async-http-client
                                   {}
                                   conn-mgr
                                   endpoint
                                   ))])
        req-options (cond-> {:with-credentials? false
                             :headers {"X-Experience-API-Version" "1.0.3"}
                             #?@(:clj [:async? true
                                       :connection-manager conn-mgr
                                       :http-client http-client])}
                      auth (assoc :basic-auth
                                  #?(:cljs (select-keys auth [:username :password])
                                     :clj ((juxt :username :password) auth)))
                      ;; If we are not using the more link, add params
                      (not more) (assoc :query-params
                                        (merge (when xapi-query
                                                 (xapi-query->params
                                                  xapi-query))
                                               {"ascending" true})))
        _ (log/infof "LRS query req - url: %s opts: %s"
                     url (:query-params req-options {}))
        shutdown-fn (fn [& _]
                      (log/infof "LRS query shutdown - uri: %s"
                                 url)
                      #?(:clj (when-not keep-conn?
                                ;; (println "shutting down, no more link")
                                (cm/shutdown-manager conn-mgr)))
                      (a/close! out-chan)
                      nil)
        ;; error fn gets an ex-info with a :status key, and retries/kills accordingly.
        ;; Designed for common operation between clj-http and cljs-http
        error-fn (fn [exi]
                   (let [{:keys [status] :as exd} (ex-data exi)]
                     (log/warnf "LRS query req error: %s" exd)
                     ;; if this isn't a transient error, and we haven't reached our
                     ;; retry limit, attempt the request again
                     (if (and (contains? error-codes status)
                              (< tries tries-max))
                       ;; wait and retry
                       (do
                         (log/warnf "LRS query req error is transient, status: %d"
                                    status)
                         (log/warnf "LRS query retry in %d ms..."
                                    wait)
                         (a/take! (a/timeout wait)
                                  (fn [_]
                                    (log/warn "Retrying query...")
                                    (query lrs-spec
                                           :out-chan out-chan
                                           :statement-idx statement-idx
                                           :retry
                                           (update-retry
                                            retry)
                                           #?@(:clj [:keep-conn? keep-conn?
                                                     :conn-mgr conn-mgr
                                                     :http-client http-client])))))
                       ;; If this is another error or we are out of tries,
                       ;; fail out + shut down
                       (do
                         (log/fatalf "LRS query req error fatal: %s - %s"
                                     (ex-message exi)
                                     (ex-data exi))
                         (a/put! out-chan
                               [:exception exi]
                               shutdown-fn)))))
        response-fn (fn [{:keys [status body] :as response}]
                      (log/infof "LRS query resp status: %d" status)
                      (if (= status 200)
                        (let [body #?(:cljs (js->clj (.parse js/JSON body)
                                                     :keywordize-keys false)
                                      :clj (json/read-str body))]
                          (log/infof "LRS query resp statements: %d" (count (get body "statements")))
                          (if (not-empty (get body "statements"))
                            (let [s-idx-range [statement-idx
                                               (+ statement-idx
                                                  (dec (count (get body "statements"))))]
                                  body (vary-meta body assoc ::statement-idx-range s-idx-range)]
                                (a/put! out-chan
                                        [:result body]
                                        (if-let [more (get body "more")]
                                          (do
                                            (log/infof "More link found: %s"
                                                       more)
                                            (fn [_]
                                              (do (query (-> lrs-spec
                                                             ;; use the more link
                                                             (assoc :more more)
                                                             ;; remove query params,
                                                             ;; as these are included
                                                             (dissoc :query)
                                                             )
                                                         :out-chan
                                                         out-chan
                                                         :statement-idx (inc (second s-idx-range))
                                                         :retry (reset-retry retry)
                                                         #?@(:clj [:keep-conn? keep-conn?
                                                                   :conn-mgr conn-mgr
                                                                   :http-client http-client]))
                                                  nil)))
                                          shutdown-fn)))
                            (shutdown-fn)))
                        (error-fn
                         ;; make sure there's a status in the exi
                         ;; so it lines up with clj-http
                         (ex-info "LRS Request Error"
                                  {:type ::lrs-request-error
                                   :status status}))))]
    ;; (println "req!" #_req-options)
    #?(:cljs (a/take!
              (http-get
               url
               req-options)
              response-fn)
       :clj (http/get url
                      req-options
                      response-fn
                      error-fn))
    out-chan))

;; Like query, but polls for new statements using since
(s/def ::poll-min ;; ms
  pos-int?)

(s/def ::poll-max ;; ms
  pos-int?)

(s/def ::poll-rate ;; multiplier on empty result
  pos?)

(s/def ::stop-chan
  channel?)

(s/fdef query-poll
  :args
  (s/cat
   :lrs-spec
   (s/keys :req-un [::lrs/endpoint]
           :opt-un [::lrs/auth
                    ;; ::lrs/more
                    ::lrs/query])
   :options (s/keys* :opt-un
                     [::out-chan
                      ::stop-chan
                      ::poll-min
                      ::poll-max
                      ::poll-rate]))
  :ret (s/tuple channel? channel?))

(defn- increase-poll-interval
  [poll-interval
   poll-rate
   poll-max]
  (let [next-interval (int (* poll-rate
                              poll-interval))]
    (if (<= next-interval poll-max)
      next-interval
      poll-max)))

(defn query-poll
  "Like query, but polls for new statements with the given parameters.
  Returns a tuple of [out-chan stop-chan], where stop-chan can be used to abort."
  [{xapi-query :query
    :keys [endpoint
           auth
           more]
    :as lrs-spec}
   & {:keys [out-chan
             stop-chan
             poll-min
             poll-max
             poll-rate
             statement-idx
             #?@(:clj [conn-mgr
                       http-client
                       keep-conn?])]
      :as options
      :or {statement-idx 0
           poll-min 200
           poll-max 5000
           poll-rate 1.5
           #?@(:clj [keep-conn? false])}
      }]
  (let [out-chan (or out-chan (a/chan))
        stop-chan (or stop-chan (a/promise-chan))
        #?@(:clj [conn-mgr (or conn-mgr (cm/make-reuseable-async-conn-manager {}))
                  http-client (or http-client
                                  (clj-http/build-async-http-client
                                   {}
                                   conn-mgr
                                   endpoint
                                   ))])
        shutdown-fn (fn [& _]
                      #?(:clj (when-not keep-conn?
                                (cm/shutdown-manager conn-mgr)))
                      (a/close! out-chan))]
    (a/go-loop [result-chan (query lrs-spec
                                   :statement-idx statement-idx
                                   #?@(:clj [:keep-conn? true
                                             :conn-mgr conn-mgr
                                             :htt-client http-client]))
                poll-interval poll-min
                last-stored nil
                last-statement-idx nil
                ]
      ;; (println "loop" poll-interval last-stored)
      (if-let [[tag body] (a/<! result-chan)]
        (case tag
          :result
          ;; In this case, the body is a statement result, and should have data
          ;; we get the last stored time before passing it on...
          (do
            ;; (println "result")
            (a/>! out-chan [:result body])
            (recur result-chan
                   poll-min ;; reset the poll interval when we have data!
                   (-> body
                       (get "statements")
                       peek
                       (get "stored"))
                   (second (::statement-idx-range (meta body)))))
          :exception
          ;; On exceptions, we stop, output the exception, and shut down.
          (do ;; (println "error" body)
              (a/>! out-chan [:exception body])
              (shutdown-fn)))
        ;; If there's no result, we've reached the end of a query
        ;; We want to wait a little and retry, unless we've been stopped.
        (let [_ (log/infof "Looking for more statements in %d ms..."
                           poll-interval)
              poll-timeout (a/timeout poll-interval)
              [v p] (a/alts! [stop-chan poll-timeout])]
          (if (= p stop-chan)
            ;; user abort
            (do (log/warn "Polling aborted by user!")
             (shutdown-fn))
            ;; timeout complete, update timing, query, recur
            (do
              (recur (query (cond-> lrs-spec
                              last-stored
                              (assoc-in [:query :since] last-stored))
                            :statement-idx (inc (or last-statement-idx
                                                    statement-idx))
                            #?@(:clj [:keep-conn? true
                                      :conn-mgr conn-mgr
                                      :htt-client http-client]))
                     (increase-poll-interval
                      poll-interval poll-rate poll-max)
                     last-stored
                     last-statement-idx))))))
    [out-chan stop-chan]))

(comment
  (def s-chan (query {:endpoint "http://localhost:9001"
                      :auth {:username "123456789"
                             :password "123456789"}}
                     #_:out-chan
                     #_(a/chan 1 (mapcat (fn [[_ sr]]
                                           (get sr "statements"))))))
  (a/take! s-chan (fn [[_ s]]
                    (println (meta s)
                             )))

  (let [[scp stop] (query-poll
                    {:endpoint "http://localhost:9001"
                     :auth {:username "123456789"
                            :password "123456789"}}
                    )]
    (def s-chan-poll scp)
    (def stop-chan stop))
  (a/put! stop-chan true)
  (a/take! s-chan-poll (fn [[_ s]]
                         (println (meta s)
                                  )))


  )
