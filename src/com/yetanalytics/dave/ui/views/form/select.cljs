(ns com.yetanalytics.dave.ui.views.form.select
  (:require ["@material/select" :refer [MDCSelect]]
            [reagent.core :as r]))

;; TODO: Proper wrapping for MDC, this tends to leave divs at the bottom of the page
(defn select [& {:keys [handler]}]
  (r/create-class
   {:component-did-mount
    (fn [c]
      (let [mdc-select (MDCSelect. (r/dom-node c))]
        (.listen ^MDCSelect mdc-select
                 "MDCSelect:change"
                 (fn [_]
                   (handler (.-value mdc-select))))))
    :reagent-render
    (fn
      [& {:keys [label
                 selected
                 options ;; ordered list of {:value <> :label <>}

                 ;; handler ;; handler callback
                 full-width?
                 ]
          :or {selected ""
               full-width? false}}]
      [:div {:class (str "mdc-select dave-select "
                         (if full-width?
                           "dave-select-full-width"
                           "dave-select-width"))}
       [:input {:type "hidden",
                :name "enhanced-select"
                :value selected}]
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
       [:div {:class "mdc-line-ripple"}]])}))
