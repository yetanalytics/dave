(ns com.yetanalytics.dave.ui.app.workbook.data
  (:require [re-frame.core :as re-frame]
            [com.yetanalytics.dave.workbook.data :as data]
            [clojure.spec.alpha :as s]))

(s/fdef fetch-fx
  :args (s/cat :data data/data-spec)
  :ret (s/nilable map?))

(defmulti fetch-fx (fn [_ data]
                     (:type data)))

(defmethod fetch-fx :default [_ _] {})

(defmethod fetch-fx ::data/file
  [workbook-id
   {:keys [statements
           uri] :as data}]
  (when-not statements
    {:http/request {:request {:url uri
                              :method :get}
                    :handler [::load workbook-id]}}))

(re-frame/reg-event-fx
 ::ensure
 (fn [{:keys [db] :as ctx} [_ workbook-id]]
   (let [data (get-in db [:workbooks
                          workbook-id
                          :data])]
     (fetch-fx workbook-id data))))

(s/fdef load
  :args (s/cat :data data/data-spec
               :response map?)
  :ret data/data-spec)

(defmulti load (fn [data _]
                    (:type data)))

(defmethod load :default [_ _]
  {})

(defmethod load ::data/file
  [{:as data} {:keys [body] :as response}]
  (assoc data :statements body))

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
 (fn [{:keys [db] :as ctx} [_ workbook-id {:keys [status] :as response}]]
   (if (= 200 status)
     {:db (update-in db
                     [:workbooks
                      workbook-id
                      :data]
                     load
                     response)
      :dispatch [::clear-errors workbook-id]}
     {:db (update-in db
                     [:workbooks
                      workbook-id
                      :data
                      :errors]
                     (fnil conj [])
                     {:type ::load-error
                      :message "Couldn't load data."
                      :workbook-id workbook-id
                      :response response})})))

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
 (fn [workbook _]
   (:title workbook)))

(re-frame/reg-sub
 :workbook.data/type
 (fn [[_ ?workbook-id] _]
   (re-frame/subscribe [:workbook/data ?workbook-id]))
 (fn [workbook _]
   (:type workbook)))

(re-frame/reg-sub
 :workbook.data/statements
 (fn [[_ ?workbook-id] _]
   (re-frame/subscribe [:workbook/data ?workbook-id]))
 (fn [{:keys [statements] :as data} _]
   (into [] statements)))

(re-frame/reg-sub
 :workbook.data/errors
 (fn [[_ ?workbook-id] _]
   (re-frame/subscribe [:workbook/data ?workbook-id]))
 (fn [{:keys [errors] :as data} _]
   errors))

(re-frame/reg-sub
 :workbook.data/statement-count
 (fn [[_ ?workbook-id] _]
   (re-frame/subscribe [:workbook.data/statements ?workbook-id]))
 (fn [statements _]
   (count statements)))

(re-frame/reg-sub
 :workbook.data/timestamp-range
 (fn [[_ ?workbook-id] _]
   (re-frame/subscribe [:workbook.data/statements ?workbook-id]))
 (fn [statements _]
   (let [stamps (keep #(get % "timestamp") statements)
         ;; only convert times once per op
         get-t (memoize (fn [stamp] (.getTime (js/Date. stamp))))]
     (when (seq stamps)
       {:min (apply min-key get-t stamps)
        :max (apply max-key get-t stamps)}))))
