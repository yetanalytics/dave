(ns com.yetanalytics.dave.ui.app.workbook.question.visualization
  (:require [re-frame.core :as re-frame]
            [com.yetanalytics.dave.workbook.question.visualization :as vis]))

(re-frame/reg-sub
 :workbook.question/visualization
 (fn [[_ ?workbook-id ?question-id _] _]
   (re-frame/subscribe [:workbook.question/visualizations ?workbook-id ?question-id]))
 (fn [vis-map [_ _ _ visualization-id]]
   (get vis-map visualization-id)))

(re-frame/reg-sub
 :workbook.question.visualization/vis
 (fn [[_ & args] _]
   (re-frame/subscribe (into [:workbook.question/visualization] args)))
 (fn [v _]
   (:vis v)))

(re-frame/reg-sub
 :workbook.question.visualization/vega-spec
 (fn [[_ w q v] _]
   [(re-frame/subscribe [:workbook.question.visualization/vis
                         w q v])
    (re-frame/subscribe [:workbook.question.function/result
                         w q])])
 (fn [[{:keys [id args]}
       result] _]
   (when (and result id args)
     (vis/prepare-vega-spec result id args))))
