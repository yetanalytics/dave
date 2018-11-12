(ns com.yetanalytics.dave.ui.app.debug
  (:require [clojure.spec.alpha :as s]
            [re-frame.core :as re-frame]))

(s/def ::expand? boolean?)

(def debug-state-spec
  (s/keys :opt-un [::expand?]))

(re-frame/reg-event-db
 :debug/toggle!
 (fn [db _]
   (update-in db [:debug :expand?] not)))

(re-frame/reg-sub
 ::state
 (fn [db _]
   (:debug db)))

(re-frame/reg-sub
 :debug/expand?
 (fn [_ _]
   (re-frame/subscribe [::state]))
 (fn [state _]
   (:expand? state false)))
