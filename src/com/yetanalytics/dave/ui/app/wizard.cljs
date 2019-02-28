(ns com.yetanalytics.dave.ui.app.wizard
  "Guide users to create a workbook, choose data, ask a question, and assign a
   visualization"
  (:require [re-frame.core :as re-frame]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.workbook :as workbook]
            [com.yetanalytics.dave.workbook.data :as data]
            [com.yetanalytics.dave.workbook.question :as question]
            [com.yetanalytics.dave.func :as func]
            [com.yetanalytics.dave.workbook.question.visualization :as vis]
            [com.yetanalytics.dave.vis :as v]
            ))

;; step order: workbook -> data -> question ->  visualization -> done

;; The current step
(s/def ::step
  #{::workbook
    ::data
    ::question
    ::visualization})

;; Define the valid transitions
(def step-transitions
  {::workbook ::data
   ::data ::question
   ::question ::visualization})

(def step-transitions-reverse
  (reduce-kv
   (fn [m k v]
     (assoc m v k))
   {}
   step-transitions))

(defn generate-skeleton
  "Generate a new, skeletal workbook. returns a tuple that is the map of ids and
  the skeleton"
  []
  (let [workbook-id (random-uuid)
        question-id (random-uuid)
        vis-id (random-uuid)]
    [{:workbook-id workbook-id
      :question-id question-id
      :vis-id vis-id}
     {:id workbook-id
      :title "DAVE Wizard Workbook"
      :description "A DAVE Workbook created with help from the DAVE Wizard."
      :index 0
      :data {:title "test dataset"
             :type :com.yetanalytics.dave.workbook.data/file
             :uri "data/dave/ds.json"
             :built-in? true
             :state {:statement-idx -1}}
      :questions
      {question-id
       {:id question-id
        :text "When do learners do their best work?"
        :function {:id :com.yetanalytics.dave.func/success-timeline
                   :state {:statement-idx -1}
                   :func (:function
                          (func/get-func
                           :com.yetanalytics.dave.func/success-timeline))}
        :index 0
        :visualizations
        {vis-id
         {:id vis-id
          :title "Scores of Successful Statements"
          :vis {:id :com.yetanalytics.dave.vis.scatter/time-scatter
                :args {}}
          :index 0}}}}}]))

(s/def ::workbook-id
  ::workbook/id)

(s/def ::question-id
  ::question/id)

(s/def ::vis-id
  ::vis/id)

;; top level key tracks the target workbook and step
(s/def ::wizard
  (s/keys :req-un [::workbook-id
                   ::question-id
                   ::vis-id
                   ::step]))

;; Initialize the workbook
(re-frame/reg-event-fx
 :wizard/start
 (fn [{:keys [db]}
      _]
   (let [[{:keys [workbook-id
                  ]
           :as id-map}
          skeleton] (generate-skeleton)]
     {:db (assoc db :wizard (merge
                             {:step ::workbook}
                             id-map))
      :dispatch-n [[:crud/create!
                    skeleton
                    workbook-id]
                   [:dialog/offer
                    {:title "DAVE Workbook Wizard"
                     :mode :com.yetanalytics.dave.ui.app.dialog/wizard
                     :dispatch-cancel [:wizard/cancel]}]]})))

;; Cancel and delete the workbook
(re-frame/reg-event-fx
 :wizard/cancel
 (fn [{:keys [db]}
      _]
   (let [id (get-in db [:wizard :workbook-id])]
     (cond-> {:db (dissoc db :wizard)}
       (get-in db [:workbooks id])
       (assoc :dispatch-n [[:com.yetanalytics.dave.ui.app.crud/delete! id]
                           [:db/save]])))))

;; completes, if possible
(re-frame/reg-event-fx
 :wizard/complete
 (fn [{:keys [db]}
      _]
   (let [workbook-id (get-in db [:wizard :workbook-id])]
     {:com.yetanalytics.dave.ui.app.nav/nav-path! [:workbooks workbook-id]
      :db (dissoc db :wizard)
      :dispatch-n [[:dialog/dismiss]
                   [:db/save]]})))


;; com.yetanalytics.dave.ui.app.workbook.data
(re-frame/reg-event-fx
 :wizard.form/set-field!
 (fn [{:keys [db]}
      [_ k v]]
   (let [k-path (if (vector? k)
                  k
                  [k])
         {:keys [step
                 workbook-id
                 question-id
                 vis-id]
          :as wizard} (:wizard db)]
     (case step
       ::workbook
       {:db (assoc-in db
                      (into [:workbooks
                             workbook-id]
                            k-path)
                      v)}
       ::data
       (let [{data-type :type
              :as data} (get-in db [:workbooks
                                    workbook-id
                                    :data])
             new-data (assoc-in data k-path v)]
         (cond-> {:db (assoc-in db
                                [:workbooks
                                 workbook-id
                                 :data]
                                new-data)}
           ;; If this is an LRS, and is valid, we'll check it,
           ;; adding errors or clearing them.
           (and (= ::data/lrs
                   data-type)
                (s/valid? data/data-spec new-data))
           (assoc :dispatch
                  [:com.yetanalytics.dave.ui.app.workbook.data/check-lrs
                   new-data
                   ;; success
                   [:com.yetanalytics.dave.ui.app.workbook.data/ensure*
                    workbook-id]
                   ;; error
                   [:com.yetanalytics.dave.ui.app.workbook.data/error!
                    workbook-id
                    {:message "Can't Reach LRS"
                     :type ::lrs-check-error}]
                   ])))

       ::question
       {:db (assoc-in db
                      (into [:workbooks
                             workbook-id
                             :questions
                             question-id]
                            k-path)
                      v)}
       ::visualization
       {:db (assoc-in db
                      (into [:workbooks
                             workbook-id
                             :questions
                             question-id
                             :visualizations
                             vis-id]
                            k-path)
                      v)}))))

