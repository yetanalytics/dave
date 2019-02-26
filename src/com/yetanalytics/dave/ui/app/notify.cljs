(ns com.yetanalytics.dave.ui.app.notify
  (:require
   ["@material/snackbar" :refer [MDCSnackbar]]
   [clojure.spec.alpha :as s]
   [re-frame.core :as re-frame]))

(defonce snackbar
  (delay (MDCSnackbar.
          (.querySelector js/document ".mdc-snackbar"))))

(s/def ::message
  string?)

(s/def ::timeout
  pos-int?)

(s/def ::actionHandler
  (s/cat :event qualified-keyword?
         :args (s/* identity)))

(s/def ::actionText
  string?)

(s/def ::multiline
  boolean?)

(s/def ::actionOnBottom
  boolean?)

(s/def ::snackbar-args
  (s/keys :req-un [::message]
          :opt-un [::actionHandler
                   ::actionText
                   ::timeout
                   ::multiline
                   ::actionOnBottom]))

(re-frame/reg-fx
 :notify/snackbar
 (fn [args]
   (if (s/valid? ::snackbar-args args)
     (let [{:keys [actionHandler] :as conformed} (s/conform ::snackbar-args args)]
       (.show @snackbar (clj->js
                         (cond-> args
                           actionHandler
                           (assoc :actionHandler
                                  (let [event-vec (into []
                                                        (s/unform ::actionHandler
                                                                  actionHandler))]
                                    #(re-frame/dispatch event-vec)))))))
     (throw (ex-info "Invalid Snackbar Args!"
                     {:type ::invalid-args
                      :args args
                      :spec-error (s/explain-data ::snackbar-args
                                                  args)})))))

;; Easy passthru that can be called from re-frame/dispatch
(re-frame/reg-event-fx
 :notify/snackbar
 (fn [_
      [_ args]]
   {:notify/snackbar args}))
