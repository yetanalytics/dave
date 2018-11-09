(ns com.yetanalytics.dave.ui.events.db
  (:require [re-frame.core :as re-frame]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.ui.events.nav :as nav]))

;; compose state specs
(s/def ::nav nav/nav-spec)

(def db-state-spec
  (s/keys :opt-un [::nav]))

(re-frame/reg-event-db
 :db/init
 (fn [_ _]
   (println "db init")
   {:foo :bar}))

(re-frame/reg-sub
 :db/debug
 (fn [db]
   db))
