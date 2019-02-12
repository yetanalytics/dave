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
   :options (s/keys* :opt-un [::out-chan]))
  :ret channel?)

(defn query
  "Given an LRS data specification, request statements from an LRS.
  Returns a channel that will recieve [:result <statement result obj>] for every
  successful batch of statements, and [:exception <ex-info>] on any error. Will
  continue pulling from the LRS until there is no more link provided in the
  response."
  [{xapi-query :query
    :keys [endpoint
           auth
           more]
    :as lrs-spec}
   & {:keys [out-chan
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
                      xapi-query (assoc :query-params
                                        (xapi-query->params
                                         xapi-query)))
        response-fn (fn [{:keys [status body] :as response}]
                      ;; (println "resp!" (keys response) #_(dissoc response :body))
                      (if (= status 200)
                        (let [body #?(:cljs (js->clj (.parse js/JSON body)
                                                     :keywordize-keys false)
                                      :clj (json/read-str body))]
                          (a/put! out-chan
                                  [:result body]
                                  (if-let [more (get body "more")]
                                    (fn [_]
                                      (do (query (assoc lrs-spec :more more)
                                                 :out-chan
                                                 out-chan
                                                 #?@(:clj [:conn-mgr conn-mgr
                                                           :http-client http-client]))
                                          nil))
                                    (fn [_]
                                      (do
                                        #?(:clj (when-not keep-conn?
                                                  (cm/shutdown-manager conn-mgr)))
                                        (a/close! out-chan)
                                        nil)))))
                        (a/put! out-chan
                                [:exception (ex-info "LRS Request Error"
                                                     {:type ::lrs-request-error
                                                      :response response})]
                                (fn [_]
                                  #?(:clj (when-not keep-conn?
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
                                            (cm/shutdown-manager conn-mgr)))
                                  (a/close! out-chan))))))
    out-chan))
