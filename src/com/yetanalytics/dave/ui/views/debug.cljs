(ns com.yetanalytics.dave.ui.views.debug
  (:require [re-frame.core :refer [dispatch subscribe]]
            [goog.string :refer [format]]
            [goog.string.format]))

(defn debug-bar
  []
  (let [expand? @(subscribe [:debug/expand?])]
    (into
     [:header.mdc-top-app-bar.mdc-top-app-bar--dense.dave-debug-bar
      [:div.mdc-top-app-bar__row
       [:section.mdc-top-app-bar__section.mdc-top-app-bar__section--align-start
        {:role "toolbar"}
        [:a.material-icons.mdc-top-app-bar__action-item
         {:aria-label (if expand? "Contract" "Expand")
          :alt (if expand? "Contract" "Expand")
          :on-click #(dispatch [:debug/toggle!])}
         (if expand? "expand_less" "expand_more")]
        [:span.mdc-top-app-bar__title
         "DEBUG"]
        [:a.material-icons.mdc-top-app-bar__action-item
         {:aria-label "Home"
          :alt "Home"
          :href "#/"}
         "home"]
        [:a.material-icons.mdc-top-app-bar__action-item
         {:aria-label "Reset DB"
          :alt "Reset DB"
          :on-click #(dispatch [:db/reset!])}
         "refresh"]
        ]]
      ]
     (when expand?
       [[:div.mdc-top-app-bar__row
         [:section.mdc-top-app-bar__section.mdc-top-app-bar__section--align-start
          [:span (format "path: %s"
                         @(subscribe [:nav/path]))]]]
        [:div.mdc-top-app-bar__row
         [:section.mdc-top-app-bar__section.mdc-top-app-bar__section--align-start
          [:span (format "context: %s"
                         @(subscribe [:nav/context]))]]]
        [:div.mdc-top-app-bar__row
         [:section.mdc-top-app-bar__section.mdc-top-app-bar__section--align-start
          [:span (format "focus: %s"
                         @(subscribe [:nav/focus-id])
                         )]]]]))))
