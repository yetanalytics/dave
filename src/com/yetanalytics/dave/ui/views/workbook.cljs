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

(defn page []
  (let [{:keys [id
                title
                description
                questions]
         :as workbook} @(subscribe [:nav/focus])]
    [:div.page.workbook
     [:div ;; inner
      [:div.workbookinfo
       [:p.hometitle title]
       [:p.workbookdesc description]
       [descendant-counts id]
       [data/info id]]
      [:div
       [:h1 "Questions"]]
      [question/grid-list id questions]
      ]]))


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
