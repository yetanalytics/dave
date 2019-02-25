(ns com.yetanalytics.dave.ui.events
  (:require
   [re-frame.core :as re-frame]
   [com.yetanalytics.dave.ui.app.nav :as nav]
   [com.yetanalytics.dave.ui.app.db :as db]
   [com.yetanalytics.dave.ui.app.dom :as dom]
   [com.yetanalytics.dave.ui.app.http]
   [com.yetanalytics.dave.ui.app.notify]
   [com.yetanalytics.dave.ui.app.workbook]
   [com.yetanalytics.dave.ui.app.workbook.data]
   [com.yetanalytics.dave.ui.app.workbook.question]
   [com.yetanalytics.dave.ui.app.workbook.question.func]
   [com.yetanalytics.dave.ui.app.workbook.question.visualization]
   [com.yetanalytics.dave.ui.app.crud]
   [com.yetanalytics.dave.ui.app.dialog]
   [com.yetanalytics.dave.ui.app.picker]))

(defn init!
  "Synchronously initialize the application:
    * Initialize/deserialize the DB
    * Start the nav listener
    * Render the application"
  [instance-id]
  (re-frame/dispatch-sync [:db/init instance-id])
  (re-frame/dispatch-sync [:nav/init])
  (re-frame/dispatch-sync [:dom/render-app]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility Handlers

;; dispatch debounce on trailing edge
(defonce timeouts
  (atom {}))

(defn find-or-replace-timeout [timeouts id new-timeout]
  (when-let [extant-timeout (get timeouts id)]
    (js/clearTimeout extant-timeout))
  (assoc timeouts id new-timeout))

(re-frame/reg-fx :dispatch-debounce
                 (fn [[id event-vec n]]
                   (swap! timeouts find-or-replace-timeout id
                          (js/setTimeout (fn []
                                           (re-frame/dispatch event-vec)
                                           (swap! timeouts dissoc id))
                                         n))))

(defn stop-timeout [timeouts id]
  (when-let [extant-timeout (get timeouts id)]
    (js/clearTimeout extant-timeout))
  (dissoc timeouts id))

(re-frame/reg-fx :stop-debounce
                 (fn [id]
                   (swap! timeouts stop-timeout id)))
