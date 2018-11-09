(ns com.yetanalytics.dave.ui.views
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.dave.ui.views.debug :as debug]
            [com.yetanalytics.dave.ui.views.snackbar :refer [snackbar]]
            [cljs.pprint :refer [pprint]]))

(defn app []
  [:div
   (when ^boolean goog.DEBUG
     [debug/debug-bar])
   [:h3 "Hello DAVE!"]
   [:a {:href "/#/foo"} "foo"]
   [:pre (with-out-str (pprint @(subscribe [:db/debug])))]
   [snackbar]
   ])
