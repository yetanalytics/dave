(ns com.yetanalytics.dave.ui.app.workbook.data
  (:require [re-frame.core :as re-frame]
            [com.yetanalytics.dave.workbook.data :as data]
            [com.yetanalytics.dave.workbook.data.state :as state]
            [com.yetanalytics.dave.workbook.data.lrs.client
             :as lrs-client]
            [clojure.core.async :as a :include-macros true]
            [clojure.spec.alpha :as s]
            [goog.string :refer [format]]
            [goog.string.format]))

(re-frame/reg-event-fx
 ::set-state
 ;; called for a data source when loading is complete.
 ;; funcs check if their state is = to this before deriving results.
 (fn [{:keys [db]}
      [_
       workbook-id
       new-state
       force?]]
   (let [new-db (assoc-in
                 (if force?
                   (assoc-in db
                             [:workbooks
                              workbook-id
                              :data
                              :state]
                             new-state)
                   (update-in db
                              [:workbooks
                               workbook-id
                               :data
                               :state]
                              (fnil
                               (partial max-key :statement-idx)
                               {:statement-idx -1})
                              new-state))
                 [:workbooks
                  workbook-id
                  :data
                  :loading?]
                 false)]
     (cond-> {:db new-db}
       (not= db new-db)
       (assoc :dispatch-n [[:workbook.question.function/result-all!
                            workbook-id]
                           [:db/save]])))))

(s/fdef fetch-fx
  :args (s/cat :data data/data-spec)
  :ret (s/nilable map?))

(defmulti fetch-fx (fn [_ data _]
                     (:type data)))

(defmethod fetch-fx :default [_ _ _] {})

(defmethod fetch-fx ::data/file
  [workbook-id
   {:keys [statements
           uri] :as data}
   _]
  (when-not statements
    {:http/request {:request {:url uri
                              :method :get}
                    :handler [::load
                              workbook-id
                              nil]}}))

(defmethod fetch-fx ::data/lrs
  [workbook-id
   {{:keys [statement-idx
            stored-domain]
     :as lrs-state} :state
    :as lrs-spec
    :or {lrs-state {:statement-idx -1}}}
   db]
  {:dispatch
   [:com.yetanalytics.dave.ui.app.workbook.data.lrs/query
    workbook-id
    lrs-spec
    {}]})

(re-frame/reg-event-fx
 ::ensure*
 (fn [{:keys [db] :as ctx} [_ workbook-id]]
   (if-let [data (get-in db [:workbooks
                             workbook-id
                             :data])]
     (if (s/valid? data/data-spec data)
       (if (:loading? data)
         {}
         (merge-with conj
                     {:db (assoc-in db [:workbooks
                                        workbook-id
                                        :data
                                        :loading?]
                                    true)}
                     ;; Ensure the timer is started if this is an LRS
                     (when (= (:type data) ::data/lrs)
                       {:dispatch-timer [[::ensure workbook-id]
                                         [::ensure workbook-id]
                                         10000]})
                     (fetch-fx workbook-id data db)))
       (.warn js/console "Skipped invalid Data source"))
     ;; Stop the timer if the data isn't present
     {:stop-timer [::ensure workbook-id]})))

(re-frame/reg-event-fx
 ::ensure
 (fn [_ [_ workbook-id]]
   #_(.log js/console "ensure!" workbook-id)
   {:dispatch-debounce
    [::ensure
     [::ensure* workbook-id]
     500]}))

(re-frame/reg-event-fx
 ::error! ;; replace errors
 (fn [{:keys [db] :as ctx} [_
                            workbook-id
                            error]]
   {:db (assoc-in db [:workbooks
                      workbook-id
                      :data
                      :errors]
                  [error])}))

(re-frame/reg-event-fx
 ::clear-errors
 (fn [{:keys [db] :as ctx} [_ workbook-id]]
   {:db (update-in db [:workbooks
                       workbook-id
                       :data]
                   dissoc
                   :errors)}))

