(ns com.yetanalytics.dave.ui.views.wizard
  (:require [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.dave.ui.views.form.textfield
             :as textfield]))

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
                            (-> e .-target .-value)]))]
   (str @(subscribe [:wizard.form/spec-errors]))])

(defn step-1-workbook
  []
  [:div.wizard.wizard-workbook "workbook"
   [step-1-form]])

(defn step-2-data
  []
  [:div.wizard.wizard-data "data"])

(defn step-3-question
  []
  [:div.wizard.wizard-question "question"])

(defn step-4-visualization
  []
  [:div.wizard.wizard-visualization "vis"])

(defn step-5-done
  []
  [:div.wizard.wizard-done "done"])

(defn wizard
  [_]
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
    [step-5-done]))
