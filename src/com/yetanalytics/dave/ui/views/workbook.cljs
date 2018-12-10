(ns com.yetanalytics.dave.ui.views.workbook
  (:require [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.dave.ui.views.workbook.question :as question]))

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
       [:div.tag
        [:p "Questions: " (count questions)]]]
      ;; TODO: Nav/Breadcrumb
      ;; TODO: Question list
      [question/grid-list questions]
      ]]))


;; TODO: more formatting specifically for cells
(defn cell [workbook]
  [:div.workbookinfo
   [:p.hometitle [:a {:href (str "#/workbooks/" (:id workbook))}
                  (:title workbook)]]
   [:p.workbookdesc (:description workbook)]
   [:div.tag
    [:p "Total Workbooks 2"]]])

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
