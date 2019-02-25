(ns com.yetanalytics.dave.ui.app.db
  "Handle top-level app state & persistence"
  (:require [re-frame.core :as re-frame]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.ui.app.nav :as nav]
            [com.yetanalytics.dave.ui.app.picker :as picker]
            [cognitect.transit :as t]
            [com.yetanalytics.dave.workbook :as workbook]
            [com.yetanalytics.dave.ui.interceptor :as i]
            [com.cognitect.transit.types :as ty]
            [com.yetanalytics.dave.util.spec :as su]
            [com.yetanalytics.dave.ui.app.dialog :as dialog]
            [com.yetanalytics.dave.func :as func])
  (:import [goog.storage Storage]
           [goog.storage.mechanism HTML5LocalStorage]))

;; Make transit UUIDs work with the uuid? spec pred
(extend-type ty/UUID
  cljs.core/IUUID)

;; Set handlers for our funcs
;; TODO: figure out a better way to handle serialization
(def read-handlers
  {"com.yetanalytics.dave.func/SuccessTimeline"
   (fn [m]
     (func/map->SuccessTimeline m))
   "com.yetanalytics.dave.func/DifficultQuestions"
   (fn [m]
     (func/map->DifficultQuestions m))
   "com.yetanalytics.dave.func/CompletionRate"
   (fn [m]
     (func/map->CompletionRate m))
   "com.yetanalytics.dave.func/FollowedRecommendations"
   (fn [m]
     (func/map->FollowedRecommendations m))})

(def write-handlers
  {func/SuccessTimeline
   (t/write-handler (constantly "com.yetanalytics.dave.func/SuccessTimeline")
                    (fn [st]
                      (into {} st)))
   func/DifficultQuestions
   (t/write-handler (constantly "com.yetanalytics.dave.func/DifficultQuestions")
                    (fn [st]
                      (into {} st)))
   func/CompletionRate
   (t/write-handler (constantly "com.yetanalytics.dave.func/CompletionRate")
                    (fn [st]
                      (into {} st)))
   func/FollowedRecommendations
   (t/write-handler (constantly "com.yetanalytics.dave.func/FollowedRecommendations")
                    (fn [st]
                      (into {} st)))})
;; Persistence
(defonce w (t/writer :json {:handlers write-handlers}))
(defonce r (t/reader :json {:handlers read-handlers}))

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
(s/def ::picker picker/picker-spec)
(s/def ::dialog dialog/dialog-spec)
;; Install an index on workbook
;; These are not present in the base spec so workbooks can stand on their own.
(s/def ::workbook/index
  su/index-spec)

(s/def ::workbooks
  (s/and (s/map-of ::workbook/id
                   (s/merge workbook/workbook-spec
                            (s/keys :req-un [::workbook/index])))
         (comp su/sequential-indices? vals)))

(def db-state-spec
  (s/keys :opt-un [::id
                   ::nav
                   ::workbooks
                   ::dialog]))


;; This will include the default workbooks for DAVE
(def db-default
  {:workbooks {#uuid "f1d0bd64-0868-43ec-96c6-a51c387f5fc8"
               {:id #uuid "f1d0bd64-0868-43ec-96c6-a51c387f5fc8"
                :title "DAVE Alpha Demo"
                :description "A tour of DAVE Alpha features."
                :index 0
                :data {:title "test dataset"
                       :type :com.yetanalytics.dave.workbook.data/file
                       :uri "data/dave/ds.json"
                       :built-in? true
                       :state {:statement-idx -1}}
                :questions {#uuid "344d1296-bb19-43f5-92e5-ceaeb7089bb1"
                            {:id #uuid "344d1296-bb19-43f5-92e5-ceaeb7089bb1"
                             :text "When do learners do their best work?"
                             :function {:id :com.yetanalytics.dave.func/success-timeline
                                        :state {:statement-idx -1}
                                        :func (:function
                                               (func/get-func
                                                :com.yetanalytics.dave.func/success-timeline))}
                             :index 0
                             :visualizations
                             {#uuid "c9d0e0c2-3d40-4c5d-90ab-5a482588459f"
                              {:id #uuid "c9d0e0c2-3d40-4c5d-90ab-5a482588459f"
                               :title "Scores of Successful Statements"
                               :vis {:id :com.yetanalytics.dave.vis.scatter/time-scatter
                                     :args {}}
                               :index 0}}}
                            #uuid "4e285a1c-ff7f-4de9-87bc-8ab346ffedea"
                            {:id #uuid "4e285a1c-ff7f-4de9-87bc-8ab346ffedea"
                             :text "What activities are most difficult?"
                             :function {:id :com.yetanalytics.dave.func/difficult-questions
                                        :state {:statement-idx -1}
                                        :func (:function
                                                (func/get-func
                                                 :com.yetanalytics.dave.func/difficult-questions))}
                             :index 1
                             :visualizations
                             {#uuid "8cd6ea72-08d0-4d8e-8547-032d6a340a0b"
                              {:id #uuid "8cd6ea72-08d0-4d8e-8547-032d6a340a0b"
                               :title "Failed Attempts Bar"
                               :vis {:id :com.yetanalytics.dave.vis.bar/base
                                     :args {}}
                               :index 0}
                              #uuid "5147c763-7e77-4e1b-80e1-1054d2225ec5"
                              {:id #uuid "5147c763-7e77-4e1b-80e1-1054d2225ec5"
                               :title "Failed Attempts Pie"
                               :vis {:id :com.yetanalytics.dave.vis.pie/base
                                     :args {}}
                               :index 1}}}
                            #uuid "ec3b9f97-d9e9-4029-9988-a96a367d9b9f"
                            {:id #uuid "ec3b9f97-d9e9-4029-9988-a96a367d9b9f"
                             :text "What activities are completed the most?"
                             :function {:id :com.yetanalytics.dave.func/completion-rate
                                        :state {:statement-idx -1}
                                        :func (:function
                                                (func/get-func
                                                 :com.yetanalytics.dave.func/completion-rate))
                                        :args {:time-unit :day}}
                             :index 2
                             :visualizations
                             {#uuid "85d7d9c4-fe08-4ab0-8e7e-5970881182c5"
                              {:id #uuid "85d7d9c4-fe08-4ab0-8e7e-5970881182c5"
                               :title "Rate of Activity Completion"
                               :vis {:id :com.yetanalytics.dave.vis.bar/base
                                     :args {}}
                               :index 0}}}
                            #uuid "958d2e94-ffdf-441f-a42c-3754cac04c71"
                            {:id #uuid "958d2e94-ffdf-441f-a42c-3754cac04c71"
                             :text "How often are recommendations followed?"
                             :function {:id :com.yetanalytics.dave.func/followed-recommendations
                                        :state {:statement-idx -1}
                                        :func (:function
                                                (func/get-func
                                                 :com.yetanalytics.dave.func/followed-recommendations))
                                        :args {:time-unit :day}}
                             :index 3
                             :visualizations
                             {#uuid "01e6394f-e67b-4f48-8c80-81046fce536e"
                              {:id #uuid "01e6394f-e67b-4f48-8c80-81046fce536e"
                               :title "Recommendations, Launches and Follows"
                               :vis {:id :com.yetanalytics.dave.vis.bar/base
                                     :args {}}
                               :index 0}}
                             }
                            }}}})

