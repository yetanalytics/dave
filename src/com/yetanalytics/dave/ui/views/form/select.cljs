(ns com.yetanalytics.dave.ui.views.form.select
  (:require ["@material/select" :refer [MDCSelect]]
            ))

(defn select
  [& {:keys [label
             selected
             options ;; ordered list of {:value <> :label <>}

             handler ;; handler callback
             ]
      :or {selected ""}}]
  [:div.mdc-select
   {:ref (fn [el]
           (when el
             (MDCSelect. el)))
    }
   [:i.mdc-select__dropdown-icon]
   (into [:select.mdc-select__native-control
          {:value selected
           :on-change #(-> % .-target .-value handler)}
          [:option
           {:value ""
            :disabled true}]]
         (for [{:keys [label
                       value] :as option} options]
           ^{:key (str "select-option-" value)}
           [:option
            {:value value}
            label]))
   [:label.mdc-floating-label label]
   [:div.mdc-line-ripple]])
