(ns com.yetanalytics.dave.ui.events.db
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-event-db
 ::init
 (fn [_ _]
   {}))
