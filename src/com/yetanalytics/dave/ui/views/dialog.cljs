(ns com.yetanalytics.dave.ui.views.dialog
  "Modal Dialog support"
  (:require ["@material/dialog" :refer [MDCDialog]]
            [reagent.core :as r]
            [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.dave.ui.views.form.textfield :as textfield]
            [com.yetanalytics.dave.ui.views.wizard :as wizard]))

(defn dialog
  "A modal dialog."
  [_]
  (let [title-id (str "dialog-title-" (random-uuid))
        content-id (str "dialog-content-" (random-uuid))
        dialog-ref (atom nil)]
    (r/create-class
     {:reagent-render
      (fn [{:keys [title
                   content
                   actions
                   full-width?
                   full-height?]}]
        [:div.mdc-dialog.dave-dialog
         (cond-> {:role "alertdialog"
                  :aria-modal true
                  :aria-labelledby title-id
                  :aria-describedby content-id}
           full-width? (update :class str " fullwidth")
           full-height? (update :class str " fullheight"))
         [:div.mdc-dialog__container
          [:div.mdc-dialog__surface
           [:h2.mdc-dialog__title
            {:id title-id}
            title]
           (into [:div.mdc-dialog__content
                  {:id content-id}]
                 content)
           (into [:footer.mdc-dialog__actions
                  [:button.mdc-button.mdc-dialog__button
                   {:data-mdc-dialog-action "cancel"
                    ;; TODO: on-click that cancels the dialog action in a
                    ;; re-framey way.
                    :tab-index 0}
                   "Cancel"]]
                 (for [[idx {:keys [label
                                    mdc-dialog-action
                                    on-click
                                    disabled?]}] (map-indexed vector actions)]
                   [:button.mdc-button.mdc-dialog__button
                    (cond-> {:on-click on-click
                             :tab-index (inc idx)}
                      disabled?
                      (assoc :disabled true)
                      mdc-dialog-action
                      (assoc :data-mdc-dialog-action
                             mdc-dialog-action))
                    label]))]
          [:div.mdc-dialog__scrim]]])
      :component-did-mount
      (fn [c]
        (let [d (MDCDialog. (r/dom-node c))]
          (.listen d "MDCDialog:closed"
                   (fn [e]
                     ;; If the dialog closes or is cancelled, we should
                     ;; also reflect this back to app state
                     (when (contains? #{"close"
                                        "cancel"}
                                      (-> e .-detail .-action))
                       (dispatch [:dialog/cancel]))))
          (.open (reset! dialog-ref d))))
      :component-will-unmount
      (fn [c]
        (.close @dialog-ref))})))

(defn form-field-input
  "Render individual form fields"
  [{field-key :key
    :keys [label]}]
  [textfield/textfield
   :label label
   :value @(subscribe [:dialog.form/form-field-val field-key])
   :on-change #(dispatch [:dialog.form/update-field
                          field-key
                          (-> % .-target .-value)])])

(defn dialog-form
  "A dialog oriented towards entering form information"
  []
  [dialog {:title @(subscribe [:dialog/title])
           :content (into []
                          (for [field @(subscribe [:dialog.form/fields])]
                            [form-field-input field]))
           :actions
           (into []
                 (concat
                  (for [{dispatch-v :dispatch
                         :keys [label] :as action}
                        @(subscribe [:dialog/additional-actions])]
                    (merge action {:on-click #(dispatch dispatch-v)}))
                  [{:label "Save"
                    ;; :mdc-dialog-action "save"
                    :on-click #(dispatch [:dialog.form/save])}]))}])

(defn dialog-wizard
  "Dialog for the dave workbook creation wizard"
  []
  [dialog {:title @(subscribe [:dialog/title])
           :full-width? true
           :full-height? true
           :content [[wizard/wizard]]
           :actions
           @(subscribe [:wizard/dialog-actions])
           #_[#_{:label "Save"
             ;; :mdc-dialog-action "save"
             :on-click #(dispatch [:dialog.form/save])}]}])

(defn dialog-container
  "Parent component that shows/hides the dialog"
  []
  (cond-> [:div.dave-dialog-container]
    @(subscribe [:dialog/open?])
    (conj (case @(subscribe [:dialog/mode])
            :com.yetanalytics.dave.ui.app.dialog/form
            [dialog-form]
            :com.yetanalytics.dave.ui.app.dialog/wizard
            [dialog-wizard]))))
