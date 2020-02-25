(ns com.yetanalytics.dave.ui.views.workbook
  (:require [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.dave.ui.views.workbook.data :as data]
            [com.yetanalytics.dave.ui.views.workbook.analysis :as analysis]))

(defn descendant-counts
  [id]
  [:div.descendant-counts
   [:div.tag
    [:p "Analyses: " (str @(subscribe [:workbook/analysis-count id]))]]])

(defn edit-button [workbook-id]
  [:button.minorbutton
   {:on-click #(dispatch
                [:workbook/edit workbook-id])}
   "Edit"])

(defn delete-button [workbook-id]
  [:button.minorbutton
   {:on-click #(dispatch
                [:crud/delete-confirm workbook-id])}
   "Delete"])

(defn page []
  (let [{:keys [id
                title
                description
                analyses
                data]
         :as workbook} @(subscribe [:nav/focus])]
    [:div.page.workbook
     [:div ;; inner
      [:div.testdatasetblock.gridblock
       [:p.hometitle title]
       [:p.workbookdesc description]
       [edit-button id]
       [delete-button id]
       [descendant-counts id]]
      [:div.testdatasetblock.gridblock
       (when data
         [data/info id])]
      [:div
       [:h1 "Analyses"]
       [:button.newblock
        {:on-click #(dispatch [:workbook.analysis/new id])}
        "New Analysis"]]
      [analysis/grid-list id analyses]]]))



;; TODO: more formatting specifically for cells
(defn cell [{:keys [id] :as workbook}]
  [:div.workbookinfo
   {:on-click #(dispatch [:nav/nav-path!
                          [:workbooks
                           id]])}
   [:div.sectiontitle
    [:p "Workbook"]]
   [:p.hometitle [:a {:href (str "#/workbooks/" id)}
                  (:title workbook)]]
   [:p.workbookdesc (:description workbook)]
   [descendant-counts id]])

(defn grid-list
  "A list of workbooks in ye responsive grid"
  []
  (let [workbook-list @(subscribe [:workbook/list])]
    [:div.workbook.list
     (into [:div] ;; inner
           (for [{:keys [id] :as workbook} workbook-list
                 :let [k (str "workbook-list-cell-" id)]]
             ^{:key k}
             [cell workbook]))]))