(s/def ::saved
  (s/keys :req-un [::workbooks]))

(s/fdef load-cofx
  :args (s/cat :cofx
               map?)
  :ret (s/keys ::opt-un [::saved]))

(defn- load-cofx [cofx]
  (let [saved (storage-get "dave.ui.db")]
    (cond
      (and (not-empty saved)
           (s/valid? db-state-spec saved))
      (assoc cofx :saved saved)

      (some? saved) ;; it must not be valid. Let's delete it
      (do
        (.warn js/console "DB invalid! %o" saved)
        (s/explain db-state-spec saved)
        (storage-remove "dave.ui.db")
        cofx)
      ;; otherwise, doesn't add any cofx
      :else cofx)))

(re-frame/reg-cofx
 ::load
 load-cofx)


;; Hold on to the last saved state so we can compare and save
(def last-saved
  (atom nil))

(s/fdef save?
  :args (s/cat :db db-state-spec)
  :ret boolean?)

(defn save?
  "Should we save this DB?"
  [db]
  (if (s/valid? db-state-spec db)
    (and
     (not= @last-saved db)
     ;; All data is done loading
     (empty?
      (for [[workbook-id {:keys [questions]
                          :as workbook}] (get db :workbooks)
            [_ {:keys [data]}] questions
            :when (true? (:loading data))]
        workbook-id))
     ;; All funcs are caught up
     (every? true?
             (for [[workbook-id {:keys [questions]
                                 :as workbook}] (get db :workbooks)
                   [_ {:keys [data
                              function]}] questions
                   :when (and data function)]
               (= (:state data) (:state function)))))
    (.error js/console "DB State Invalid, not saving!" (s/explain-str db-state-spec
                                                                      db))))

(s/fdef save!-fx
  :args (s/cat :db-state db-state-spec))

(defn- save!-fx
  [db-state]
  (let [to-save (dissoc db-state
                        ;; Dissoc ID
                        :id
                        ;; Dissoc nav, as this would force navigation in
                        ;; multi-tab situations
                        :nav
                        ;; picker is ephemeral
                        :picker
                        ;; so is the dialog
                        :dialog
                        ;; Don't save dave.debug state, as it might be huge
                        :debug)]
    (when (save? to-save)
      (do #_(.log js/console "saving to LocalStorage")
          (storage-set "dave.ui.db"
                       to-save)
          (reset! last-saved to-save))
      #_(.log js/console "Skipped save..."))))
(re-frame/reg-fx
 :db/save!
 save!-fx)

(re-frame/reg-fx
 :db/destroy!
 (fn [_]
   (storage-remove "dave.ui.db")))

(re-frame/reg-event-fx
 :db/init
 [(re-frame/inject-cofx ::load)
  i/persist-interceptor]
 (fn [{:keys [saved
              db] :as ctx} [_ instance-id]]
   {:db (merge
         db ;; merge DB so it works with reset!
         saved
         (when-not saved
           (.log js/console "Creating new DAVE ui DB...")
           db-default)
         {:id instance-id})}))

(re-frame/reg-event-fx
 :db/reset!
 (fn [{{:keys [id]} :db
       :as ctx} _]
   {:db/destroy! true
    :dispatch [:db/init id]}))

(re-frame/reg-event-fx
 :db/save!
 (fn [{:keys [db]
       :as ctx} _]
   {:db/save! db}))

;; Debounced save, can be called a lot
(re-frame/reg-event-fx
 :db/save
 (fn [_ _]
   {:dispatch-debounce
    [::save!
     [:db/save!]
     3000]}))



;; Top-level sub for form-2 subs
(re-frame/reg-sub
 :dave/db
 (fn [db _]
   db))

(re-frame/reg-sub
 :db/debug
 (fn [db _]
   db))

(re-frame/reg-sub
 :db/transit-str
 (fn [db _]
   (t/write w db)))

(re-frame/reg-sub
 :db/edn-str
 (fn [db _]
   (pr-str db)))
