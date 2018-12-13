(ns com.yetanalytics.dave.ui.views.form
  (:require [reagent.core :as r]
            ["@material/textfield" :refer [MDCTextField]]))


(defn text-field
  []
  (let [id (str "text-field-" (random-uuid))]
    (r/create-class
     {:reagent-render
      (fn []
        [:div.mdc-text-field.mdc-text-field--fullwidth
         [:input.mdc-text-field__input
          {:type "text"
           :id id
           :on-input #(println 'input %)}]
         [:label.mdc-floating-label.mdc-floating-label--float-above
          {:for id}
          "hey"]])
      :component-did-mount
      (fn [c]
        (MDCTextField. (r/dom-node c)))})))

(defn text-area
  []
  (let [id (str "text-area-" (random-uuid))]
    (r/create-class
     {:reagent-render
      (fn []
        [:div.mdc-text-field.mdc-text-field--textarea
         [:textarea.mdc-text-field__input
          {:id id
           :rows 8
           :cols 40}]
         [:label.mdc-floating-label.mdc-floating-label--float-above
          {:for id}
          "hey"]])
      :component-did-mount
      (fn [c]
        (MDCTextField. (r/dom-node c)))})))
