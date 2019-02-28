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
            [com.yetanalytics.dave.vis :as v]))

;; step order: workbook -> data -> question ->  visualization -> done

;; The current step
(s/def ::step
  #{::workbook
    ::data
    ::question
    ::visualization
    ::done})

;; Define the valid transitions
(def step-transitions
  {::workbook ::data
   ::data ::question
   ::question ::visualization
   ::visualization ::done})

(def step-transitions-reverse
  (reduce-kv
   (fn [m k v]
     (assoc m v k))
   {}
   step-transitions))

;; For each step, we hold an ID to create, validate against. Some steps (like data)
;; don't need it.
;; We also have a form map for edits

;; workbook + data partial specs

(s/def :com.yetanalytics.dave.ui.app.wizard.workbook/id
  ::workbook/id)

(s/def :com.yetanalytics.dave.ui.app.wizard.workbook/form
  (s/keys :req-un [::workbook/title
                   ::workbook/description]))

(s/def ::workbook
  (s/keys
   :req-un
   [:com.yetanalytics.dave.ui.app.wizard.workbook/id]
   :opt-un
   [:com.yetanalytics.dave.ui.app.wizard.workbook/form]))

(s/def :com.yetanalytics.dave.ui.app.wizard.workbook.data/form
  ::workbook/data)

(s/def ::data
  (s/keys :opt-un [:com.yetanalytics.dave.ui.app.wizard.workbook.data/form]))

;; Question + function
(s/def :com.yetanalytics.dave.ui.app.wizard.workbook.question/id
 ::question/id)

(s/def :com.yetanalytics.dave.ui.app.wizard.workbook.question/form
  (s/keys :req-un [::question/text
                   ::question/function]))

(s/def ::question
  (s/keys
   :req-un [:com.yetanalytics.dave.ui.app.wizard.workbook.question/id]
   :opt-un [:com.yetanalytics.dave.ui.app.wizard.workbook.question/form]))

;; Visualization + vis
(s/def :com.yetanalytics.dave.ui.app.wizard.workbook.question.visualization/id
  ::vis/id)

(s/def :com.yetanalytics.dave.ui.app.wizard.workbook.question.visualization/form
  (s/keys :req-un [::vis/title]))

(s/def ::visualization
  (s/keys
   :req-un [:com.yetanalytics.dave.ui.app.wizard.workbook.question.visualization/id]
   :opt-un [:com.yetanalytics.dave.ui.app.wizard.workbook.question.visualization/form]))

(defmulti step-type :step)

(defmethod step-type ::workbook
  [_]
  (s/keys :req-un [::step]))

(defmethod step-type ::data
  [_]
  (s/keys :req-un [::step
                   ::workbook]))

(defmethod step-type ::question
  [_]
  (s/keys :req-un [::step
                   ::workbook]))

(defmethod step-type ::visualization
  [_]
  (s/keys :req-un [::step
                   ::workbook
                   ::question]))

(defmethod step-type ::done
  [_]
  (s/keys :req-un [::step
                   ::workbook
                   ::question
                   ::visualization]))

;; top level key
(s/def ::wizard
  (s/multi-spec step-type :step))

(def init-forms
  {::workbook {:title "My Workbook"
               :description "A new DAVE Workbook"}})

(defn init-state
  []
  {:step ::workbook
   :workbook {:id (random-uuid)
              :form (::workbook init-forms)}})

(re-frame/reg-event-fx
 :wizard/start
 (fn [{:keys [db]}
      _]
   {:db (assoc db :wizard (init-state))
    :dispatch [:dialog/offer
               {:title "DAVE Workbook Wizard"
                :mode :com.yetanalytics.dave.ui.app.dialog/wizard
                :dispatch-cancel [:wizard/cancel]}]}))

(defn maybe-set-next-id
  [db next-step-key]
  (if-not (or (= :data next-step-key) ;; data has no id
              (get-in db [:wizard next-step-key :id]))
    (assoc-in db [:wizard next-step-key :id] (random-uuid))
    db))

(defn next!
  [db]
  (let [this-step (get-in db [:wizard :step])
        next-step (get step-transitions this-step)
        next-step-key (keyword (name next-step))]
    (-> db
        (maybe-set-next-id next-step-key)
        (assoc-in [:wizard :step] next-step))))

