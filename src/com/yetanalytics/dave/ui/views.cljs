(ns com.yetanalytics.dave.ui.views
  (:require [reagent.core :as reagent]))

(defn app []
  [:div
   [:h3 "Hello DAVE!"]
   [:a {:href "/#/foo"} "foo"]])
