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
     [:dd stamp-max]]))


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

(defn change-button [workbook-id]
  [:button.majorbuttonpurple
   {:on-click #(dispatch [:workbook.data/offer-picker
                          workbook-id])}
   "Select Dataset"])

(defn loading-bar [workbook-id]
  [:div.mdc-linear-progress.mdc-linear-progress--indeterminate
   {:class (when-not @(subscribe [:workbook.data/loading? workbook-id])
             "mdc-linear-progress--closed")}
   [:div.mdc-linear-progress__buffering-dots]
   [:div.mdc-linear-progress__buffer]
   [:div.mdc-linear-progress__bar.mdc-linear-progress__primary-bar
    [:span.mdc-linear-progress__bar-inner]]
   [:div.mdc-linear-progress__bar.mdc-linear-progress__secondary-bar
    [:span.mdc-linear-progress__bar-inner]]])

(defn info
  [?workbook-id]
  [:div.data
   [loading-bar ?workbook-id]
   [:h3.title
    [:i.material-icons
     (case @(subscribe [:workbook.data/type ?workbook-id])
       :com.yetanalytics.dave.workbook.data/file "insert_drive_file"
       :com.yetanalytics.dave.workbook.data/lrs "storage")]
    @(subscribe [:workbook.data/title ?workbook-id])]
   [change-button ?workbook-id]
   [details ?workbook-id]
   [errors ?workbook-id]])
