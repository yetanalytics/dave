(ns com.yetanalytics.dave.ui.views
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.dave.ui.views.snackbar :refer [snackbar]]
            [com.yetanalytics.dave.ui.views.root :as root]
            [com.yetanalytics.dave.ui.views.workbook :as workbook]
            [com.yetanalytics.dave.ui.views.workbook.question :as question]
            [com.yetanalytics.dave.ui.views.workbook.question.visualization
             :as visualization]
            [com.yetanalytics.dave.ui.views.nav :as nav]
            [cljs.pprint :refer [pprint]]
            [com.yetanalytics.dave.ui.views.picker :as picker]))

(defmulti page
  "Main dispatch component for full-page context.
  Methods are defined corresponding to the :nav/context sub."
  identity)

(defmethod page :default [_]
  [:div.page.loading "Loading Dave..."])

(defmethod page :root [_]
  [root/page])

(defmethod page :workbook [_]
  [workbook/page])

(defmethod page :question [_]
  [question/page])

(defmethod page :visualization [_]
  [visualization/page])

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

(when ^boolean goog.DEBUG
  (require 'dave.debug))

(defn app []
  (let [context @(subscribe [:nav/context])]
    [:div.dave-app
     #_(when ^boolean goog.DEBUG
         [dave.debug/debug-bar])
     ;; App description floats in the upper right
     [nav/app-description]
     ;; the top bar dominates the header area
     [nav/top-bar]
     ;; single-line loading bar
     [loading-bar (contains? #{:loading} context)]
     ;; Breadcrumb nav expresses context and allows tree nav
     [nav/breadcrumbs]
     ;; The page changes depending on context
     [nav/hometitle]
     ;;[nav/hometitle]
     [page context]
     ;; Static Footer
     [nav/footer]
     ;; Picker overlay
     [picker/picker]
     ;; mdc snackbar overlay
     [snackbar]
     ]))
