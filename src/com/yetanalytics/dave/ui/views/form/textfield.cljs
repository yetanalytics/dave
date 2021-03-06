(ns com.yetanalytics.dave.ui.views.form.textfield
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            ["@material/textfield" :refer [MDCTextField]]
            #_["@material/textfield/helper-text"
             :refer [MDCTextFieldHelperText]]))

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
                     value
                     full-width?]
              :or {full-width? true}}]
        [:div.mdc-text-field
         {:class (when full-width?
                   "mdc-text-field--fullwidth")}
         [:input.mdc-text-field__input
          {:type "text"
           :id id
           :value value
           :on-change on-change}]
         [:label.mdc-floating-label
          {:for id}
          label]
         [:div.mdc-line-ripple]])})))

(defn textarea
  [_]
  (let [id (str "mdc-text-field-textarea" (random-uuid))]
    (r/create-class
     {:component-did-mount
      (fn [c]
        (MDCTextField. (r/dom-node c)))
      :reagent-render
      (fn [& {:keys [on-change
                     label
                     value
                     full-width?
                     rows
                     cols]
              :or {rows 3
                   cols 50
                   full-width? true}}]
        [:div.mdc-text-field.mdc-text-field--textarea
         {:class (when full-width?
                   "mdc-text-field--fullwidth")}
         [:textarea.mdc-text-field__input
          {:id id
           :value value
           :on-change on-change
           :rows rows
           :cols cols}]
         [:label.mdc-floating-label
          {:for id}
          label]
         #_[:div.mdc-notched-outline
          [:div.mdc-notched-outline__leading]
          [:div.mdc-notched-outline__notch
           [:label.mdc-floating-label
            {:for id}
            label]]
          [:div.mdc-notched-outline__trailing]]

         #_[:div.mdc-line-ripple]])})))


(def helper-text*
  (r/create-class
   {#_:component-did-mount
    #_(fn [c]
      (MDCTextFieldHelperText. (r/dom-node c)))
    :reagent-render
    (fn [{:keys [text
                 persistent?
                 validation?]}]
      [:div
       {:class (cond-> "mdc-text-field-helper-text"
                 persistent?
                 (str " mdc-text-field-helper-text--persistent")
                 validation?
                 (str " mdc-text-field-helper-text--validation-msg"))}
       text])}))

(defn helper-text
  "A line of helper text, must be immediate sibling of textfield/area"
  [& {:as args}]
  [:div.mdc-text-field-helper-line
   [helper-text* args]])