(defn next!
  [db]
  (let [this-step (get-in db [:wizard :step])
        next-step (get step-transitions this-step)]
    (assoc-in db [:wizard :step] next-step)))

;; proceeds, if possible
(re-frame/reg-event-fx
 :wizard/next
 (fn [{:keys [db]}
      _]
   (when-let [{:keys [step
                      workbook-id
                      question-id
                      vis-id]
               :as wizard} (:wizard db)]

     (cond-> {:db (next! db)}
       ;; ensure the data is there
       (#{::data
          ::workbook}
        step)
       (assoc :dispatch
              [:com.yetanalytics.dave.ui.app.workbook.data/ensure
               workbook-id])))))

;; goes back, if possible
(re-frame/reg-event-fx
 :wizard/prev
 (fn [{:keys [db]}
      _]
   (when-let [step (get-in db [:wizard :step])]
     (when (not= ::workbook step)
       {:db (assoc-in db [:wizard :step]
                      (get step-transitions-reverse step))}))))

;; Specific Handlers
(re-frame/reg-event-fx
 :wizard.data/offer-picker
 (fn [{:keys [db]}
      _]
   (let [workbook-id (get-in db [:wizard :workbook-id])]
     {:dispatch
      [:picker/offer
       {:title "Choose a Data Source"
        :choices
        [{:label "DAVE Test Dataset"
          :img-src "/img/folder.png"
          :dispatch
          [:com.yetanalytics.dave.ui.app.workbook.data/change
           workbook-id
           {:title "test dataset"
            :type :com.yetanalytics.dave.workbook.data/file
            :uri "data/dave/ds.json"
            :built-in? true}]}
         {:label "LRS Data"
          :img-src "/img/db.png"
          :dispatch
          [:com.yetanalytics.dave.ui.app.workbook.data/change
           workbook-id
           {:type :com.yetanalytics.dave.workbook.data/lrs
            ;; remove dummy vals
            ;; :title "My LRS"
            ;; :endpoint "http://localhost:9001"
            :auth {;; :username "123456789"
                   ;; :password "123456789"
                   :type :com.yetanalytics.dave.workbook.data.lrs.auth/http-basic}}]}]}]})))

(re-frame/reg-event-fx
 :wizard.question.function/offer-picker
 (fn [{:keys [db]}
      _]
   (let [{:keys [workbook-id
                 question-id]} (:wizard db)]

     {:dispatch [:workbook.question.function/offer-picker
                 workbook-id
                 question-id]})))

(re-frame/reg-event-fx
 :wizard.question.visualization/offer-picker
 (fn [{:keys [db]}
      _]
   (let [{:keys [workbook-id
                 question-id
                 vis-id]} (:wizard db)]
     {:dispatch [:workbook.question.visualization/offer-picker
                 workbook-id
                 question-id
                 vis-id]})))

(re-frame/reg-sub
 ::wizard
 (fn [db _]
   (:wizard db)))

(re-frame/reg-sub
 :wizard/step
 :<- [::wizard]
 (fn [wizard _]
   (:step wizard)))

(re-frame/reg-sub
 :wizard/current-target
 :<- [::wizard]
 :<- [:workbook/map]
 (fn [[{:keys [step
               workbook-id
               question-id
               vis-id]}
       workbook-map] _]
   (get-in workbook-map
           (case step
             ::workbook [workbook-id]
             ::data [workbook-id
                     :data]
             ::question [workbook-id
                         :questions
                         question-id]
             ::visualization
             [workbook-id
              :questions
              question-id
              :visualizations
              vis-id]
             ))))

;; Specify a step target, won't change
(re-frame/reg-sub
 :wizard/target
 :<- [::wizard]
 :<- [:workbook/map]
 (fn [[{:keys [
               workbook-id
               question-id
               vis-id]}
       workbook-map] [_ step]]
   (get-in workbook-map
           (case step
             ::workbook [workbook-id]
             ::data [workbook-id
                     :data]
             ::question [workbook-id
                         :questions
                         question-id]
             ::visualization
             [workbook-id
              :questions
              question-id
              :visualizations
              vis-id]
             ))))

(re-frame/reg-sub
 :wizard/current-spec
 :<- [:wizard/step]
 (fn [step _]
   (case step
     ::workbook workbook/workbook-spec
     ::data data/data-spec
     ::question question/question-spec
     ::visualization vis/visualization-spec)))

