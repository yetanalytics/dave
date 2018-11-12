(ns com.yetanalytics.dave.ui.views.workbook.question.visualization
  (:require [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.dave.ui.views.vega :as v :refer [vega]]
            [reagent.core :as r]))

(defn page []
  (let [{:keys [id]
         :as question} @(subscribe [:nav/focus])]
    [:div.page.question
     [:div ;; inner
      [:div.splash
       [:h2 id]]
      ;; TODO: Nav/Breadcrumb
      [vega v/bar-spec-demo]]]))

(defn cell [{:keys [id] :as visualization}]
  (let [[_ workbook-id _ question-id] @(subscribe [:nav/path])]
    [:div
     [:h4 id]
     [:a {:href (str "#/workbooks/" workbook-id
                     "/questions/" question-id
                     "/visualizations/" id)}
      (str "visualization cell for " id)]]))

(defn grid-list
  "A list of Visualizations"
  [visualizations]
  [:div.visualization.list
   (into [:div] ;; inner
         (for [[id visualization] visualizations
               :let [k (str "visualization-list-cell-" id)]]
           ^{:key k}
           [cell visualization]))])
