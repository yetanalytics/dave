(ns com.yetanalytics.dave.ui.app.vega
  (:require [cljsjs.vega]))

(defonce loader
  (js/vega.loader. #js {:defaultProtocol "https"}))

(comment
  ;; This is an example of using the vega data loader to get + parse statements
  (-> loader
      (.load "https://demo-lrs.yetanalytics.io/xapi/statements"
             #js {:headers #js {"X-Experience-API-Version" "1.0.3"
                                "Content-Type" "application/json;"
                                "Authorization"
                                (str "Basic "
                                     (js/btoa
                                      (str "<api key>"
                                           ":"
                                           "<api key secret>")))}})
      (.then (fn [data]
               (.log js/console (.read js/vega data #js {:type "json"
                                                         :property "statements"
                                                         })))))
  ;; From a local relative source
  (-> loader
      (.load "/data/kokea/rate_of_completions_16.json")
      (.then (fn [data]
               (.log js/console (.read js/vega data #js {:type "json"
                                                         })))))



  )
