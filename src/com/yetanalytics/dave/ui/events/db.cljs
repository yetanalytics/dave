(ns com.yetanalytics.dave.ui.events.db
  "Handle top-level app state & persistence"
  (:require [re-frame.core :as re-frame]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.ui.events.nav :as nav]
            [cognitect.transit :as t]
            [com.yetanalytics.dave.workbook :as workbook])
  (:import [goog.storage Storage]
           [goog.storage.mechanism HTML5LocalStorage]))

;; Persistence
(defonce w (t/writer :json))
(defonce r (t/reader :json))

(defonce storage
  (Storage. (HTML5LocalStorage.)))

(defn storage-get
  "Get a value from storage, if it exists deserialize it from transit."
  [k]
  (when-let [data-str (.get storage k)]
    (t/read r data-str)))

(defn storage-set
  "Set a value in storage, serializing it to transit."
  [k v]
  (.set storage k (t/write w
                           v)))

(defn storage-remove
  "Remove a value from storage"
  [k]
  (.remove storage k))

;; compose state specs
(s/def ::id uuid?)
(s/def ::nav nav/nav-spec)
(s/def ::workbooks
  (s/map-of ::workbook/id
            workbook/workbook-spec))

(def db-state-spec
  (s/keys :opt-un [::id
                   ::nav
                   ::workbooks]))

;; This will include the default workbooks for DAVE
(def db-default
  {:workbooks {#uuid "f1d0bd64-0868-43ec-96c6-a51c387f5fc8"
               {:id #uuid "f1d0bd64-0868-43ec-96c6-a51c387f5fc8"
                :title "Test Workbook"
                :description "A dummy workbook for dev/testing"
                :questions {#uuid "344d1296-bb19-43f5-92e5-ceaeb7089bb1"
                            {:id #uuid "344d1296-bb19-43f5-92e5-ceaeb7089bb1"
                             :text "What makes a good automaton?"
                             :visualizations
                             {#uuid "c9d0e0c2-3d40-4c5d-90ab-5a482588459f"
                              {:id #uuid "c9d0e0c2-3d40-4c5d-90ab-5a482588459f"}}}}}}})

(re-frame/reg-cofx
 ::load
 (fn [cofx]
   (let [saved (storage-get "dave.ui.db")]
     (cond
       (and saved
                (s/valid? db-state-spec saved))
       (assoc cofx :saved saved)

       saved ;; it must not be valid. Let's delete it
       (do (storage-remove "dave.ui.db")
           cofx)
       ;; otherwise, doesn't add any cofx
       :else cofx))))

(re-frame/reg-fx
 :db/save!
 (fn [db-state]
   (storage-set "dave.ui.db"
                (dissoc db-state
                        ;; Dissoc ID
                        :id
                        ;; Dissoc nav, as this would force navigation in
                        ;; multi-tab situations
                        :nav))))

(re-frame/reg-fx
 :db/destroy!
 (fn [_]
   (storage-remove "dave.ui.db")))

(re-frame/reg-event-fx
 :db/init
 [(re-frame/inject-cofx ::load)]
 (fn [{:keys [saved
              db] :as ctx} [_ instance-id]]
   (let [new-db (merge
                 db ;; merge DB so it works with reset!
                 saved
                 (when-not saved
                   (println "New db")
                   db-default)
                 {:id instance-id})]
     {:db new-db
      :db/save! new-db})))

(re-frame/reg-event-fx
 :db/reset!
 (fn [{{:keys [instance-id]} :db
       :as ctx} _]
   {:db/destroy! true
    :dispatch [:db/init instance-id]}))

(re-frame/reg-sub
 :db/debug
 (fn [db]
   db))
