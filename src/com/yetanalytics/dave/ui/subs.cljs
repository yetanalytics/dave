(ns com.yetanalytics.dave.ui.subs
  (:require [re-frame.core                   :refer [reg-sub subscribe]]
            [com.yetanalytics.dave.ui.app.db :as db]))

(reg-sub
 :db/analysis
 (fn [db _]
   (::db/analysis db)))

(reg-sub
 :analysis/query
 (fn [_ _]
   (subscribe [:db/analysis]))
 (fn [analysis _]
   (:analysis/query analysis)))

(reg-sub
 :analysis/viz
 (fn [_ _]
   (subscribe [:db/analysis]))
 (fn [analysis _]
   (:analysis/viz analysis)))

(reg-sub
 :analysis/render
 (fn [_ _]
   (subscribe [:db/analysis]))
 (fn [analysis _]
   (:analysis/render analysis)))
