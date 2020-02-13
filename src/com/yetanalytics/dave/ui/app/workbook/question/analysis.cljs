(ns com.yetanalytics.dave.ui.app.workbook.analysis
  (:require [clojure.spec.alpha                      :as s]
            [re-frame.core                           :as re-frame]
            [com.yetanalytics.dave.workbook.analysis :as analysis]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handlers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
 :workbook.analysis/new
 (fn [_ [_ workbook-id]]
   {:dispatch
    [:dialog.form/offer
     {:title         "New Analysis"
      :mode          :com.yetanalytics.dave.ui.app.dialog/form
      :dispatch-save [:workbook.analysis/create workbook-id]
      :fields        [{:key   :text
                       :label "Analysis Text"}]
      :form          {}}]}))

(re-frame/reg-event-fx
 :workbook.analysis/create
 (fn [_ [_ workbook-id form-map]]
   (let [{:keys [id]
          :as   new-analysis} (merge form-map
                                     {:id            (random-uuid)
                                      :index         0
                                      :visualization {}})]
     (if-let [spec-error (s/explain-data analysis/analysis-spec
                                         new-analysis)]
       {:notify/snackbar
        ;; TODO: HUMAN READ
        {:message "Invalid Analysis"}}
       (:dispatch-n [[:dialog/dismiss]
                     [:crud/create!
                      new-analysis
                      workbook-id
                      id]
                     [:workbook.analysis/after-create
                      workbook-id
                      id]])))))

(re-frame/reg-event-fx
 :workbook.analysis/after-create
 (fn [_ [_ workbook-id
         analysis-id]]
   {:com.yetanalytics.dave.ui.app.nav/nav-path! [:workbooks
                                                 workbook-id
                                                 :analyses
                                                 analysis-id]}))

(re-frame/reg-event-fx
 :workbook.analysis/edit
 (fn [{:keys [db]} [_
                    workbook-id
                    analysis-id]]
   (let [analysis (get-in db [:workbooks
                              workbook-id
                              :analyses
                              analysis-id])]
     {:dispatch
      [:dialog.form/offer
       {:title         "Edit Analysis"
        :mode          :com.yetanalytics.dave.ui.app.dialog/form
        :dispatch-save [:workbook.analysis/update
                        workbook-id
                        analysis-id]
        :fields        [{:key   :text
                         :label "Analysis Text"}]
        :form          (select-keys analysis [:text])}]})))

(re-frame/reg-event-fx
 :workbook.analysis/update
 (fn [{:keys [db]} [_
                    workbook-id
                    analysis-id
                    form-map]]
   (let [analysis         (get-in db [:workbooks
                                      workbook-id
                                      :analyses
                                      analysis-id])
         updated-analysis (merge analysis
                                 form-map)]
     (if-let [spec-error (s/explain-data analysis/analysis-spec
                                         updated-analysis)]
       {:notify/snackbar
        {:message "Invalid Analysis"}}
       {:dispatch-n [[:dialog/dismiss]
                     [:crud/update!
                      updated-analysis
                      workbook-id
                      analysis-id]]}))))

(re-frame/reg-event-fx
 :workbook.analysis/run
 (fn [{:keys [db]} [_
                    workbook-id
                    analysis-id]]
   (let [analysis (get-in db [:workbooks
                              workbook-id
                              :analyses
                              analysis-id])
         query    (-> analysis :query)
         viz      (-> analysis :viz)]
     (println (str query " - " viz))
     {})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Subs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-sub
 :workbook/analysis
 (fn [[_ ?workbook-id _] _]
   [(if (some? ?workbook-id)
      (re-frame/subscribe [:workbook/lookup ?workbook-id])
      (re-frame/subscribe [:workbook/current]))
    (re-frame/subscribe [:nav/path])])
 (fn [[workbook
       [_ _ _ ?path-analysis-id & _ :as path]] [_
                                                _
                                                ?analysis-id]]
   (get-in workbook [:analyses
                     (or ?analysis-id
                         ?path-analysis-id)])))

(re-frame/reg-sub
 :workbook.analysis/text
 (fn [[_ & args] _]
   (re-frame/subscribe (into [:workbook/analysis] args)))
 (fn [analysis _]
   (:text analysis)))

(re-frame/reg-sub
 :workbook.analysis/query
 (fn [[_ & args] _]
   (re-frame/subscribe (into [:workbook/analysis] args)))
 (fn [analysis _]
   (:query analysis)))

(re-frame/reg-sub
 :workbook.analysis/vega
 (fn [[_ & args] _]
   (re-frame/subscribe (into [:workbook/analysis] args)))
 (fn [analysis _]
   (:vega analysis)))

(re-frame/reg-sub
 :workbook.analysis/visualization
 (fn [[_ & args] _]
   (re-frame/subscribe (into [:workbook/analysis] args)))
 (fn [analysis _]
   (:visualization analysis)))
