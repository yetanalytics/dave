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

(s/fdef query
  :args
  (s/cat
   :lrs-spec
   (s/keys :req-un [::lrs/endpoint]
           :opt-un [::lrs/auth
                    ::lrs/more
                    ::lrs/query])
   :options (s/keys* :opt-un [::out-chan
                              ]))
  :ret channel?)

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
             close-chan?
             #?@(:clj [conn-mgr
                       http-client
                       keep-conn?])]
      :as options
      #?@(:clj [:or {keep-conn? false}])}]
  (let [out-chan (or out-chan (a/chan))
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
        ;; _ (do (println "req " url " " (:query-params req-options {})) (flush))
        response-fn (fn [{:keys [status body] :as response}]
                      ;; (println "resp " status) (flush)
                      (if (= status 200)
                        (let [body #?(:cljs (js->clj (.parse js/JSON body)
                                                     :keywordize-keys false)
                                      :clj (json/read-str body))]
                          ;; (println "statements: " (count (get body "statements"))) (flush)
                          (if (not-empty (get body "statements"))
                            (a/put! out-chan
                                    [:result body]
                                    (if-let [more (get body "more")]
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
                                                   #?@(:clj [:keep-conn? keep-conn?
                                                             :conn-mgr conn-mgr
                                                             :http-client http-client]))
                                            nil))
                                      (fn [_]
                                        (do
                                          #?(:clj (when-not keep-conn?
                                                    ;; (println "shutting down, no more link")
                                                    (cm/shutdown-manager conn-mgr)))
                                          (a/close! out-chan)
                                          nil))))
                            (do #?(:clj (when-not keep-conn?
                                          ;; (println "shutting down, empty statement result")
                                          (cm/shutdown-manager conn-mgr)))
                                (a/close! out-chan))))
                        (a/put! out-chan
                                [:exception (ex-info "LRS Request Error"
                                                     {:type ::lrs-request-error
                                                      :response response})]
                                (fn [_]
                                  #?(:clj (when-not keep-conn?
                                            ;; (println "shutting down, bad status")
                                            (cm/shutdown-manager conn-mgr)))
                                  (a/close! out-chan)))))
        ]
    ;; (println "req!" #_req-options)
    #?(:cljs (a/take!
              (http-get
               url
               req-options)
              response-fn)
       :clj (http/get url
                      req-options
                      response-fn
                      (fn [err]
                        (a/put! out-chan
                                [:exception (ex-info "LRS Request Error"
                                                     {:type ::lrs-request-error
                                                      }
                                                     err)]
                                (fn [_]
                                  #?(:clj (when-not keep-conn?
                                            (println "shutting down")
                                            (cm/shutdown-manager conn-mgr)))
                                  (a/close! out-chan))))))
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
                    ::lrs/more
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
             #?@(:clj [conn-mgr
                       http-client
                       keep-conn?])]
      :as options
      :or {poll-min 200
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
        shutdown-fn (fn []
                      #?(:clj (when-not keep-conn?
                                (cm/shutdown-manager conn-mgr)))
                      (a/close! out-chan))]
    (a/go-loop [;; phase :init ;; :receive, :poll
                result-chan (query lrs-spec
                                   #?@(:clj [:keep-conn? true
                                             :conn-mgr conn-mgr
                                             :htt-client http-client]))
                poll-interval poll-min
                last-stored nil
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
                       (get "stored"))))
          :exception
          ;; On exceptions, we stop, output the exception, and shut down.
          (do ;; (println "error" body)
              (a/>! out-chan [:exception body])
              (shutdown-fn)))
        ;; If there's no result, we've reached the end of a query
        ;; We want to wait a little and retry, unless we've been stopped.
        (let [;; _ (println "waiting " poll-interval " ms...")
              poll-timeout (a/timeout poll-interval)
              [v p] (a/alts! [stop-chan poll-timeout])]
          (if (= p stop-chan)
            ;; user abort
            (shutdown-fn)
            ;; timeout complete, update timing, query, recur
            (do
              ;; (println "retrying...")
              (recur (query (cond-> lrs-spec
                              last-stored
                              (assoc-in [:query :since] last-stored))
                            #?@(:clj [:keep-conn? true
                                      :conn-mgr conn-mgr
                                      :htt-client http-client]))
                     (increase-poll-interval
                      poll-interval poll-rate poll-max)
                     last-stored))))))
    [out-chan stop-chan]))
