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
                 (conj handler
                       (a/<! (client/request request))))
                (catch js/Error e
                  (let [exi (ex-info "HTTP Error"
                                     {:type ::error
                                      :request request
                                      :handler handler}
                                     e)]
                    (if error-handler
                      (re-frame/dispatch (conj error-handler
                                               exi))
                      (throw exi)))))))))
