(ns com.yetanalytics.dave.ui.views.debug
  (:require [re-frame.core :refer [dispatch subscribe]]))

(defn debug-bar
  []
  [:header.mdc-top-app-bar.dave-debug-bar
   [:div.mdc-top-app-bar__row
    [:section.mdc-top-app-bar__section.mdc-top-app-bar__section--align-start
     {:role "toolbar"}
     [:span.mdc-top-app-bar__title
      "DEBUG"]
     [:a.material-icons.mdc-top-app-bar__action-item
      {:aria-label "Reset DB"
       :alt "Reset DB"
       :on-click #(dispatch [:db/reset!])}
      "refresh"]]]])
