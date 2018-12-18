(ns com.yetanalytics.dave.ui.app.crud
  "CRUD operations for the DAVE object hierarchy"
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-event-fx
 :crud/delete-confirm
 (fn [{:keys [db] :as ctx} [_ & delete-args :as wat]]
   {:notify/snackbar
    {:message "Are you sure?"
     :actionText "Delete"
     :actionHandler (into [::delete!] delete-args)
     }}))

(defn- re-index
  "Re index a map with indexed values"
  [m]
  (into {}
        (map-indexed (fn [idx [k v]]
                       [k (assoc v :index idx)])
                     (sort-by (comp :index val)
                              m))))

(re-frame/reg-event-fx
 ::delete!
 (fn [{:keys [db] :as ctx} [_ & object-path]]
   (let [db-path (interleave [:workbooks :questions :visualizations]
                             object-path)
         parent-map-path (butlast db-path)
         new-db (-> db
                    (update-in parent-map-path
                               dissoc
                               (last db-path))
                    (update-in parent-map-path
                               re-index))
         parent-path (butlast parent-map-path)]
     {:db new-db
      :db/save! new-db
      :notify/snackbar
      {:message "Deleted!"}
      :com.yetanalytics.dave.ui.app.nav/nav-path! parent-path})))

(re-frame/reg-event-fx
 :crud/create!
 (fn [{:keys [db] :as ctx} [_ item & object-path]]
   (let [db-path (interleave [:workbooks :questions :visualizations]
                             object-path)
         parent-map-path (butlast db-path)
         new-db (-> db
                    (assoc-in db-path
                              item)
                    (update-in parent-map-path
                               re-index))]
     {:db new-db
      :db/save! new-db
      :notify/snackbar
      {:message "Success"}})))

;; Really just the same as update!, but gives us flexibility
(re-frame/reg-event-fx
 :crud/update!
 (fn [{:keys [db] :as ctx} [_ item & object-path]]
   (let [db-path (interleave [:workbooks :questions :visualizations]
                             object-path)
         parent-map-path (butlast db-path)
         new-db (-> db
                    (assoc-in db-path
                              item)
                    (update-in parent-map-path
                               re-index))]
     {:db new-db
      :db/save! new-db
      :notify/snackbar
      {:message "Success"}})))
