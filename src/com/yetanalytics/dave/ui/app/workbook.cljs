(ns com.yetanalytics.dave.ui.app.workbook
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
        (sort-by (comp
                  :index
                  second))
        (mapv second))))

(re-frame/reg-sub
 :workbook/current
 (fn [_ _]
   [(re-frame/subscribe [:nav/path])
    (re-frame/subscribe [:workbook/map])])
 (fn [[[p0 ?workbook-id & _]
       workbook-map] _]
   (when (= p0 :workbooks)
     (get workbook-map ?workbook-id))))

(re-frame/reg-sub
 :workbook/lookup
 (fn [_ _]
   (re-frame/subscribe [:workbook/map]))
 (fn [workbook-map [_ workbook-id]]
   (get workbook-map workbook-id)))

(re-frame/reg-sub
 :workbook/questions
 (fn [[_ workbook-id] _]
   (re-frame/subscribe [:workbook/lookup workbook-id]))
 (fn [{:keys [questions]} _]
   questions))

(re-frame/reg-sub
 :workbook/question-count
 (fn [[_ workbook-id] _]
   (re-frame/subscribe [:workbook/questions workbook-id]))
 (fn [questions _]
   (count questions)))

;; Collect a map of all vis for a workbook
(re-frame/reg-sub
 :workbook/visualizations
 (fn [[_ workbook-id] _]
   (re-frame/subscribe [:workbook/questions workbook-id]))
 (fn [questions _]
   (reduce conj
           {}
           (for [[_ {:keys [visualizations]}] questions
                 v visualizations]
             v))))

(re-frame/reg-sub
 :workbook/visualization-count
 (fn [[_ workbook-id] _]
   (re-frame/subscribe [:workbook/visualizations workbook-id]))
 (fn [visualizations _]
   (count visualizations)))
