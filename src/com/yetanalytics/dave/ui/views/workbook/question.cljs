(ns com.yetanalytics.dave.ui.views.workbook.question
  (:require [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.dave.ui.views.workbook.question.visualization
             :as visualization]
            [com.yetanalytics.dave.ui.views.workbook.question.func :as func]))

(defn edit-button
  [workbook-id question-id]
  [:button.minorbutton
   {:on-click #(dispatch
                [:workbook.question/edit workbook-id question-id])}
   "Edit"])

(defn delete-button
  [workbook-id question-id]
  [:button.minorbutton
   {:on-click #(dispatch
                [:crud/delete-confirm workbook-id question-id])}
   "Delete"])

(defn page []
  (let [{:keys [id
                text
                visualizations
                function]
         :as question} @(subscribe [:nav/focus])
        [workbook-id & _] @(subscribe [:nav/path-ids])]

    [:div.page.question
     [:div.splash]
     [:div ;; inner
      [:div.testdatasetblock
       [:p.hometitle (str "Question: " text)] ;;The question from the previous page
       [edit-button workbook-id id]
       [delete-button workbook-id id]
       [:div.descendant-counts
        [:div.tag.visualtag
         [:p "Total Visualizations: " (count visualizations)]]] ]
      [:div.testdatasetblock (if function
                               [func/info workbook-id id]
                               [:button
                                {:on-click #(dispatch [:workbook.question.function/offer-picker
                                                       workbook-id id])}
                                "Select Function"])]]

     [:div.locationtitle
      [:h1 "Visualizations"]
      ]
     [:div [:button.newblock {:on-click #(dispatch [:workbook.question.visualization/new
                                           workbook-id id])}
            "New Visualization"]]
     [visualization/grid-list
      workbook-id id visualizations]]))

(defn cell [workbook-id {:keys [id text visualizations] :as question}]
  [:div.boxselection
   [:div.cardtitle
    "Question"]
   [:h4 text]
   (when (seq visualizations)
     [visualization/display
      workbook-id id
      @(subscribe [:workbook.question/first-visualization-id
                   workbook-id id])
      :vega-override {:height 200
                      :width 200}])
   [:a {:href (str "#/workbooks/" @(subscribe [:nav/focus-id])
                   "/questions/" id)}
    "Select"]])

(defn grid-list
  "A list of Questions"
  [workbook-id questions]
  [:div.question.list
   (into [:div] ;; inner
         (for [[id question] questions
               :let [k (str "question-list-cell-" id)]]
           ^{:key k}
           [cell workbook-id question]))])
