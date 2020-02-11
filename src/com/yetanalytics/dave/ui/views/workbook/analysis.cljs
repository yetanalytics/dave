(ns com.yetanalytics.dave.ui.views.workbook.analysis
  (:require [re-frame.core      :refer [subscribe dispatch]]
            [re-codemirror.core :as cm]
            [cljsjs.codemirror.mode.clojure]
            [cljsjs.codemirror.mode.javascript]))

(defn textarea
  [key mode]
  [cm/codemirror
   {:mode         mode
    :theme        "solarized"
    :lineNumbers  true
    :lineWrapping true}
   {:value  @(subscribe [key])
    :events {"change" (fn [this [cm _]]
                        (dispatch [key (.getValue cm)]))}}])

(defn grid
  []
  [:div
   [:h1 "Analysis"]
   [:div.analysis-grid
    [:div.analysis-inner
     [:div.cell-6
      [:div.analysis-inner
       [:div.cell-12
        [:h4 "Query"]
        [textarea :analysis/query "clojure"]]
       [:div.cell-12
        [:h4 "Viz"]
        [textarea :analysis/viz "application/json"]]]]
     [:div.cell-6
      [:div.analysis-inner
       [:div.cell-6
        [:button.minorbutton
         {:on-click (fn [e]
                      (.preventDefault e)
                      (.stopPropagation e)
                      (dispatch [:analysis/run]))}
         "Run"]
        [:div @(subscribe [:analysis/render])]]]]]]])
