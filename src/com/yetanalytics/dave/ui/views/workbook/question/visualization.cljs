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
      [vega v/bar-spec-demo
       ;; :signals-in {"bar_color" [:debug/bar-color]} ;; signals in from subs
       ;; signals out to handlers
       :signals-out {"tooltip" [:debug/log "tooltip state:"]
                     "bar_color" [:debug/log "bar color out:"]}
       ;; dom + vega events out to handlers
       :events-out {"click" [:debug/log "click event:"]}]]]))
       ;; Other options:
       ;; :renderer "canvas" ;; use canvas rather than SVG
       ;; :hover? false ;; don't initialize hovering
       ;; :log-level :debug ;; set log level (default is :warn)



(defn cell [{:keys [id] :as visualization}]
  (let [[_ workbook-id _ question-id] @(subscribe [:nav/path])]
    [:div.boxselection
     [:div.cardtitle
      "Visualization"]
     [:h4 id]
     [:a {:href (str "#/workbooks/" workbook-id
                     "/questions/" question-id
                     "/visualizations/" id)}
      (str "Select Visualization")]]))

(defn grid-list
  "A list of Visualizations"
  [visualizations]
  [:div.visualization.list
   ; [:div.cardtitle
   ;  "Visualizations"]
   (into [:div] ;; inner
         (for [[id visualization] visualizations
               :let [k (str "visualization-list-cell-" id)]]
           ^{:key k}
           [cell visualization]))])
