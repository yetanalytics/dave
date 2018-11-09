(ns com.yetanalytics.dave.ui.views.snackbar)

(defn snackbar
  []
  [:div.mdc-snackbar
   {:aria-live "assertive"
    :aria-atomic "true"
    :aria-hidden "true"}
   [:div.mdc-snackbar__text]
   [:div.mdc-snackbar__action-wrapper
    [:button.mdc-snackbar__action-button
     {:type "button"}]]])
