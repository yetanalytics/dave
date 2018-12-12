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
        [:p.hometitle ] ;;The question from the previous page
        [:p.workbookdesc ] ;;The description from the previous page
        [:div.tag
         [:p "Questions: 1" ]]
         [:div.tag.visualtag
           [:p "Total Visualizations: 1"]]]
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
   [:questiondesc]
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
