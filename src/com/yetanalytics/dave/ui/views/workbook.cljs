(ns com.yetanalytics.dave.ui.views.workbook
  (:require [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.dave.ui.views.workbook.question :as question]
            [com.yetanalytics.dave.ui.views.workbook.data :as data]))

(defn descendant-counts
  [id]
  [:div.descendant-counts
   [:div.tag
    [:p "Questions: " (str @(subscribe [:workbook/question-count id]))]]
   [:div.tag.visualtag
    [:p "Visualizations: " (str @(subscribe [:workbook/visualization-count id]))]]])

(defn edit-button [workbook-id]
  [:button
   {:on-click #(dispatch
                [:workbook/edit workbook-id])}
   "Edit"])

(defn delete-button [workbook-id]
  [:button
   {:on-click #(dispatch
                [:crud/delete-confirm workbook-id])}
   "Delete"])

(defn page []
  (let [{:keys [id
                title
                description
                questions
                data]
         :as workbook} @(subscribe [:nav/focus])]
    [:div.page.workbook
     [:div ;; inner
      [:div.workbookinfo.gridblock
       [:p.hometitle title]
       [:p.workbookdesc description]
       [edit-button id]
       [delete-button id]
       [descendant-counts id]]
      [:div.workbookinfo.testdatasetblock
       (when data
         [data/info id])]
      [:div
       [:h1 "Questions"]
       [:button
        {:on-click #(dispatch [:workbook.question/new id])}
        "New Question"]]
      [question/grid-list id questions]]]))



;; TODO: more formatting specifically for cells
(defn cell [{:keys [id] :as workbook}]
  [:div.workbookinfo
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
