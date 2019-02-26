(ns com.yetanalytics.dave.ui.views.form.select
  (:require ["@material/select" :refer [MDCSelect]]))

(defn select
  [& {:keys [label
             selected
             options ;; ordered list of {:value <> :label <>}

             handler ;; handler callback
             full-width?
             ]
      :or {selected ""
           full-width? false}}]
  [:div {:class (str "mdc-select dave-select "
                     (if full-width?
                       "dave-select-full-width"
                       "dave-select-width"))
         :ref (fn [el]
                (when el
                  (MDCSelect. el)))}
   [:input {:type "hidden",
            :name "enhanced-select"
            :value selected
            :on-change #(-> % .-target .-value handler)}]
   [:i {:class "mdc-select__dropdown-icon"}]
   [:div {:class "mdc-select__selected-text"}]
   [:div {:class (str "mdc-select__menu mdc-menu mdc-menu-surface "
                      (if full-width?
                        "dave-select-full-width"
                        "dave-select-width"))}
    (into [:ul.mdc-list]
          (for [{:keys [label value]} options]
            [:li.mdc-list-item
             (cond-> {:class (when (= selected value)
                               "mdc-list-item--selected")
                      :data-value value}
               (= selected value)
               (assoc :aria-selected "true"))
             label]))]
   [:span {:class "mdc-floating-label"} label]
   [:div {:class "mdc-line-ripple"}]])