;; proceeds, if possible
(re-frame/reg-event-fx
 :wizard/next
 (fn [{:keys [db]}
      _]
   (when-let [{:keys [step
                      workbook
                      data
                      question
                      visualization]
               :as wizard} (:wizard db)]
     (when (not= ::done step)
       (case step
         ::workbook
         {:db (next! db)
          :dispatch-n
          (let [{:keys [id form]} workbook]
            (if-let [extant (get-in db [:workbooks id])]
              [[:crud/update! (merge extant
                                     form)
                id]]
              [[:crud/create! (merge form
                                     {:id id
                                      :index 0
                                      :questions {}})
                id]]))}

         ::data
         (let [{:keys [form]} data]
           (if (= ::data/lrs (:type form))
             ;; we only advance if the LRS is all set.
             ;; If it isn't, we try to create it, which will call this again on success
             (if (some-> db
                         (get-in [:workbooks (:id workbook) :data])
                         (select-keys [:title
                                       :endpoint
                                       :auth
                                       :type])
                         (= form))
               ;; next!
               {:db (next! db)}
               ;; Attempt LRS creation
               {:db db
                :dispatch [:com.yetanalytics.dave.ui.app.workbook.data/create-lrs
                           (:id workbook)
                           form]})
             ;; For file, it's much easier
             {:db (next! db)
              :dispatch [:com.yetanalytics.dave.ui.app.workbook.data/change
                         (:id workbook)
                         form]}
             ))
         ::question
         {:db (next! db)
          :dispatch-n
          (let [{workbook-id :id} workbook
                {question-id :id
                 :keys [form]} question]
            (if-let [extant (get-in db [:workbooks
                                        workbook-id
                                        :questions
                                        question-id])]
              [[:crud/update! (merge extant
                                     form)
                workbook-id
                question-id]]
              [[:crud/create! (merge form
                                     {:id question-id
                                      :index 0
                                      :visualizations {}})
                workbook-id
                question-id]]))}
         ::visualization
         {:db (next! db)
          :dispatch-n
          (let [{workbook-id :id} workbook
                {question-id :id} question
                {vis-id :id
                 :keys [form]} visualization]
            (if-let [extant (get-in db [:workbooks
                                        workbook-id
                                        :questions
                                        question-id
                                        :visualizations
                                        vis-id])]
              [[:crud/update! (merge extant
                                     form)
                workbook-id
                question-id
                vis-id]]
              [[:crud/create! (merge form
                                     {:id vis-id
                                      :index 0})
                workbook-id
                question-id
                vis-id]]))})))))

;; goes back, if possible
(re-frame/reg-event-fx
 :wizard/prev
 (fn [{:keys [db]}
      _]
   (when-let [step (get-in db [:wizard :step])]
     (when (not= ::workbook step)
       {:db (assoc-in db [:wizard :step]
                      (get step-transitions-reverse step))}))))

(re-frame/reg-event-fx
 :wizard/cancel
 (fn [{:keys [db]}
      _]
   (let [id (get-in db [:wizard :workbook :id])]
     (cond-> {:db (dissoc db :wizard)}
       (get-in db [:workbooks id])
       (assoc :dispatch-n [[:com.yetanalytics.dave.ui.app.crud/delete! id]
                           [:db/save]])))))

;; completes, if possible
(re-frame/reg-event-fx
 :wizard/complete
 (fn [{:keys [db]}
      _]
   (let [workbook-id (get-in db [:wizard :workbook :id])]
     {:com.yetanalytics.dave.ui.app.nav/nav-path! [:workbooks workbook-id]
      :db (dissoc db :wizard)
      :dispatch [:dialog/dismiss]})))

;; Form manipulation
(re-frame/reg-event-fx
 :wizard.form/set-field!
 (fn [{:keys [db]}
      [_ k v]]
   (let [{:keys [step]
          :as wizard} (:wizard db)]
     {:db (assoc-in db ((if (vector? k)
                          into
                          conj)
                        [:wizard
                         (keyword (name step))
                         :form]
                        k)
                    v)})))

