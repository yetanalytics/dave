(ns com.yetanalytics.dave.ui.app.wizard
  "Guide users to create a workbook, choose data, ask a question, and assign a
   visualization"
  (:require [re-frame.core :as re-frame]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.workbook :as workbook]
            [com.yetanalytics.dave.workbook.question :as question]
            [com.yetanalytics.dave.workbook.question.visualization :as vis]))

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

;; workbook + data

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

;; Question + function
(s/def :com.yetanalytics.dave.ui.app.wizard.workbook.question/id
 ::question/id)

(s/def :com.yetanalytics.dave.ui.app.wizard.workbook.question/form
  (s/keys :req-un [::question/text]))

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

(defmethod step-type :done
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
               {:title "DAVE Wizard"
                :mode :com.yetanalytics.dave.ui.app.dialog/wizard
                :dispatch-cancel [:wizard/cancel]}]}))

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
       {:db (assoc-in db [:wizard :step]
                      (get step-transitions step))
        :dispatch-n
        (case step
          ::workbook
          (let [{:keys [id form]} workbook]
            (if-let [extant (get-in db [:workbooks id])]
              [[:crud/update! (merge extant
                                     form)
                id]]
              [[:crud/create! (merge form
                                     {:id id
                                      :index 0
                                      :questions {}})
                id]]))
          )}))))

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
       (assoc :dispatch [:com.yetanalytics.dave.ui.app.crud/delete! id])))))

;; completes, if possible
(re-frame/reg-event-fx
 :wizard/complete
 (fn [{:keys [db]}
      _]
   ))

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
                         (if (= step ::data)
                           :workbook
                           (keyword (name step)))
                         :form]
                        k)
                    v)})))

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
   (if (= step ::data)
     :workbook
     (keyword (name step)))))

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
 :wizard/id ;; id of current item
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
     ;; TODO: other checks
     (and (nil? ?spec-error)
          (not= step :done))
     (conj
      {:label "Next"
       :on-click #(re-frame/dispatch [:wizard/next])}))))
