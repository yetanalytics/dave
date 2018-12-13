(ns com.yetanalytics.dave.ui.views.workbook.question
  (:require [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.dave.ui.views.workbook.question.visualization
             :as visualization]
            [com.yetanalytics.dave.ui.views.workbook.question.func :as func]))

(defn page []
  (let [{:keys [id
                text
                visualizations]
         :as question} @(subscribe [:nav/focus])
        [workbook-id & _] @(subscribe [:nav/path-ids])]
    [:div.page.question
     [:div.splash]
     [:div ;; inner
      [:div.workbookinfo
       [:p.hometitle (str "Question: " text)] ;;The question from the previous page
       [:div.descendant-counts
        [:div.tag.visualtag
         [:p "Total Visualizations: " (count visualizations)]]]
       [func/info workbook-id id]]]
     [:div.locationtitle
      [:h1 "Visualizations"]]
     [visualization/grid-list
      workbook-id id visualizations]]))

(defn cell [workbook-id {:keys [id text] :as question}]
  [:div.boxselection
   [:div.cardtitle
    "Question"]
   [:h4 text]
   [visualization/display
    workbook-id id
    @(subscribe [:workbook.question/first-visualization-id
                 workbook-id id])
    :vega-override {:height 200
                    :width 200}]
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
