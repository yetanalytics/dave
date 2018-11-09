(ns com.yetanalytics.dave.ui.views.workbook
  (:require [re-frame.core :refer [dispatch subscribe]]))

(defn cell [workbook]
  [:div (str "workbook cell for " (:id workbook))])

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
