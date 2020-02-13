(ns com.yetanalytics.dave.ui.views.workbook.analysis
  (:require [re-frame.core      :refer [subscribe dispatch]]
            [re-codemirror.core :as cm]
            [cljsjs.codemirror.mode.clojure]
            [cljsjs.codemirror.mode.javascript]))

(defn textarea
  [{:keys [workbook-id analysis-id
           sub-key dis-key mode]}]
  [cm/codemirror
   {:mode         mode
    :theme        "solarized"
    :lineNumbers  true
    :lineWrapping true}
   {:value  @(subscribe [sub-key])
    :events {"change" (fn [this [cm _]]
                        (dispatch [:workbook.analysis/update
                                   workbook-id
                                   analysis-id
                                   {dis-key (.getValue cm)}]))}}])

(defn page
  []
  (let [{:keys [id
                text
                query
                vega
                visualization]}    @(subscribe [:nav/focus])
        [workbook-id & _] @(subscribe [:nav/path-ids])]
    [:div.page.question
     [:div.splash]
     [:div
      [:div.testdatasetblock
       [:p.hometitle (str "Analysis: " text)]]]
     [:div.analysis-grid
      [:div.analysis-inner
       [:div.cell-6
        [:div.analysis-inner
         [:div.cell-12
          [:h4 "Query Editor"]
          [textarea {:workbook-id workbook-id
                     :analysis-id id
                     :sub-key     :workbook.analysis/query
                     :dis-key     :query
                     :mode        "clojure"}]]
         [:div.cell-12
          [:h4 "Visualization Editor"]
          [textarea {:workbook-id workbook-id
                     :analysis-id id
                     :sub-key     :workbook.analysis/vega
                     :dis-key     :vega
                     :mode        "application/json"}]]]]
       [:div.cell-6
        [:div.analysis-inner
         [:div.cell-6
          [:button.minorbutton
           {:on-click (fn [e]
                        (.preventDefault e)
                        (.stopPropagation e)
                        (dispatch [:workbook.analysis/run
                                   workbook-id
                                   id]))}
           "Run"]
          [:div visualization]]]]]]]))

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
