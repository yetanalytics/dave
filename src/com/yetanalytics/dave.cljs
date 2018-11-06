(ns ^:figwheel-hooks com.yetanalytics.dave
  (:require
   [re-frame.core :refer [dispatch dispatch-sync]]
   [com.yetanalytics.dave.events :as events]
   [com.yetanalytics.dave.subs]
   [com.yetanalytics.dave.views]))

(println "This text is printed from src/com/yetanalytics/dave.cljs. Go ahead and edit it and see reloading in action.")

(defn multiply [a b] (* a b))

(defonce bootstrap
  (dispatch-sync [::events/init]))

;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  (dispatch [:com.yetanalytics.dave.events.dom/render-app])
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
