(ns com.yetanalytics.dave.ui.app.wizard
  (:require [re-frame.core :as re-frame]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.workbook :as workbook]))

(re-frame/reg-event-fx
 :wizard/start
 (fn [{:keys [db]}
      _]
   {:dispatch [:dialog/offer
               {:title "DAVE Wizard"
                :mode :com.yetanalytics.dave.ui.app.dialog/wizard
                :dispatch-cancel [:wizard/cancel]}]}))

;; proceeds, if possible
(re-frame/reg-event-fx
 :wizard/next
 (fn [{:keys [db]}
      _]
  ))


;; goes back, if possible
(re-frame/reg-event-fx
 :wizard/prev
 (fn [{:keys [db]}
      _]
   ))

(re-frame/reg-event-fx
 :wizard/cancel
 (fn [{:keys [db]}
      _]
   {:notify/snackbar
    {:message "Wizard Cancelled"}}))

;; completes, if possible
(re-frame/reg-event-fx
 :wizard/complete
 (fn [{:keys [db]}
      _]
   ))
