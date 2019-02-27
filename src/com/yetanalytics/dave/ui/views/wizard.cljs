(ns com.yetanalytics.dave.ui.views.wizard
  (:require [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.dave.ui.views.form.textfield
             :as textfield]
            [com.yetanalytics.dave.ui.views.form.select
             :as select]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Step 2: Workbook ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; top level, workbook
(defn step-1-form
  []
  [:div.wizard-form
   [textfield/textfield
    :label "Title"
    :value @(subscribe [:wizard.form/field :title])
    :on-change (fn [e]
                 (dispatch [:wizard.form/set-field!
                            :title
                            (-> e .-target .-value)]))]
   [textfield/textarea
    :label "Description"
    :value @(subscribe [:wizard.form/field :description])
    :on-change (fn [e]
                 (dispatch [:wizard.form/set-field!
                            :description
                            (-> e .-target .-value)]))]])

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
   [textfield/textfield
    :label "LRS Title"
    :value @(subscribe [:wizard.form/field :title])
    :on-change (fn [e]
                 (dispatch [:wizard.form/set-field!
                            :title
                            (-> e .-target .-value)]))]
   [textfield/textfield
    :label "LRS Endpoint"
    :value @(subscribe [:wizard.form/field :endpoint])
    :on-change (fn [e]
                 (dispatch [:wizard.form/set-field!
                            :endpoint
                            (-> e .-target .-value)]))]
   [textfield/textfield
    :label "API Key"
    :value @(subscribe [:wizard.form/field [:auth :username]])
    :on-change (fn [e]
                 (dispatch [:wizard.form/set-field!
                            [:auth :username]
                            (-> e .-target .-value)]))]
   [textfield/textfield
    :label "API Key Secret"
    :value @(subscribe [:wizard.form/field [:auth :password]])
    :on-change (fn [e]
                 (dispatch [:wizard.form/set-field!
                            [:auth :password]
                            (-> e .-target .-value)]))]])

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
   #_[select/select
    :label "Select A Data Source"
    :selected (or (some-> @(subscribe [:wizard.form/field :type])
                          name)
                  "file")
    :options [{:value "file"
               :label "Dave Built-in Dataset"}
              {:value "lrs"
               :label "xAPI LRS"}]
    :handler
    (fn [v-str]
      (dispatch [:wizard.form/set-field!
                 :type
                 (keyword "com.yetanalytics.dave.workbook.data"
                          v-str)]))]
   ])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Step 3: Question ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn step-3-question
  []
  [:div.wizard.wizard-question "question"])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Step 4: Vis ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn step-4-visualization
  []
  [:div.wizard.wizard-visualization "vis"])

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
