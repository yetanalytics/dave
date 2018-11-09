(ns com.yetanalytics.dave.ui.views
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe]]
            [cljs.pprint :refer [pprint]]))

(defn app []
  [:div
   [:h3 "Hello DAVE!"]
   [:a {:href "/#/foo"} "foo"]
   [:pre (with-out-str (pprint @(subscribe [:db/debug])))]])
