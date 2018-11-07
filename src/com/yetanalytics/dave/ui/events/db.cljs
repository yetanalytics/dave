(ns com.yetanalytics.dave.ui.events.db
  (:require [re-frame.core :as re-frame]
            [clojure.spec.alpha :as s]))

(re-frame/reg-event-db
 ::init
 (fn [db _]
   (or db {})))
