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
      [:div.splash
       [:h2 title]
       [:p description]
       ]
      ;; TODO: Nav/Breadcrumb
      ;; TODO: Question list
      [question/grid-list questions]
      ]]))

(defn cell [workbook]
  [:div
   [:a {:href (str "#/workbooks/" (:id workbook))}
    (str "workbook cell for " (:id workbook))]])

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
