(ns com.yetanalytics.dave.ui.views.wizard
  (:require [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.dave.ui.views.form.textfield
             :as textfield]
            [com.yetanalytics.dave.ui.views.form.select
             :as select]))

(defn wizard-field
  "Simple text field for string fields"
  [field-key label]
  [:div.wizard-field
   [textfield/textfield
    :label label
    :value @(subscribe [:wizard.form/field field-key])
    :on-change (fn [e]
                 (dispatch [:wizard.form/set-field!
                            field-key
                            (-> e .-target .-value)]))]
   [textfield/helper-text
    :text (str @(subscribe [:wizard.form.field/problem field-key]))
    :persistent? true
    :validation? true]])

(defn wizard-textarea
  "Simple text area"
  [field-key label]
  [:div.wizard-field
   [textfield/textarea
    :label label
    :value @(subscribe [:wizard.form/field field-key])
    :on-change (fn [e]
                 (dispatch [:wizard.form/set-field!
                            field-key
                            (-> e .-target .-value)]))]
   [textfield/helper-text
    :text (str @(subscribe [:wizard.form.field/problem field-key]))
    :persistent? true
    :validation? true]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Step 1: Workbook ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; top level, workbook
(defn step-1-form
  []
  [:div.wizard-form
   [wizard-field :title "Title"]
   [wizard-textarea :description "Description"]])

(defn step-1-workbook
  []
  [:div.wizard.wizard-workbook "workbook"
   [step-1-form]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Step 2: Data ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn step-2-lrs-form
  []
  [:div.wizard-form
   [wizard-field :title "LRS Title"]
   [wizard-field :endpoint "LRS Endpoint"]
   [wizard-field [:auth :username] "HTTP Basic Auth Username"]
   [wizard-field [:auth :password] "HTTP Basic Auth Password"]])

(defn step-2-data
  []
  [:div.wizard.wizard-data
   "data"
   [:button
    {:on-click #(dispatch [:wizard.data/offer-picker])}
    "Choose Data Source"]
   (case @(subscribe [:wizard.form/field :type])
     :com.yetanalytics.dave.workbook.data/lrs
     [step-2-lrs-form]
     nil)
   ])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Step 3: Question ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn step-3-question-form
  []
  [:div.wizard-form
   [wizard-field :text "Question Text"]])

(defn step-3-question
  []
  [:div.wizard.wizard-question
   "question"
   [step-3-question-form]
   [:button
    {:on-click #(dispatch [:wizard.question.function/offer-picker])}
    "Choose Function"]
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Step 4: Vis ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn step-4-visualization-form
  []
  [:div.wizard-form
   [wizard-field :title "Title"]])
(defn step-4-visualization
  []
  [:div.wizard.wizard-visualization "vis"
   [step-4-visualization-form]
   [:button
    {:on-click #(dispatch [:wizard.question.visualization/offer-picker])}
    "Choose Visualization"]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Step 5: Done ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn step-5-done
  []
  [:div.wizard.wizard-done "done"])

(defn wizard
  [_]
  [:div
   (case @(subscribe [:wizard/step])
     :com.yetanalytics.dave.ui.app.wizard/workbook
     [step-1-workbook]
     :com.yetanalytics.dave.ui.app.wizard/data
     [step-2-data]
     :com.yetanalytics.dave.ui.app.wizard/question
     [step-3-question]
     :com.yetanalytics.dave.ui.app.wizard/visualization
     [step-4-visualization]
     :com.yetanalytics.dave.ui.app.wizard/done
     [step-5-done])
   [:p (str @(subscribe [:wizard.form/spec-errors]))]])
