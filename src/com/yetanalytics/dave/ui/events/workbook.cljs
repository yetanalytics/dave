(ns com.yetanalytics.dave.ui.events.workbook
  (:require [re-frame.core :as re-frame]
            ))

;; Subs
(re-frame/reg-sub
 :workbook/map
 (fn [db _]
   (:workbooks db {})))

(re-frame/reg-sub
 :workbook/list
 ;; a stable (sorted) list
 ;; TODO: add some value for better sorting
 (fn [_ _] (re-frame/subscribe [:workbook/map]))
 (fn [workbooks _]
   (->> workbooks
        (sort-by first)
        (mapv second))))
