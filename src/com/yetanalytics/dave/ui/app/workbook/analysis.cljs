(ns com.yetanalytics.dave.ui.app.workbook.analysis
  (:require [clojure.spec.alpha                      :as s]
            [re-frame.core                           :as re-frame]
            [com.yetanalytics.dave.datalog           :as d]
            [com.yetanalytics.dave.workbook.analysis :as analysis]
            [com.yetanalytics.dave.ui.util.vega      :as vu]))

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
                                     {:id    (random-uuid)
                                      :index 0
                                      :query ""
                                      :vega  ""})]
     (if-let [spec-error (s/explain-data analysis/analysis-spec
                                         new-analysis)]
       {:notify/snackbar
        ;; TODO: HUMAN READ
        {:message "Invalid Analysis"}}
       {:dispatch-n [[:dialog/dismiss]
                     [:crud/create!
                      new-analysis
                      workbook-id
                      id]
                     [:workbook.analysis/after-create
                      workbook-id
                      id]]}))))

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
                                      analysis-id])]
     {:dispatch-n [[:dialog/dismiss]
                   [:crud/update-silent!
                    (analysis/update-analysis analysis form-map)
                    workbook-id
                    analysis-id]]})))

(re-frame/reg-event-fx
 :workbook.analysis/run
 (fn [{:keys [db]} [_
                    workbook-id
                    analysis-id]]
   (try (let [{:keys [query-data]
               :as analysis}
              (get-in db [:workbooks
                          workbook-id
                          :analyses
                          analysis-id])]
          (when query-data
            (if-let [db (get-in db [:workbooks
                                    workbook-id
                                    :data
                                    :state
                                    :db])]
              (try (let [result (d/q query-data db)]
                     {:dispatch [:crud/update!
                                 (assoc analysis :result result)
                                 workbook-id
                                 analysis-id]})
                   (catch js/Error e
                     {:notify/snackbar
                      ;; TODO: HUMAN READ
                      {:timeout 5000
                       :message (str "Query Error! " (ex-message e))}}))
              {:notify/snackbar
               ;; TODO: HUMAN READ
               {:message "Can't find db"}})))
        (catch js/Error e
          {:notify/snackbar
           {:message (str "Can't query: " (ex-message e))}}))))

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
   (:query analysis
           ;; derive from query-data if not available.
           (when-let [qd (:query-data analysis)]
             (pr-str qd)))))

(re-frame/reg-sub
 :workbook.analysis/query-data
 (fn [[_ & args] _]
   (re-frame/subscribe (into [:workbook/analysis] args)))
 (fn [analysis _]
   (:query-data analysis)))

(re-frame/reg-sub
 :workbook.analysis/query-parse-error
 (fn [[_ & args] _]
   (re-frame/subscribe (into [:workbook/analysis] args)))
 (fn [analysis _]
   (:query-parse-error analysis)))

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

(re-frame/reg-sub
 :workbook.analysis/result
 (fn [[_ & args] _]
   (re-frame/subscribe (into [:workbook/analysis] args)))
 (fn [analysis _]
   (:result analysis)))

(re-frame/reg-sub
 :workbook.analysis/query-find-bindings
 (fn [[_ & args] _]
   (re-frame/subscribe (into [:workbook.analysis/query-data] args)))
 (fn [query-data _]
   (when query-data
     (d/describe-query-find query-data))))

(re-frame/reg-sub
 :workbook.analysis/visualization-fields
 (fn [[_ & args] _]
   (re-frame/subscribe (into [:workbook.analysis/visualization] args)))
 (fn [vis _]
   (when vis
     (analysis/vis-fields vis))))

(re-frame/reg-sub
 :workbook.analysis/result-vega-spec
 (fn [[_ & args] _]
   [(re-frame/subscribe (into [:workbook.analysis/result] args))
    (re-frame/subscribe (into [:workbook.analysis/visualization] args))
    (re-frame/subscribe (into [:workbook.analysis/query-data] args))])
 (fn [[result vis query-data] _]
   (if result
     (if vis
       (if query-data
         (analysis/result-vega-spec vis result query-data))))))

(re-frame/reg-sub
 :workbook.analysis/visualization-parse-error
 (fn [[_ & args] _]
   (re-frame/subscribe (into [:workbook.analysis/result-vega-spec] args)))
 (fn [vis _]
   (if vis
     (vu/parse-error vis))))
