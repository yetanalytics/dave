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
 ::load
 (fn [{:keys [db] :as ctx} [_ workbook-id {:keys [status] :as response}]]
   (if (= 200 status)
     {:db (update-in db
                     [:workbooks
                      workbook-id
                      :data]
                     load
                     response)}
     (throw (ex-info "Workbook data load error"
                     {:type ::load-error
                      :workbook-id workbook-id
                      :response response})))))
