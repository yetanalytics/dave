(ns com.yetanalytics.dave.ui.app.workbook.question
  (:require [re-frame.core :as re-frame]))

;; Subs

(re-frame/reg-sub
 :workbook/question
 (fn [[_ ?workbook-id _] _]
   [(if (some? ?workbook-id)
     (re-frame/subscribe [:workbook/lookup ?workbook-id])
     (re-frame/subscribe [:workbook/current]))
    (re-frame/subscribe [:nav/path])])
 (fn [[workbook
       [_ _ _ ?path-question-id & _ :as path]] [_
                                                _ ;; workbook-id, not used
                                                ?question-id]]
   (get-in workbook [:questions
                     (or ?question-id
                         ?path-question-id)])))

(re-frame/reg-sub
 :workbook.question/text
 (fn [[_ & args] _]
   (re-frame/subscribe (into [:workbook/question] args)))
 (fn [question _]
   (:text question)))

(re-frame/reg-sub
 :workbook.question/visualizations
 (fn [[_ & args] _]
   (re-frame/subscribe (into [:workbook/question] args)))
 (fn [question _]
   (:visualizations question)))
