(ns com.yetanalytics.dave.ui.app.workbook
  (:require [re-frame.core :as re-frame]
            [com.yetanalytics.dave.workbook :as workbook]
            [clojure.spec.alpha :as s]))

;; Handlers
(re-frame/reg-event-fx
 :workbook/new
 (fn [{:keys [db] :as ctx} _]
   {:dispatch
    [:dialog.form/offer
     {:title "New Workbook"
      :mode :com.yetanalytics.dave.ui.app.dialog/form
      :dispatch-save [:workbook/create]
      :fields [{:key :title
                :label "Title"}
               {:key :description
                :label "Description"}]
      :form {}}]}))

;; Only run after the new workbook action, NOT in the wiz
(re-frame/reg-event-fx
 :workbook/create
 (fn [{:keys [db] :as ctx} [_ form-map]]
   (let [{:keys [id]
          :as new-workbook} (merge form-map
                                   {:id (random-uuid)
                                    :index 0
                                    :questions {}})]
     (if-let [spec-error (s/explain-data workbook/workbook-spec
                                         new-workbook)]
       ;; it's invalid, need to inform the user
       {:notify/snackbar
        ;; TODO: human readable spec errors
        {:message "Invalid Workbook"}}
       ;; it's valid, dismiss the dialog and pass it off to CRUD
       ;; the after-create action will nav to it and open the picker
       {:dispatch-n [[:dialog/dismiss]
                     [:crud/create!
                      new-workbook
                      id]
                     [:workbook/after-create
                      id]]}))))

(re-frame/reg-event-fx
 :workbook/after-create
 (fn [_ [_ workbook-id]]
   {:com.yetanalytics.dave.ui.app.nav/nav-path! [:workbooks
                                                 workbook-id]
    :dispatch [:workbook.data/offer-picker
               workbook-id]}))

(re-frame/reg-event-fx
 :workbook/edit
 (fn [{:keys [db] :as ctx} [_ workbook-id]]
   (let [workbook (get-in db [:workbooks
                              workbook-id])]
     {:dispatch
      [:dialog.form/offer
       {:title "Edit Workbook"
        :mode :com.yetanalytics.dave.ui.app.dialog/form
        :dispatch-save [:workbook/update
                        workbook-id]
        :fields [{:key :title
                  :label "Title"}
                 {:key :description
                  :label "Description"}]
        :form (select-keys workbook [:title :description])
        :additional-actions
        [{:label "Select Dataset"
          :mdc-dialog-action "cancel"
          :dispatch [:workbook.data/offer-picker
                     workbook-id]}]}]})))

(re-frame/reg-event-fx
 :workbook/update
 (fn [{:keys [db] :as ctx} [_
                            workbook-id
                            form-map]]
   (let [workbook (get-in db [:workbooks
                              workbook-id])
         updated-workbook (merge
                           workbook
                           form-map)]
     (if-let [spec-error (s/explain-data workbook/workbook-spec
                                         updated-workbook)]
       ;; it's invalid, need to inform the user
       {:notify/snackbar
        ;; TODO: human readable spec errors
        {:message "Invalid Workbook"}}
       ;; it's valid, dismiss the dialog and pass it off to CRUD
       {:dispatch-n [[:dialog/dismiss]
                     [:crud/update!
                      updated-workbook
                      workbook-id
                      ]]}))))

;; Subs
(re-frame/reg-sub
 :workbook/map
 (fn [db _]
   (:workbooks db {})))

(re-frame/reg-sub
 :workbook/list
 ;; a stable (sorted) list
 ;; TODO: add some value for better sorting
 (fn [_ _] (re-frame/subscribe [:workbook/map]))
 (fn [workbooks _]
   (->> workbooks
        (sort-by (comp
                  :index
                  second))
        (mapv second))))

(re-frame/reg-sub
 :workbook/current
 (fn [_ _]
   [(re-frame/subscribe [:nav/path])
    (re-frame/subscribe [:workbook/map])])
 (fn [[[p0 ?workbook-id & _]
       workbook-map] _]
   (when (= p0 :workbooks)
     (get workbook-map ?workbook-id))))

(re-frame/reg-sub
 :workbook/lookup
 (fn [_ _]
   (re-frame/subscribe [:workbook/map]))
 (fn [workbook-map [_ workbook-id]]
   (get workbook-map workbook-id)))

(re-frame/reg-sub
 :workbook/questions
 (fn [[_ workbook-id] _]
   (re-frame/subscribe [:workbook/lookup workbook-id]))
 (fn [{:keys [questions]} _]
   questions))

(re-frame/reg-sub
 :workbook/question-count
 (fn [[_ workbook-id] _]
   (re-frame/subscribe [:workbook/questions workbook-id]))
 (fn [questions _]
   (count questions)))

;; Collect a map of all vis for a workbook
(re-frame/reg-sub
 :workbook/visualizations
 (fn [[_ workbook-id] _]
   (re-frame/subscribe [:workbook/questions workbook-id]))
 (fn [questions _]
   (reduce conj
           {}
           (for [[_ {:keys [visualizations]}] questions
                 v visualizations]
             v))))

(re-frame/reg-sub
 :workbook/visualization-count
 (fn [[_ workbook-id] _]
   (re-frame/subscribe [:workbook/visualizations workbook-id]))
 (fn [visualizations _]
   (count visualizations)))
