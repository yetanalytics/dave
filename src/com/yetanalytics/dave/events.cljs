(ns com.yetanalytics.dave.events
  (:require
   [re-frame.core :as re-frame]
   [com.yetanalytics.dave.events.db :as db]
   [com.yetanalytics.dave.events.dom :as dom]))

(re-frame/reg-event-fx
 ::init ;; initialize the entire application.
 (fn [_ _]
   {:dispatch-n [[::db/init]
                 [::dom/render-app]]}))

;; TODO:
;;   * Routes
