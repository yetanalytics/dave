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

#_(defn grid
  []
  [:div
   [:h1 "Analysis"]
   [:div.analysis-grid
    [:div.analysis-inner
     [:div.cell-6
      [:div.analysis-inner
       [:div.cell-12
        [:h4 "Query Editor"]
        [textarea :analysis/query "clojure"]]
       [:div.cell-12
        [:h4 "Visualization Editor"]
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

(defn page
  []
  (let [{:keys [id
                text
                query
                vega]}    @(subscribe [:nav/focus])
        [workbook-id & _] @(subscribe [:nav/path-ids])]
    [:div.page.question
     [:div.splash]
     [:div
      [:div.testdatasetblock
       [:p.hometitle (str "Analysis: " text)]]]]))

(defn cell
  [workbook-id {:keys [id text]}]
  [:div.boxselection
   {:on-click #(dispatch [:nav/nav-path!
                          [:workbooks
                           workbook-id
                           :analyses
                           id]])}
   [:div.cardtitle
    "Analysis"]
   [:h4
    [:a
     {:href (str "#/workbooks/" @(subscribe [:nav/focus-id])
                 "/analyses/" id)}
     text]]])

(defn grid-list
  "This contains the list of all analyses."
  [workbook-id analyses]
  [:div.question.list
   (into [:div]
         (for [[id analysis] analyses
               :let          [k (str "analysis-list-cell-" id)]]
           ^{:key k}
           [cell workbook-id analysis]))])
