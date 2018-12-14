(ns com.yetanalytics.dave.ui.views.form.textfield
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            ["@material/textfield" :refer [MDCTextField]]))

(defn textfield
  [_]
  (let [id (str "mdc-text-field-" (random-uuid))]
    (r/create-class
     {:component-did-mount
      (fn [c]
        (MDCTextField. (r/dom-node c)))
      :reagent-render
      (fn [& {:keys [on-change
                     label
                     value]}]
        [:div.mdc-text-field
         [:input.mdc-text-field__input
          {:type "text"
           :id id
           :value value
           :on-change on-change}]
         [:label.mdc-floating-label
          {:for id}
          label]
         [:div.mdc-line-ripple]])})))