(re-frame/reg-event-fx
 ::load
 (fn [{:keys [db] :as ctx}
      [_ workbook-id
       idx-range
       {:keys [status body] :as response}
       then-dispatch]]
   (let [
         {data-type :type
          data-state :state
          :as data} (get-in db
                            [:workbooks
                             workbook-id
                             :data])
         then-dispatch (case data-type
                         ::data/file
                         [::set-state
                          workbook-id
                          (state/update-state
                           {:statement-idx -1}
                           body)
                          true]
                         ::data/lrs
                         then-dispatch)]
     (if (= 200 status)
       {:dispatch-n [[:workbook.question.function/step-all!
                      workbook-id
                      idx-range
                      body
                      ;; pass continuation
                      then-dispatch]
                     [::clear-errors workbook-id]]}
       {:db (update-in db
                       [:workbooks
                        workbook-id
                        :data
                        :errors]
                       (fnil conj [])
                       {:type ::load-error
                        :message "Couldn't load data."
                        :workbook-id workbook-id
                        :response response})}))))
(re-frame/reg-event-fx
 ::change
 (fn [{:keys [db] :as ctx}
      [_
       workbook-id
       data-spec]]
   (cond-> {:db (assoc-in db
                          [:workbooks
                           workbook-id
                           :data]
                          ;; Force a fresh state
                          (merge data-spec
                                 {:state {:statement-idx -1}}))
            :dispatch [:workbook.question.function/reset-all!
                       workbook-id
                       [::ensure workbook-id]]
            #_(cond-> [:workbook.question.function/reset-all!
                               workbook-id
                               [::ensure workbook-id]]
                        (or (not (:wizard db))
                            (= (:type data-spec) ::data/file))
                        (conj [::ensure workbook-id]))
            }
     (= (:type data-spec) ::data/file)
     (assoc :stop-timer
            [::ensure workbook-id]))))

(re-frame/reg-event-fx
 ::check-lrs
 (fn [{:keys [db] :as ctx}
      [_
       lrs-spec
       dispatch-ok
       dispatch-error]]
   ;; Do an authenticated HEAD request
   {:http/request
    {:request {:url (str (:endpoint lrs-spec)
                         "/xapi/statements?limit=1")
               :headers {"X-Experience-Api-Version" "1.0.3"}
               :basic-auth (select-keys
                            (:auth lrs-spec)
                            [:username :password])
               :with-credentials? false
               :method :head}
     :handler dispatch-ok
     :error-handler dispatch-error}}))

(re-frame/reg-event-fx
 ::create-lrs
 (fn [{:keys [db] :as ctx}
      [_
       workbook-id
       lrs-data-spec
       ?check-resp]]
   (if-let [{:keys [success status] :as resp} ?check-resp]
     (if success
       ;; TODO: proceed with creation
       {:notify/snackbar
        {:message "Connecting LRS..."}
        :dispatch-n (cond-> [[::change
                              workbook-id
                              lrs-data-spec]]
                      ;; If we're in the wizard, don't dismiss the dialog
                      (not (:wizard db))
                      (conj [:dialog/dismiss])
                      ;; if we are, advance
                      #_(:wizard db)
                      #_(conj [:wizard/next])
                      )}
       {:notify/snackbar
        {:message
         (format "LRS Error: %d"
                 status)
         #_(condp contains? status
           #{401 403} "Invalid Credentials"
           #{404} "Invalid Endpoint"
           (str ))}}
       )
     (if (s/valid? data/data-spec lrs-data-spec)
       {:dispatch [::check-lrs
                   lrs-data-spec
                   [::create-lrs
                    workbook-id
                    lrs-data-spec]]}
       {:notify/snackbar
        {:message "Invalid LRS Info!"}}))))

