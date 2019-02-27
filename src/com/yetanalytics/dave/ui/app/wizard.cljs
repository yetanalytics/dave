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

;; For each step, we hold an ID to validate against. Some steps (like data)
;; don't need it.
(s/def ::workbook
  ::workbook/id)

(s/def ::question
  ::question/id)

(s/def ::visualization
  ::vis/id)

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

(def init-state
  {:step :workbook})

(re-frame/reg-event-fx
 :wizard/start
 (fn [{:keys [db]}
      _]
   {:db (assoc db :wizard init-state)
    :dispatch [:dialog/offer
               {:title "DAVE Wizard"
                :mode :com.yetanalytics.dave.ui.app.dialog/wizard
                :dispatch-cancel [:wizard/cancel]}]}))

;; proceeds, if possible
(re-frame/reg-event-fx
 :wizard/next
 (fn [{:keys [db]}
      _]
  ))


;; goes back, if possible
(re-frame/reg-event-fx
 :wizard/prev
 (fn [{:keys [db]}
      _]
   ))

(re-frame/reg-event-fx
 :wizard/cancel
 (fn [{:keys [db]}
      _]
   {:notify/snackbar
    {:message "Wizard Cancelled"}}))

;; completes, if possible
(re-frame/reg-event-fx
 :wizard/complete
 (fn [{:keys [db]}
      _]
   ))
