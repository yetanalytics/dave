(ns com.yetanalytics.dave.ui.app.dialog
  "Generic modal dialog show/hide functions"
  (:require [re-frame.core :as re-frame]
            [clojure.spec.alpha :as s]))

(s/def ::title
  string?)

(s/def ::mode
  #{::form})

(s/def ::dispatch-cancel
  (s/cat :event-id qualified-keyword?
         :args (s/* identity)))

(def dialog-common-spec
  (s/keys :req-un [::title
                   ::mode]
          :opt-un [::dispatch-cancel]))

(defmulti dialog-mode :mode)

(s/def :form.field/key
  keyword?)

(s/def :form.field/label
  string?)

(s/def :form.field/spec
  s/spec?)

(s/def :form/field
  (s/keys :req-un [:form.field/key
                   :form.field/label
                   ]
          :opt-un [:form.field/spec]))

(s/def ::fields
  (s/every :form/field))

(s/def ::form
  map?)

(s/def ::dispatch-save
  (s/cat :event-id qualified-keyword?
         :args (s/* identity)))



(defmethod dialog-mode ::form [_]
  (s/merge dialog-common-spec
           (s/keys :req-un [::fields
                            ::form
                            ::dispatch-save]
                   )))

(def dialog-spec
  (s/multi-spec dialog-mode :mode))

(re-frame/reg-event-db
 :dialog/dismiss
 (fn [db _]
   (dissoc db :dialog)))

(re-frame/reg-event-fx
 :dialog/cancel
 (fn [{:keys [db] :as ctx} _]
   (let [{:keys [dispatch-cancel] :as dialog} (:dialog db)]
     (cond-> {:db (dissoc db :dialog)}
       dispatch-cancel (assoc :dispatch (conj dispatch-cancel dialog))))))

;; Some actions for doing stuff
(re-frame/reg-event-fx
 :dialog.form/offer
 (fn [{:keys [db] :as ctx} [_ dialog-form-spec]]
   (if (s/valid? dialog-spec dialog-form-spec)
     {:db (assoc db :dialog dialog-form-spec)}
     (.error js/log "Invalid form dialog"
             (s/explain-str dialog-spec dialog-form-spec)))))

(re-frame/reg-event-db
 :dialog.form/update-field
 (fn [db [_ k v]]
   (assoc-in db [:dialog :form k] v)))

(re-frame/reg-event-fx
 :dialog.form/save
 (fn [{:keys [db] :as ctx} _]
   (let [{:keys [dispatch-save
                 form]} (:dialog db)]
     {;; pass the form result to the caller.
      ;; Let them decide whether or not to dismiss
      :dispatch (conj dispatch-save form)})))

;; General Subs
(re-frame/reg-sub
 ::dialog
 (fn [db _]
   (:dialog db)))

(re-frame/reg-sub
 :dialog/open?
 :<- [::dialog]
 (fn [dialog _]
   (some? dialog)))

(re-frame/reg-sub
 :dialog/title
 :<- [::dialog]
 (fn [dialog _]
   (:title dialog)))

(re-frame/reg-sub
 :dialog/mode
 :<- [::dialog]
 (fn [dialog _]
   (:mode dialog)))

;; Form Subs

(re-frame/reg-sub
 :dialog.form/fields
 :<- [::dialog]
 (fn [dialog _]
   (:fields dialog)))

(re-frame/reg-sub
 :dialog.form/form
 :<- [::dialog]
 (fn [dialog _]
   (:form dialog)))

(re-frame/reg-sub
 :dialog.form/form-field-val
 :<- [:dialog.form/form]
 (fn [form [_ k]]
   (get form k)))

#_(re-frame/reg-sub
 :dialog.form.fields/field
 :<- [:dialog.form/fields]
 (fn [fields [_ idx]]
   (get fields idx)))