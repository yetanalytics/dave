(ns com.yetanalytics.dave.ui.views.workbook.question.visualization
  (:require [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.dave.ui.views.vega :as v :refer [vega]]
            [reagent.core :as r]))

(defn display
  [workbook-id
   question-id
   visualization-id
   & {:keys [vega-override]}]
  (when-let [vega-spec @(subscribe [:workbook.question.visualization/vega-spec
                                    workbook-id question-id visualization-id])]
    [vega (merge vega-spec
                 vega-override)
     ;; :signals-in {"bar_color" [:debug/bar-color]} ;; signals in from subs
     ;; signals out to handlers
     #_:signals-out #_{"tooltip" [:debug/log "tooltip state:"]
                       "bar_color" [:debug/log "bar color out:"]}
     ;; dom + vega events out to handlers
     #_:events-out #_{"click" [:debug/log "click event:"]}
     ;; Other options:
     ;; :renderer "canvas" ;; use canvas rather than SVG
     ;; :hover? false ;; don't initialize hovering
     ;; :log-level :debug ;; set log level (default is :warn)

     ]))

(defn page []
  (let [{:keys [id]
         :as visualization} @(subscribe [:nav/focus])
        [_ workbook-id _ question-id] @(subscribe [:nav/path])]
    [:div.page.visualization
     [:div ;; inner
      [:div.splash
       [:h2 id]
       [display workbook-id question-id id]]
      ]]))

(defn cell [{:keys [id] :as visualization}]
  (let [[_ workbook-id _ question-id] @(subscribe [:nav/path])]
    [:div
     [:h4 id]
     [:a {:href (str "#/workbooks/" workbook-id
                     "/questions/" question-id
                     "/visualizations/" id)}

      [display workbook-id question-id id
       :vega-override {:width 200}]]]))

(defn grid-list
  "A list of Visualizations"
  [visualizations]
  [:div.visualization.list
   (into [:div] ;; inner
         (for [[id visualization] visualizations
               :let [k (str "visualization-list-cell-" id)]]
           ^{:key k}
           [cell visualization]))])
