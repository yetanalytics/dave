(ns com.yetanalytics.dave.ui.views.workbook.analysis
  (:require [reagent.core       :as r]
            [re-frame.core      :refer [subscribe dispatch]]
            [re-codemirror.core :as cm]
            [cljsjs.codemirror.mode.clojure]
            [cljsjs.codemirror.mode.javascript]
            [cljsjs.codemirror.addon.edit.matchbrackets]
            [cljsjs.codemirror.addon.edit.closebrackets]
            [com.yetanalytics.dave.ui.app.io     :as io]
            [com.yetanalytics.dave.ui.views.vega :as v :refer [vega]]
            [cljs.pprint :refer [pprint]]))

(defn hidden-button
  [id workbook-id analysis-id key]
  [:input.hidden-button
   {:id       id
    :type     "file"
    :onChange (fn [e]
                (io/import-file e workbook-id analysis-id key))}])

(defn import-button
  [id]
  [:button.minimalbutton
   {:on-click (fn [e]
                (.preventDefault e)
                (.stopPropagation e)
                (.click (js/document.getElementById id)))}
   "Import"])

(defn export-button
  [key text]
  [:button.minimalbutton
   {:on-click (fn [e]
                (io/export-file e
                                (js/Blob. [@(subscribe [key])]
                                          (clj->js {:type "application/json"}))
                                "query.json"))}
   "Export"])

(defn textarea
  [{:keys [workbook-id analysis-id
           sub-key dis-key opts]}]
  [cm/codemirror
   (merge {:mode              "javascript"
           :lineNumbers       true
           :lineWrapping      true
           :matchBrackets     true
           :autoCloseBrackets true}
          opts)
   {:value  @(subscribe [sub-key])
    :events {"change" (fn [this [cm _]]
                        (dispatch [:workbook.analysis/update
                                   workbook-id
                                   analysis-id
                                   {dis-key (.getValue cm)}]))}}])

(defn edit-button
  [workbook-id analysis-id]
  [:button.minorbutton
   {:on-click #(dispatch
                [:workbook.analysis/edit workbook-id analysis-id])}
   "Edit"])

(defn delete-button
  [workbook-id analysis-id]
  [:button.minorbutton
   {:on-click #(dispatch
                [:crud/delete-confirm workbook-id analysis-id])}
   "Delete"])

(defn error-display
  [error-str]
  [:div.error
   error-str])

(defn query-parse-error-display
  [workbook-id analysis-id]
  [error-display @(subscribe [:workbook.analysis/query-parse-error workbook-id analysis-id])])

(defn query-find-bindings-display
  [workbook-id analysis-id]
  [:div.result
   [:h4 "Query Bindings"]
   [:pre
    (with-out-str
      (print @(subscribe [:workbook.analysis/query-find-bindings workbook-id analysis-id])))]])

(defn result-display
  [workbook-id analysis-id]
  [:div.result
   [:h4 "Result"]
   [:pre
    (with-out-str
      (pprint @(subscribe [:workbook.analysis/result])))]])

(defn vega-parse-error-display
  [workbook-id analysis-id]
  [error-display @(subscribe [:workbook.analysis/vega-parse-error workbook-id analysis-id])])

(defn visualization-fields-display
  [workbook-id analysis-id]
  [:div.vis-fields
   [:h4 "Visualization Fields"]
   (with-out-str
     (print @(subscribe [:workbook.analysis/visualization-fields workbook-id analysis-id])))])

(defn visualization-display
  [workbook-id analysis-id]
  (let [;; The spec + result will be passed into vega for parsing,
        ;; If there is something illegal that will crash the vis,
        ;; it must not be shown
        error @(subscribe
                [:workbook.analysis/visualization-parse-error
                 workbook-id analysis-id])
        spec  @(subscribe
                [:workbook.analysis/result-vega-spec
                 workbook-id analysis-id])
        base  [:div
               [:div.flex-container
                [:h4.header-title "Data Visualization"]
                [:div.spacer]
                [:button.minorbutton.header-button
                 {:on-click (fn [e]
                              (.preventDefault e)
                              (.stopPropagation e)
                              (dispatch [:workbook.analysis/run
                                         workbook-id
                                         analysis-id]))}
                 "Run"]]]]
    (cond error
          (conj base
                [error-display error])
          spec
          [vega spec :workbook-id workbook-id :analysis-id analysis-id]
          :else (conj base
                      [:p "Create a query and data visualization spec."]))))

(defn text-display
  [workbook-id analysis-id]
  [:p.hometitle (str "Analysis: " @(subscribe [:workbook.analysis/text
                                               workbook-id analysis-id]))])

(defn page
  []
  (let [state (r/atom {:advanced false})]
    (fn []
      (let [[workbook-id id & _] @(subscribe [:nav/path-ids])]
        [:div.page.question
         [:div.splash]
         [:div
          [:div.testdatasetblock
           [text-display workbook-id id]
           [edit-button workbook-id id]
           [delete-button workbook-id id]]]
         [:div.analysis-grid
          [:div.analysis-inner
           [:div.cell-6
            [:div.analysis-inner
             [:div.cell-12
              [:div.flex-container
               [:h4.header-title "Query Editor"]
               [:div.spacer]
               [:button.minorbutton
                {:on-click #(dispatch [:workbook.analysis.template/query workbook-id id])}
                "Select Template"]
               [hidden-button "query-input-file" workbook-id id :query]
               [import-button "query-input-file"]
               [export-button :workbook.analysis/query "query.json"]]
              [textarea {:workbook-id workbook-id
                         :analysis-id id
                         :sub-key     :workbook.analysis/query
                         :dis-key     :query
                         :opts        {:mode "text/x-clojure"}}]
              [query-parse-error-display workbook-id id]]
             [:div.cell-12
              [:div.flex-container
               [:h4.header-title "Visualization Code Editor"]
               [:div.spacer]
               [:button.minorbutton
                {:on-click #(dispatch [:workbook.analysis.template/vega workbook-id id])}
                "Select Template"]
               [hidden-button "vega-input-file" workbook-id id :vega]
               [import-button "vega-input-file"]
               [export-button :workbook.analysis/vega "visualization.json"]]
              [textarea {:workbook-id workbook-id
                         :analysis-id id
                         :sub-key     :workbook.analysis/vega
                         :dis-key     :vega
                         :opts        {:mode "application/json"}}]
              [vega-parse-error-display workbook-id id]]]]
           [:div.cell-6
            [:div.analysis-inner
             [:div.cell-12
              [visualization-display workbook-id id]]]]
           [:div.cell-12
            [:button.minimalbutton
             {:on-click (fn [e]
                          (.preventDefault e)
                          (.stopPropagation e)
                          (swap! state update :advanced not))}
             (if (:advanced @state)
               "Hide Advanced"
               "Show Advanced")]
            (when (:advanced @state)
              [:div.analysis-inner
               [:div.cell-3
                [query-find-bindings-display workbook-id id]]
               [:div.cell-3
                [visualization-fields-display workbook-id id]]
               [:div.cell-6
                [result-display workbook-id id]]])]]]]))))

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
