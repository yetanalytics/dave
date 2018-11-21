(ns ^:figwheel-hooks com.yetanalytics.dave.ui.core
  "Entrypoint for DAVE interactive docs/workbooks.
  The convention for re-frame handlers/subs is:
    * Public/API handlers - :<short name>/<event/sub>, ie. :nav/dispatch
    * Private handlers - :<fq ns>/<event/fx/cofx>"
  (:require
   [cljsjs.vega] ;; vega
   [re-frame.core :refer [dispatch dispatch-sync]]
   [com.yetanalytics.dave.ui.events :as events]
   [com.yetanalytics.dave.ui.subs]
   [com.yetanalytics.dave.ui.views]))

(println "This text is printed from src/com/yetanalytics/dave/ui.cljs. Go ahead and edit it and see reloading in action.")

;; The instance ID identifies this instance of the app.
;; If multiple tabs are run in the same browser, they will have different ids.
(defonce instance-id
  (random-uuid))

(defonce bootstrap
  (events/init! instance-id))

;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  (dispatch [:dom/render-app])
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )

(when ^boolean goog.DEBUG
  (require '[dave.debug]))
