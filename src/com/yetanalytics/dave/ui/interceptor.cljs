(ns com.yetanalytics.dave.ui.interceptor)

(def persist-interceptor
  {:id ::persist
   :after
   (fn [ctx]
     (let [db-state (-> ctx
                        :coeffects
                        :db)]
       (assoc-in ctx [:effects :db/save!] db-state)))})
