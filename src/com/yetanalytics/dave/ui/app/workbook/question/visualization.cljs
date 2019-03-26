(ns com.yetanalytics.dave.ui.app.workbook.question.visualization
  (:require [re-frame.core :as re-frame]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.workbook.question.visualization :as vis]
            [com.yetanalytics.dave.vis :as v]))

;; Handlers
(re-frame/reg-event-fx
 :workbook.question.visualization/new
 (fn [{:keys [db] :as ctx} [_ workbook-id question-id]]
   {:dispatch
    [:dialog.form/offer
     {:title "New Visualization"
      :mode :com.yetanalytics.dave.ui.app.dialog/form
      :dispatch-save [:workbook.question.visualization/create workbook-id question-id]
      :fields [{:key :title
                :label "Title"}]
      :form {}}]}))

(re-frame/reg-event-fx
 :workbook.question.visualization/create
 (fn [{:keys [db] :as ctx} [_ workbook-id question-id form-map]]
   (let [{:keys [id]
          :as new-visualization} (merge form-map
                                        {:id (random-uuid)
                                         :index 0})]
     (if-let [spec-error (s/explain-data vis/visualization-spec
                                         new-visualization)]
       {:notify/snackbar
        ;; TODO: human readable spec errors
        {:message "Invalid Visualization"}}
       {:dispatch-n [[:dialog/dismiss]
                     [:crud/create!
                      new-visualization
                      workbook-id
                      question-id
                      id
                      ]
                     [:workbook.question.visualization/after-create
                      workbook-id
                      question-id
                      id]]}))))

(re-frame/reg-event-fx
 :workbook.question.visualization/after-create
 (fn [_ [_
         workbook-id
         question-id
         vis-id]]
   {:com.yetanalytics.dave.ui.app.nav/nav-path! [:workbooks
                                                 workbook-id
                                                 :questions
                                                 question-id
                                                 :visualizations
                                                 vis-id]
    :dispatch [:workbook.question.visualization/offer-picker
               workbook-id
               question-id
               vis-id]}))

;; Handlers
(re-frame/reg-event-fx
 :workbook.question.visualization/edit
 (fn [{:keys [db] :as ctx} [_
                            workbook-id
                            question-id
                            visualization-id]]
   (let [visualization
         (get-in db [:workbooks
                     workbook-id
                     :questions
                     question-id
                     :visualizations
                     visualization-id])]
     {:dispatch
      [:dialog.form/offer
       {:title "Edit Visualization"
        :mode :com.yetanalytics.dave.ui.app.dialog/form
        :dispatch-save [:workbook.question.visualization/update
                        workbook-id question-id visualization-id]
        :fields [{:key :title
                  :label "Title"}]
        :form (select-keys visualization [:title])}]})))

(re-frame/reg-event-fx
 :workbook.question.visualization/update
 (fn [{:keys [db] :as ctx} [_
                            workbook-id question-id visualization-id
                            form-map]]
   (let [visualization (get-in db [:workbooks
                                   workbook-id
                                   :questions
                                   question-id
                                   :visualizations
                                   visualization-id])
         updated-visualization (merge
                                visualization
                                form-map)]
     (if-let [spec-error (s/explain-data vis/visualization-spec
                                         updated-visualization)]
       {:notify/snackbar
        ;; TODO: human readable spec errors
        {:message "Invalid Visualization"}}
       {:dispatch-n [[:dialog/dismiss]
                     [:crud/update!
                      updated-visualization
                      workbook-id
                      question-id
                      visualization-id
                      ]]}))))





(re-frame/reg-event-fx
 :workbook.question.visualization/set-vis!
 (fn [{:keys [db] :as ctx} [_
                            workbook-id
                            question-id
                            visualization-id
                            vis-id
                            ?args :as call]]
   (let [new-db (update-in db
                           [:workbooks
                            workbook-id
                            :questions
                            question-id
                            :visualizations
                            visualization-id]
                           merge
                           {:vis
                            {:id vis-id
                             :args (or ?args
                                       {})}
                            :title (get-in v/registry [vis-id :title]
                                           "Unnamed Chart")})]
     {:db new-db
      :dispatch [:db/save]})))

;; Offer vis picker
(re-frame/reg-event-fx
 :workbook.question.visualization/offer-picker
 (fn [{:keys [db] :as ctx} [_
                            workbook-id
                            question-id
                            visualization-id]]
   {:dispatch [:picker/offer
               {:title "Choose a DAVE Chart Prototype"
                :choices (into []
                               (for [[id {:keys [title
                                                 vega-spec]}] v/registry]
                                 {:label title
                                  :vega-spec vega-spec
                                  :dispatch [:workbook.question.visualization/set-vis!
                                             workbook-id
                                             question-id
                                             visualization-id
                                             id]}))}]}))


(re-frame/reg-sub
 :workbook.question/visualization
 (fn [[_ ?workbook-id ?question-id _] _]
   (re-frame/subscribe [:workbook.question/visualizations ?workbook-id ?question-id]))
 (fn [vis-map [_ _ _ visualization-id]]
   (get vis-map visualization-id)))

(re-frame/reg-sub
 :workbook.question.visualization/vis
 (fn [[_ & args] _]
   (re-frame/subscribe (into [:workbook.question/visualization] args)))
 (fn [v _]
   (:vis v)))

(re-frame/reg-sub
 :workbook.question.visualization/vega-spec
 (fn [[_ w q v] _]
   [(re-frame/subscribe [:workbook.question.visualization/vis
                         w q v])
    (re-frame/subscribe [:workbook.question.function/result
                         w q])])
 (fn [[{:keys [id args]}
       result] _]
   (when (and result id args)
     (vis/prepare-vega-spec result id args))))

(re-frame/reg-sub
 :workbook.question.visualization/vega-spec-json
 (fn [[_ w q v] _]
   (re-frame/subscribe [:workbook.question.visualization/vega-spec
                        w q v]))
 (fn [vega-spec _]
   (clj->js vega-spec)))

(re-frame/reg-sub
 :workbook.question.visualization/vega-spec-json-pp
 (fn [[_ w q v] _]
   (re-frame/subscribe [:workbook.question.visualization/vega-spec-json
                        w q v]))
 (fn [vega-spec-json _]
   (when vega-spec-json
     (.stringify js/JSON vega-spec-json nil 2))))
