(ns com.yetanalytics.dave.ui.views.wizard
  (:require [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.dave.ui.views.form.textfield
             :as textfield]
            [com.yetanalytics.dave.ui.views.form.select
             :as select]

            #_[cljs.pprint :refer [pprint]]))

;; Some form helpers
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
;; Structure ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;    wizard-progress
;;    wizard-header
;; wizard-form | wizard-info
;;     wizard-problems


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Step 1: Workbook ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; top level, workbook
(defn step-1-header
  []
  [:div.wizard-header
   [:h1 "Step 1: Create a Workbook"]])

(defn step-1-form
  []
  [:div.wizard-form
   [wizard-field :title "Title"]
   [wizard-textarea :description "Description"]])

(defn step-1-info
  []
  [:div.wizard-info
   [:p.infomain "Give your workbook a name and a short description."]])

(defn step-1-problems
  []
  [:div.wizard-problems
   [:p (if @(subscribe [:wizard.form/spec-errors])
         "Please fill out all fields."
         "Looks good, click NEXT to continue!")]])

(defn step-1-workbook
  []
  [:div.wizard.wizard-workbook
   [:div ;; inner
    [step-1-header]
    [step-1-form] [step-1-info]
    [step-1-problems]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Step 2: Data ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn step-2-header
  []
  [:div.wizard-header
   [:h1 "Step 2: Select A Data Source"]])

(defn step-2-form
  []
  (let [data-type @(subscribe [:wizard.form/field :type])]
    (cond-> [:div.wizard-form
             [:button.majorbutton
              {:on-click #(dispatch [:wizard.data/offer-picker])}
              (if data-type
                "Choose Another Data Source"
                "Choose Data Source")]]
      (= :com.yetanalytics.dave.workbook.data/lrs
         data-type)
      (conj [:h2 "LRS Data Source"]
            [wizard-field :title "LRS Title"]
            [wizard-field :endpoint "LRS Endpoint"]
            [wizard-field [:auth :username] "HTTP Basic Auth Username"]
            [wizard-field [:auth :password] "HTTP Basic Auth Password"])
      (= :com.yetanalytics.dave.workbook.data/file
         data-type)
      (conj [:h2 "DAVE Test Dataset"]))))

(defn step-2-data-state
  []
  (let [{:keys [statement-idx
                ]
         [s-first s-last] :stored-domain
         [t-first t-last] :timestamp-domain
         :as state} @(subscribe [:wizard.data/state])]
    [:dl
     [:dt "Statements"]
     [:dd (if statement-idx
            (str (inc statement-idx))
            "Loading...")]

     [:dt "Stored Range"]
     [:dd (if s-first
            (str s-first " to " s-last)
            "Loading...")]
     [:dt "Timestamp Range"]
     [:dd (if t-first
            (str t-first " to " t-last)
            "Loading...")]]))

(defn step-2-info
  []
  (let [data-type @(subscribe [:wizard.form/field :type])]
    [:div.wizard-info
     [:p.infomain  "Select a source for the xAPI data that you want to consider in your workbook."]
     [:p.infomain  (case data-type
           :com.yetanalytics.dave.workbook.data/lrs
           "Connecting DAVE to an xAPI Learning Record Store (LRS) allows it to pull data in real-time."
           :com.yetanalytics.dave.workbook.data/file
           "The DAVE Test Dataset is a collection of xAPI statements appropriate for use with DAVE's algorithms. It is built in to DAVE, so no further configuration is needed.")]
     [step-2-data-state]]))

(defn step-2-problems
  []
  [:div.wizard-problems
   [:p

    (let [data-type @(subscribe [:wizard.form/field :type])
          spec-errors @(subscribe [:wizard.form/spec-errors])
          other-errors @(subscribe [:wizard.form/other-errors])]
      (cond
        (nil? data-type) "Please select a data source using the button above."
        spec-errors "Please fill out all fields."
        other-errors (if (= :com.yetanalytics.dave.workbook.data/lrs
                            data-type)
                       "DAVE couldn't reach this LRS. Confirm your settings."
                       "An unknown error occurred. Please report it.")
        :else "Looks good, click NEXT to continue."))]])

(defn step-2-data
  []
  [:div.wizard.wizard-data
   [:div
    [step-2-header]
    [step-2-form] [step-2-info]
    [step-2-problems]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Step 3: Question ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn step-3-header
  []
  [:div.wizard-header
   [:h1 "Step 3: Ask a Question"]])

(defn step-3-info
  []
  (let [result-count @(subscribe [:wizard.question.function/result-count])]
    [:div.wizard-info
     [:p.infomain "Write out the question you would like to answer, and select a function to answer it."]
     [:p (str "For the chosen data source (and arguments, if provided), this function will return " result-count  " results.")]]))

(defn step-3-form-function-info
  []
  (let [{:keys [title
                doc]} @(subscribe [:wizard.question.function/info])]
    [:div
     [:h3 "Function: " title]
     [:p doc]]))

(defn step-3-form-function-args
  []
  (let [{:keys [workbook-id question-id]}
        @(subscribe [:com.yetanalytics.dave.ui.app.wizard/wizard])]
    (into [:div.args]
          (for [[k enum] @(subscribe [:wizard.question.function.info/args-enum])
                ]
            [select/select
             :label (name k)
             :options (into []
                            (for [v enum]
                              {:label (name v)
                               :value (name v)}))
             :selected (if-let [sel @(subscribe [:wizard.form/field (conj [:function :args]
                                                                            k)])]
                         (name sel)
                         "")
             :handler #(-> %
                           keyword
                           (->> (conj [:workbook.question.function/set-arg!
                                       workbook-id question-id
                                       k]))
                           dispatch)]))))
(defn step-3-form
  []
  [:div.wizard-form
   [wizard-field :text "Question Text"]
   [step-3-form-function-info]
   [step-3-form-function-args]
   [:button.majorbutton
    {:on-click #(dispatch [:wizard.question.function/offer-picker])}
    (if-not @(subscribe [:wizard.form/field :function])
      "Choose A Function"
      "Choose Another Function")]])

(defn step-3-problems
  []
  [:div.wizard-problems
   [:p

    (let [spec-errors @(subscribe [:wizard.form/spec-errors])
          other-errors @(subscribe [:wizard.form/other-errors])]
      (cond
        spec-errors "Please fill out all fields."
        other-errors "The function didn't return any results. Please try another, or go back and select a different data source."
        :else "Looks good, click NEXT to continue."))]])

(defn step-3-question
  []
  [:div.wizard.wizard-question
   [:div
    [step-3-header]
    [step-3-form] [step-3-info]
    [step-3-problems]]
   ])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Step 4: Vis ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn step-4-header
  []
  [:div.wizard-header
   [:h1 "Step 4: Create a Visualization"]])

(defn step-4-form
  []
  [:div.wizard-form
   [wizard-field :title "Title"]
   [:button.majorbutton
    {:on-click #(dispatch [:wizard.question.visualization/offer-picker])}
    "Choose Another Visualization"]])

(defn step-4-info
  []
  [:div.wizard-info
   [:p.infomain "Choose one of the DAVE visualization prototypes to display the data."]])

(defn step-4-problems
  []
  [:div.wizard-problems
   [:p

    (let [spec-errors @(subscribe [:wizard.form/spec-errors])
          other-errors @(subscribe [:wizard.form/other-errors])]
      (cond
        spec-errors "Please fill out all fields."
        :else "Looks good, click NEXT to continue."))]])

(defn step-4-visualization
  []
  [:div.wizard.wizard-visualization
   [:div
    [step-4-header]
    [step-4-form] [step-4-info]
    [step-4-problems]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Step 5: Done ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn step-5-done
  []
  [:div.wizard.wizard-done "done"])

(defn wizard
  [_]
  [:div.wizard-container
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
   #_[:pre (with-out-str (pprint @(subscribe [:wizard/current-target])))]
   #_[:p (str @(subscribe [:wizard.form/spec-errors]))]])
