(ns com.yetanalytics.dave.ui.views.root
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.dave.ui.views.workbook :as workbook]))

(defn page []
  [:div.page.root
   [:div ;; inner
    [:div.splash
     "Splash!"]
    ;; TODO: Nav/breadcrumbs
    [workbook/grid-list]
    ]])
