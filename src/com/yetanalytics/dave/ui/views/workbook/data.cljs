(ns com.yetanalytics.dave.ui.views.workbook.data
  (:require [re-frame.core :refer [dispatch subscribe]]))

(defn details [?workbook-id]
  (let [{stamp-min :min
         stamp-max :max} @(subscribe [:workbook.data/timestamp-range ?workbook-id])]

    [:dl.details
     [:dt "Statements"]
     [:dd (str @(subscribe [:workbook.data/statement-count ?workbook-id]))]
     [:dt "First Timestamp"]
     [:dd stamp-min]
     [:dt "Last Timestamp"]
     [:dd stamp-max]]
    ))

(defn errors [?workbook-id]
  (let [errors @(subscribe [:workbook.data/errors ?workbook-id])]
    (when errors
      (into [:ul.errors]
            (for [[idx {:keys [message]}]
                  (map-indexed vector errors)]
              ^{:key (str "workbook-" (or ?workbook-id
                                          "current")
                          "-data-errors-"
                          idx)}
              [:li
               message])))))

(defn info
  [?workbook-id]
  [:div.data
   [:h3.title
    [:i.material-icons
     (case @(subscribe [:workbook.data/type ?workbook-id])
       :com.yetanalytics.dave.workbook.data/file "insert_drive_file"
       :com.yetanalytics.dave.workbook.data/lrs "storage")]
    @(subscribe [:workbook.data/title ?workbook-id])]
   [details ?workbook-id]
   [errors ?workbook-id]])
