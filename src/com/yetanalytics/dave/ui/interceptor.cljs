(ns com.yetanalytics.dave.ui.interceptor
  (:require [re-frame.core :as re-frame]))


(def persist-interceptor
  (re-frame/->interceptor
   :id :persist
   :after
   (fn [ctx]
     (let [db-state (-> ctx
                        :effects
                        :db)]
       (assoc-in ctx [:effects :db/save!] db-state)))))
