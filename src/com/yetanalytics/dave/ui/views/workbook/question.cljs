(ns com.yetanalytics.dave.ui.views.workbook.question
  (:require [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.dave.ui.views.workbook.question.visualization
             :as visualization]))

(defn page []
  (let [{:keys [id
                text
                visualizations]
         :as question} @(subscribe [:nav/focus])]
    [:div.page.question
      [:div.splash]

      [:div ;; inner
       [:div.workbookinfo
        [:p.hometitle (str "Question: " text)] ;;The question from the previous page
        [:div.tag.visualtag
         [:p "Total Visualizations: " (count visualizations)]]]
       ]
       [:div.locationtitle
        "Visualization"]
      ;; TODO: Nav/Breadcrumb
      [visualization/grid-list
       visualizations]]))

(defn cell [{:keys [id text] :as question}]
  [:div
   [:div.locationtitle
    "Question"]
   [:h4 text]
   [:a {:href (str "#/workbooks/" @(subscribe [:nav/focus-id])
                   "/questions/" id)}
    (str "question cell for " id)]])

(defn grid-list
  "A list of Questions"
  [questions]
  [:div.question.list
   (into [:div] ;; inner
         (for [[id question] questions
               :let [k (str "question-list-cell-" id)]]
           ^{:key k}
           [cell question]))])
