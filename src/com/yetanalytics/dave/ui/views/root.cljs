(ns com.yetanalytics.dave.ui.views.root
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.dave.ui.views.workbook :as workbook]))

(defn page []
  [:div.page.root
   [:div ;; inner
    #_[:div.splash
       [:h1 "DAVE"]
       ]
    [:div [:button.newblock
           {:on-click #(dispatch [:workbook/new])}
           "New Workbook"]]
    [workbook/grid-list]]])