;; Specific Handlers
(re-frame/reg-event-fx
 :wizard.data/offer-picker
 (fn [{:keys [db]}
      _]
   (let [workbook-id (get-in db [:wizard :workbook :id])]
     {:dispatch
      [:picker/offer
       {:title "Choose a Data Source"
        :choices
        [{:label "DAVE Test Dataset"
          :img-src ""
          :dispatch [:wizard.form/set-field!
                     []
                     {:title "test dataset"
                      :type :com.yetanalytics.dave.workbook.data/file
                      :uri "data/dave/ds.json"
                      :built-in? true}]}
         {:label "LRS Data"
          :img-src ""
          :dispatch
          [:wizard.form/set-field!
           []
           {:type :com.yetanalytics.dave.workbook.data/lrs
            ;; remove dummy vals
            :title "My LRS"
            :endpoint "http://localhost:9001"
            :auth {:username "123456789"
                   :password "123456789"
                   :type :com.yetanalytics.dave.workbook.data.lrs.auth/http-basic}}]}]}]})))

(re-frame/reg-event-fx
 :wizard.question.function/offer-picker
 (fn [{:keys [db]}
      _]
   (let [{{workbook-id :id} :workbook
          {question-id :id
           form :form} :question} (:wizard db)]
     {:dispatch [:picker/offer
                 {:title "Choose a DAVE Function"
                  :choices (into []
                                 (for [[id {:keys [title
                                                   doc]}] func/registry]
                                   {:label title
                                    :img-src "img/lambda.svg"
                                    :dispatch
                                    [:wizard.form/set-field!
                                     :function
                                     {:id id
                                      :func (:function (func/get-func id))
                                      :state {:statement-idx -1}
                                      :args {}}]}))}]})))

(re-frame/reg-event-fx
 :wizard.question.visualization/offer-picker
 (fn [{:keys [db]}
      _]
   (let [{{workbook-id :id} :workbook
          {question-id :id
           form :form} :question} (:wizard db)]
     {:dispatch [:picker/offer
                 {:title "Choose a DAVE Chart Prototype"
                  :choices (into []
                                 (for [[id {:keys [title
                                                   vega-spec]}] v/registry]
                                   {:label title
                                    :vega-spec vega-spec
                                    :dispatch [:wizard.form/set-field!
                                               :vis
                                               {:id id
                                                :args {}}]}))}]})))

;; Subs

;; Top-level
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
 ::step-key
 :<- [:wizard/step]
 (fn [step _]
   (keyword (name step))))

(re-frame/reg-sub
 :wizard.step/data
 :<- [::wizard]
 :<- [::step-key]
 (fn [[wizard step-key] _]
   (get wizard step-key)))

;; Main identified objects
(re-frame/reg-sub
 :wizard/workbook
 :<- [::wizard]
 (fn [wizard _]
   (:workbook wizard)))

(re-frame/reg-sub
 :wizard/question
 :<- [::wizard]
 (fn [wizard _]
   (:question wizard)))

(re-frame/reg-sub
 :wizard/visualization
 :<- [::wizard]
 (fn [wizard _]
   (:visualization wizard)))

(re-frame/reg-sub
 :wizard/current-id ;; id of current item
 :<- [:wizard.step/data]
 (fn [data _]
   (:id data)))

;; Form subs
(re-frame/reg-sub
 :wizard/form
 :<- [:wizard.step/data]
 (fn [data _]
   (:form data)))

(re-frame/reg-sub
 :wizard.form/field
 :<- [:wizard/form]
 (fn [form [_ k]]
   (if (vector? k)
     (get-in form k)
     (get form k))))

(defonce form-specs
  {:workbook :com.yetanalytics.dave.ui.app.wizard.workbook/form
   :data :com.yetanalytics.dave.ui.app.wizard.workbook.data/form
   :question :com.yetanalytics.dave.ui.app.wizard.workbook.question/form
   :visualization :com.yetanalytics.dave.ui.app.wizard.workbook.question.visualization/form})

(re-frame/reg-sub
 :wizard.form/spec-errors
 :<- [:wizard/form]
 :<- [::step-key]
 (fn [[form step-key] _]
   (s/explain-data
    (get form-specs step-key)
    form)))

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
 :<- [:wizard/step]
 (fn [[?spec-error
       step] _]
   (cond-> []
     (not= step
           ::workbook)
     (conj
      {:label "Previous"
       :on-click #(re-frame/dispatch [:wizard/prev])})
     (not= step :done)
     (conj
      {:label "Next"
       :disabled?
       ;; TODO: other checks
       (some? ?spec-error)
       :on-click #(re-frame/dispatch [:wizard/next])})
     (= step ::done)
     (conj
      {:label "Go to Workbook"
       :on-click #(re-frame/dispatch [:wizard/complete])}))))

#_(s/reg-sub
 :wizard.function)