;; Picker/selection
(re-frame/reg-event-fx
 :workbook.data/offer-picker
 (fn [{:keys [db] :as ctx} [_
                            workbook-id]]
   {:dispatch [:picker/offer
               {:title "Choose a Data Source"
                :choices
                [{:label "DAVE Test Dataset"
                  :img-src "/img/folder.png"
                  :dispatch [::change workbook-id
                             {:title "test dataset"
                              :type :com.yetanalytics.dave.workbook.data/file
                              :uri "data/dave/ds.json"
                              :built-in? true}]}
                 {:label "LRS Data"
                  :img-src "/img/db.png"
                  :dispatch
                  [:dialog.form/offer
                   {:title "LRS Data Info"
                    :mode :com.yetanalytics.dave.ui.app.dialog/form
                    :fields [{:key :title
                              :label "LRS Name"}
                             {:key :endpoint
                              :label "LRS Endpoint"}
                             {:key [:auth :username]
                              :label "API Key"}
                             {:key [:auth :password]
                              :label "API Key Secret"}]
                    :form {:type :com.yetanalytics.dave.workbook.data/lrs
                           :built-in? false
                           ;; :title "My LRS"
                           ;; :endpoint "http://localhost:9001"
                           :auth {;; :username "123456789"
                                  ;; :password "123456789"
                                  :type :com.yetanalytics.dave.workbook.data.lrs.auth/http-basic}}
                    :dispatch-save [::create-lrs workbook-id]}]}]}]}))

(re-frame/reg-sub
 :workbook/data
 (fn [[_ ?workbook-id] _]
   (if (some? ?workbook-id)
     (re-frame/subscribe [:workbook/lookup ?workbook-id])
     (re-frame/subscribe [:workbook/current])))
 (fn [workbook _]
   (:data workbook)))

(re-frame/reg-sub
 :workbook.data/title
 (fn [[_ ?workbook-id] _]
   (re-frame/subscribe [:workbook/data ?workbook-id]))
 (fn [data _]
   (:title data)))

(re-frame/reg-sub
 :workbook.data/type
 (fn [[_ ?workbook-id] _]
   (re-frame/subscribe [:workbook/data ?workbook-id]))
 (fn [data _]
   (:type data)))

(re-frame/reg-sub
 :workbook.data/state
 (fn [[_ ?workbook-id] _]
   (re-frame/subscribe [:workbook/data ?workbook-id]))
 (fn [data _]
   (:state data)))

(re-frame/reg-sub
 :workbook.data/loading?
 (fn [[_ ?workbook-id] _]
   (re-frame/subscribe [:workbook/data ?workbook-id]))
 (fn [data _]
   (:loading? data)))

(re-frame/reg-sub
 :workbook.data/errors
 (fn [[_ ?workbook-id] _]
   (re-frame/subscribe [:workbook/data ?workbook-id]))
 (fn [{:keys [errors] :as data} _]
   errors))

(re-frame/reg-sub
 :workbook.data.state/statement-idx
 (fn [[_ ?workbook-id] _]
   (re-frame/subscribe [:workbook.data/state ?workbook-id]))
 (fn [state _]
   (:statement-idx state -1)))

(re-frame/reg-sub
 :workbook.data.state/timestamp-domain
 (fn [[_ ?workbook-id] _]
   (re-frame/subscribe [:workbook.data/state ?workbook-id]))
 (fn [state _]
   (:timestamp-domain state)))

(re-frame/reg-sub
 :workbook.data/statement-count
 (fn [[_ ?workbook-id] _]
   (re-frame/subscribe [:workbook.data.state/statement-idx ?workbook-id]))
 (fn [statement-idx _]
   (inc statement-idx)))

(re-frame/reg-sub
 :workbook.data/timestamp-range
 (fn [[_ ?workbook-id] _]
   (re-frame/subscribe [:workbook.data.state/timestamp-domain ?workbook-id]))
 (fn [[mn mx :as timestamp-domain] _]
   {:min mn
    :max mx}))
