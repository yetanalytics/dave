(ns com.yetanalytics.dave.ui.views.workbook.analysis
  (:require [reagent.core       :as r]
            [re-frame.core      :refer [subscribe dispatch]]
            [re-codemirror.core :as cm]
            [cljsjs.codemirror.mode.clojure]
            [cljsjs.codemirror.mode.javascript]
            [cljsjs.codemirror.addon.edit.matchbrackets]
            [cljsjs.codemirror.addon.edit.closebrackets]
            [com.yetanalytics.dave.ui.views.vega :as v :refer [vega]]
            [cljs.pprint :refer [pprint]]))

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
   [:h4.header-title "Result"]
   [:button.minorbutton.header-button
    {:on-click (fn [e]
                 (.preventDefault e)
                 (.stopPropagation e)
                 (println "Download Result"))}
    "Export Data"]
   [:pre
    (with-out-str
      (pprint @(subscribe [:workbook.analysis/result workbook-id analysis-id])))]])

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
        spec @(subscribe
               [:workbook.analysis/result-vega-spec
                workbook-id analysis-id])]
    (conj [:div ;; .vis
           [:h4.header-title
            "Data Visualization"]
           [:button.minorbutton.header-button
            {:on-click (fn [e]
                         (.preventDefault e)
                         (.stopPropagation e)
                         (dispatch [:workbook.analysis/run
                                    workbook-id
                                    analysis-id]))}
            "Run"]]
          (cond error
                [error-display error]
                spec
                [vega spec]
                :else [:p "Cannot Display"]
                ))))

(defn text-display
  [workbook-id analysis-id]
  [:p.hometitle (str "Analysis: " @(subscribe [:workbook.analysis/text
                                               workbook-id analysis-id]))])

(defn export-file
  "Taking in a file blob and a file name, this will create a new anchor element.
   It will use this to download the object."
  [e blob file-name]
  (.preventDefault e)
  (.stopPropagation e)
  (let [link (js/document.createElement "a")]
    (set! (.-download link) file-name)
    ;; webkit does not need the object to be added to the page
    (if js/window.webkitURL
      (set! (.-href link)
            (.createObjectURL js/window.webkitURL
                              blob))
      (do
        ;; for non webkit browsers, add the element to the page
        (set! (.-href link)
              (.createObjectURL js/window.URL
                                blob))
        (set! (.-onclick link)
              (fn [e]
                (.preventDefault e)
                (.stopPropagation e)
                (.remove (.. e -currentTarget))))
        (set! (.. link -style -display)
              "none")
        (.append js/document.body
                 link)))
    (.click link)))

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
              [:h4.header-title "Query Editor"]
              [:button.minorbutton.header-button
               {:on-click (fn [e]
                            (.preventDefault e)
                            (.stopPropagation e)
                            (println @(subscribe [:workbook.analysis/query]))
                            (export-file e
                                         (js/Blob. @(subscribe [:workbook.analysis/query])
                                                   (clj->js {:type "application/json"}))
                                         "query.json"))}
               "Export"]
              [textarea {:workbook-id workbook-id
                         :analysis-id id
                         :sub-key     :workbook.analysis/query
                         :dis-key     :query
                         :opts        {:mode "text/x-clojure"}}]
              [query-parse-error-display workbook-id id]]
             [:div.cell-12
              [:h4 "Visualization Code Editor"]
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
            [:button.minorbutton
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