(re-frame/reg-sub
 :wizard.form/field
 :<- [:wizard/current-target]
 (fn [target [_ k]]
   (if (vector? k)
     (get-in target k)
     (get target k))))

(re-frame/reg-sub
 :wizard.form/spec-errors
 :<- [:wizard/step]
 :<- [:wizard/current-target]
 :<- [:wizard/current-spec]
 (fn [[step target spec] _]
   (when spec
     (s/explain-data
      spec
      (case step
        ::workbook (assoc target :questions {})
        ::question (assoc target :visualizations {})
        target)))))

;; Any other state errors besides spec
(re-frame/reg-sub
 :wizard.form/other-errors
 :<- [:wizard/step]
 :<- [:wizard/current-target]
 (fn [[step target] _]
   (case step
     ::data (not-empty (:errors target))
     ::question (let [r-count (get-in target
                                      [:function
                                       :result
                                       :values])]
                  (when (= 0 r-count)
                    [{:message "No results"
                      :type ::no-function-results}]))
     nil)))

(re-frame/reg-sub
 :wizard.form.spec-errors/problems
 :<- [:wizard.form/spec-errors]
 (fn [spec-errors _]
   (::s/problems spec-errors)))

(re-frame/reg-sub
 :wizard.form.field/problem
 :<- [:wizard.form.spec-errors/problems]
 (fn [problems [_ k]]
   (let [k-path (if (vector? k)
                  k
                  [k])]
     (when-let [{:keys [pred]
                 :as problem}
                (some
                 (fn [{:keys [in]
                       :as prob}]
                   (when (= k-path in)
                     prob))
                 problems)]

       (case pred
         cljs.core/not-empty
         "Required Field"
         "Unknown Problem")))))

(re-frame/reg-sub
 :wizard/dialog-actions
 :<- [:wizard.form/spec-errors]
 :<- [:wizard.form/other-errors]
 :<- [:wizard/step]
 (fn [[?spec-error
       ?other-errors
       step] _]
   (cond-> []
     (not= step
           ::workbook)
     (conj
      {:label "Previous"
       :on-click #(re-frame/dispatch [:wizard/prev])})
     (not= step ::visualization)
     (conj
      {:label "Next"
       :disabled?
       ;; TODO: other checks
       (or (some? ?spec-error)
           (some? ?other-errors))
       :on-click #(re-frame/dispatch [:wizard/next])})
     (= step ::visualization)
     (conj
      {:label "Go to Workbook"
       :disabled?
       (or (some? ?spec-error)
           (some? ?other-errors))
       :on-click #(re-frame/dispatch [:wizard/complete])}))))

(re-frame/reg-sub
 :wizard.data/state
 :<- [:wizard/target ::data]
 (fn [data _]
   (:state data)))

(re-frame/reg-sub
 :wizard.question/function
 :<- [:wizard/target ::question]
 (fn [question _]
   (:function question)))

(re-frame/reg-sub
 :wizard.question.function/id
 :<- [:wizard.question/function]
 (fn [function _]
   (:id function)))

(re-frame/reg-sub
 :wizard.question.function/info
 :<- [:wizard.question.function/id]
 (fn [func-id _]
   (func/get-func func-id)))

(re-frame/reg-sub
 :wizard.question.function.info/args-enum
 :<- [:wizard.question.function/info]
 (fn [func _]
   (:args-enum func)))

(re-frame/reg-sub
 :wizard.question.function/result
 :<- [:wizard.question/function]
 (fn [function _]
   (:result function)))

(re-frame/reg-sub
 :wizard.question.function.result/values
 :<- [:wizard.question.function/result]
 (fn [result _]
   (:values result [])))

(re-frame/reg-sub
 :wizard.question.function/result-count
 :<- [:wizard.question.function.result/values]
 (fn [values _]
   (count values)))

(re-frame/reg-sub
 :wizard.visualization/vis
 :<- [:wizard/target ::visualization]
 (fn [v _]
   (:vis v)))

(re-frame/reg-sub
 :wizard.visualization.vis/id
 :<- [:wizard.visualization/vis]
 (fn [vis _]
   (:id vis)))

(re-frame/reg-sub
 :wizard.visualization.vis/info
 :<- [:wizard.visualization.vis/id]
 (fn [vis-id _]
   (get v/registry vis-id)))


(re-frame/reg-sub
 :wizard.visualization.vis.info/title
 :<- [:wizard.visualization.vis/info]
 (fn [vis-info _]
   (:title vis-info)))

(re-frame/reg-sub
 :wizard.visualization.vis.info/datum-spec
 :<- [:wizard.visualization.vis/info]
 (fn [vis-info _]
   (:datum-spec vis-info)))

;; Is the visualization appropriate for the data?
(re-frame/reg-sub
 :wizard.visualization/apropos?
 :<- [:wizard.question.function.result/values]
 :<- [:wizard.visualization.vis.info/datum-spec]
 (fn [[result-values
       datum-spec] _]
   (s/valid? (s/every datum-spec)
             result-values)))
