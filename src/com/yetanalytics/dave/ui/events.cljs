(ns com.yetanalytics.dave.ui.events
  (:require
   [re-frame.core :as re-frame]
   [com.yetanalytics.dave.ui.app.nav :as nav]
   [com.yetanalytics.dave.ui.app.db :as db]
   [com.yetanalytics.dave.ui.app.dom :as dom]
   [com.yetanalytics.dave.ui.app.notify]
   [com.yetanalytics.dave.ui.app.workbook]))

(defn init!
  "Synchronously initialize the application:
    * Initialize/deserialize the DB
    * Start the nav listener
    * Render the application"
  [instance-id]
  (re-frame/dispatch-sync [:db/init instance-id])
  (re-frame/dispatch-sync [:nav/init])
  (re-frame/dispatch-sync [:dom/render-app]))
