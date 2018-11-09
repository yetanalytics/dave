(ns com.yetanalytics.dave.ui.views
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.dave.ui.views.debug :as debug]
            [com.yetanalytics.dave.ui.views.snackbar :refer [snackbar]]
            [com.yetanalytics.dave.ui.views.root :as root]
            [cljs.pprint :refer [pprint]]))

(defmulti page
  "Main dispatch component for full-page context.
  Methods are defined corresponding to the :nav/context sub."
  identity)

(defmethod page :default [_]
  [:div.page.loading "Loading Dave..."])

(defmethod page :root [_]
  [root/page])

(defn loading-bar [loading?]
  [:div.mdc-linear-progress.mdc-linear-progress--indeterminate
   {:class (when-not loading?
             "mdc-linear-progress--closed")}
   [:div.mdc-linear-progress__buffering-dots]
   [:div.mdc-linear-progress__buffer]
   [:div.mdc-linear-progress__bar.mdc-linear-progress__primary-bar
    [:span.mdc-linear-progress__bar-inner]]
   [:div.mdc-linear-progress__bar.mdc-linear-progress__secondary-bar
    [:span.mdc-linear-progress__bar-inner]]])

(defn app []
  (let [context @(subscribe [:nav/context])]
    [:div.dave-app
     (when ^boolean goog.DEBUG
       [debug/debug-bar])
     ;; TODO: Title/top bar
     ;; TODO: nav/breadcrumbs
     [loading-bar (contains? #{:loading} context)]
     [page context]
     [snackbar]
     ]))
