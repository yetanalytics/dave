(ns com.yetanalytics.dave.ui.app.http
  (:require [re-frame.core :as re-frame]
            [cljs-http.client :as client]
            [clojure.core.async :as a :include-macros true]
            clojure.string
            cljs-http.util))

(defn json-decode-str-keys
  "JSON decode an object from `s`."
  [s]
  (let [v (if-not (clojure.string/blank? s) (js/JSON.parse s))]
    (when (some? v)
      (js->clj v :keywordize-keys false))))

(re-frame/reg-fx
 :http/request
 (fn [{:keys [request
              handler ;; partial handler vector
              error-handler]}]
   (a/go (with-redefs [cljs-http.util/json-decode
                       json-decode-str-keys]
           (try (re-frame/dispatch
                 (let [{:keys [success
                               status] :as resp} (a/<! (client/request request))]
                   ;; single-callback invocation
                   (if (or success (nil? error-handler))
                     (conj handler resp)
                     (conj error-handler
                           (ex-info "HTTP Error Response"
                                    {:type ::error-response
                                     :request request
                                     :response resp
                                     :handler handler
                                     :error-handler error-handler})))))
                (catch js/Error e
                  (let [exi (ex-info "Client Error"
                                     {:type ::client-error
                                      :request request
                                      :handler handler
                                      :error-handler error-handler}
                                     e)]
                    (if error-handler
                      (re-frame/dispatch (conj error-handler
                                               exi))
                      (throw exi)))))))))
