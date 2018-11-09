(ns com.yetanalytics.dave.ui.events
  (:require
   [re-frame.core :as re-frame]
   [com.yetanalytics.dave.ui.events.nav :as nav]
   [com.yetanalytics.dave.ui.events.db :as db]
   [com.yetanalytics.dave.ui.events.dom :as dom]))

(defn init!
  "Synchronously initialize the application:
    * Initialize/deserialize the DB
    * Start the nav listener
    * Render the application"
  [instance-id]
  (re-frame/dispatch-sync [:db/init instance-id])
  (re-frame/dispatch-sync [:nav/init])
  (re-frame/dispatch-sync [:dom/render-app]))
