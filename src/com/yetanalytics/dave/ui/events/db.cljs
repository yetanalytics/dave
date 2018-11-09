(ns com.yetanalytics.dave.ui.events.db
  "Handle top-level app state & persistence"
  (:require [re-frame.core :as re-frame]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.ui.events.nav :as nav]
            [cognitect.transit :as t])
  (:import [goog.storage Storage]
           [goog.storage.mechanism HTML5LocalStorage]))

;; Persistence
(defonce w (t/writer :json))
(defonce r (t/reader :json))

(defonce storage
  (delay (Storage. (HTML5LocalStorage.))))

(defn storage-get
  "Get a value from storage, if it exists deserialize it from transit."
  [^Storage storage k]
  (when-let [data-str (.get storage k)]
    (t/read r data-str)))

(defn storage-set
  "Set a value in storage, serializing it to transit."
  [^Storage storage k v]
  (.set storage k (t/write w v)))

(defn storage-remove
  "Remove a value from storage"
  [^Storage storage k]
  (.remove storage k))

;; compose state specs
(s/def ::nav nav/nav-spec)

(def db-state-spec
  (s/keys :opt-un [::nav]))

(re-frame/reg-event-db
 :db/init
 (fn [_ _]
   {:foo :bar}))

(re-frame/reg-sub
 :db/debug
 (fn [db]
   db))
