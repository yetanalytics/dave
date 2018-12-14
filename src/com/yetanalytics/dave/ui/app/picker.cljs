(ns com.yetanalytics.dave.ui.app.picker
  (:require [re-frame.core :as re-frame]
            [clojure.spec.alpha :as s]))


(s/def :choice/label
  string?)

(s/def :choice/img-src
  string?)

(s/def :choice/dispatch
  (s/cat :event-id qualified-keyword?
         :args (s/* identity)))

(s/def ::choice
  (s/keys :req-un [:choice/label
                   :choice/img-src
                   :choice/dispatch]))

(s/def ::choices
  (s/every ::choice))

(s/def ::title
  string?)

(def picker-spec
  (s/keys :req-un [::title
                   ::choices]))

(re-frame/reg-event-fx
 :picker/offer
 (fn [{:keys [db] :as ctx} [_ {:keys [title choices]}]]
   {:db (assoc db :picker {:title
                           title
                           :choices
                           choices})}))

(re-frame/reg-event-fx
 :picker/pick
 (fn [{:keys [db] :as ctx} [_ choice-idx]]
   (if-let [{:keys [dispatch] :as choice} (get-in db [:picker :choices choice-idx])]
     ;; TODO: DO something with choice
     {
      ;; dismiss the picker
      :dispatch-n [[:picker/dismiss]
                   dispatch]}

     {:notify/snackbar
      {:message "Choice not found!"
       :timeout 1000}})))

(re-frame/reg-event-fx
 :picker/dismiss
 (fn [{:keys [db] :as ctx} _]
   {:db (dissoc db :picker)}))

(re-frame/reg-sub
 ::picker
 (fn [db _]
   (:picker db)))

(re-frame/reg-sub
 :picker/title
 (fn [_ _]
   (re-frame/subscribe [::picker]))
 (fn [picker _]
   (:title picker)))

(re-frame/reg-sub
 :picker/choices
 (fn [_ _]
   (re-frame/subscribe [::picker]))
 (fn [picker _]
   (:choices picker)))
