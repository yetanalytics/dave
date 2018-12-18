(ns com.yetanalytics.dave.ui.app.workbook.question
  (:require [re-frame.core :as re-frame]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.workbook.question :as question]))


;; Handlers
(re-frame/reg-event-fx
 :workbook.question/new
 (fn [{:keys [db] :as ctx} [_ workbook-id]]
   {:dispatch
    [:dialog.form/offer
     {:title "New Question"
      :mode :com.yetanalytics.dave.ui.app.dialog/form
      :dispatch-save [:workbook.question/create workbook-id]
      :fields [{:key :text
                :label "Question Text"}]
      :form {}}]}))

(re-frame/reg-event-fx
 :workbook.question/create
 (fn [{:keys [db] :as ctx} [_ workbook-id form-map]]
   (let [{:keys [id]
          :as new-question} (merge form-map
                                   {:id (random-uuid)
                                    :index 0
                                    :visualizations {}})]
     (if-let [spec-error (s/explain-data question/question-spec
                                         new-question)]
       {:notify/snackbar
        ;; TODO: human readable spec errors
        {:message "Invalid Question"}}
       {:dispatch-n [[:dialog/dismiss]
                     [:crud/create!
                      new-question
                      workbook-id
                      id
                      ]]}))))

(re-frame/reg-event-fx
 :workbook.question/edit
 (fn [{:keys [db] :as ctx} [_
                            workbook-id
                            question-id]]
   (let [question (get-in db [:workbooks
                              workbook-id
                              :questions
                              question-id])]
     {:dispatch
      [:dialog.form/offer
       {:title "Edit Question"
        :mode :com.yetanalytics.dave.ui.app.dialog/form
        :dispatch-save [:workbook.question/update
                        workbook-id
                        question-id]
        :fields [{:key :text
                  :label "Question Text"}]
        :form (select-keys question [:text])}]})))

(re-frame/reg-event-fx
 :workbook.question/update
 (fn [{:keys [db] :as ctx} [_
                            workbook-id
                            question-id
                            form-map]]
   (let [question (get-in db [:workbooks
                              workbook-id
                              :questions
                              question-id])
         updated-question (merge
                           question
                           form-map)]
     (if-let [spec-error (s/explain-data question/question-spec
                                         updated-question)]
       ;; it's invalid, need to inform the user
       {:notify/snackbar
        ;; TODO: human readable spec errors
        {:message "Invalid Question"}}
       ;; it's valid, dismiss the dialog and pass it off to CRUD
       {:dispatch-n [[:dialog/dismiss]
                     [:crud/update!
                      updated-question
                      workbook-id
                      question-id
                      ]]}))))

;; Subs

(re-frame/reg-sub
 :workbook/question
 (fn [[_ ?workbook-id _] _]
   [(if (some? ?workbook-id)
     (re-frame/subscribe [:workbook/lookup ?workbook-id])
     (re-frame/subscribe [:workbook/current]))
    (re-frame/subscribe [:nav/path])])
 (fn [[workbook
       [_ _ _ ?path-question-id & _ :as path]] [_
                                                _ ;; workbook-id, not used
                                                ?question-id]]
   (get-in workbook [:questions
                     (or ?question-id
                         ?path-question-id)])))

(re-frame/reg-sub
 :workbook.question/text
 (fn [[_ & args] _]
   (re-frame/subscribe (into [:workbook/question] args)))
 (fn [question _]
   (:text question)))

(re-frame/reg-sub
 :workbook.question/visualizations
 (fn [[_ & args] _]
   (re-frame/subscribe (into [:workbook/question] args)))
 (fn [question _]
   (:visualizations question)))

(re-frame/reg-sub
 :workbook.question/first-visualization
 (fn [[_ & args] _]
   (re-frame/subscribe (into [:workbook.question/visualizations] args)))
 (fn [vs _]
   (first (sort-by :index (vals vs)))))

(re-frame/reg-sub
 :workbook.question/first-visualization-id
 (fn [[_ & args] _]
   (re-frame/subscribe (into [:workbook.question/first-visualization] args)))
 (fn [v _]
   (:id v)))
