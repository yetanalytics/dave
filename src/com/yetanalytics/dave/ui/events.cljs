(ns com.yetanalytics.dave.ui.events
  (:require
   [re-frame.core :as re-frame]
   [com.yetanalytics.dave.ui.events.nav :as nav]
   [com.yetanalytics.dave.ui.events.db :as db]
   [com.yetanalytics.dave.ui.events.dom :as dom]))

(re-frame/reg-event-fx
 ::init ;; initialize the entire application.
 (fn [_ _]
   {:dispatch-n [[::db/init]
                 [::dom/render-app]]}))

;; TODO:
;;   * Routes
