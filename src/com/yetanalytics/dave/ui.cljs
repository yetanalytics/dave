(ns ^:figwheel-hooks com.yetanalytics.dave.ui
  (:require
   [re-frame.core :refer [dispatch dispatch-sync]]
   [com.yetanalytics.dave.ui.events :as events]
   [com.yetanalytics.dave.ui.subs]
   [com.yetanalytics.dave.ui.views]))

(println "This text is printed from src/com/yetanalytics/dave/ui.cljs. Go ahead and edit it and see reloading in action.")

(defonce bootstrap
  (dispatch-sync [::events/init]))

;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  (dispatch [:com.yetanalytics.dave.ui.events.dom/render-app])
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
