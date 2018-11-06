(ns com.yetanalytics.dave.events.db
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-event-db
 ::init
 (fn [_ _]
   {}))
