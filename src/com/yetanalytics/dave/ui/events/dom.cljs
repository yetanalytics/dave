(ns com.yetanalytics.dave.ui.events.dom
  (:require [goog.dom :as gdom]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [com.yetanalytics.dave.ui.views :as views]))

(re-frame/reg-cofx
 ::app-element
 (fn [cofx _]
   (assoc cofx ::app-element (gdom/getElement "app"))))

(re-frame/reg-fx
 ::render
 (fn [{:keys [element
              component]}]
   (reagent/render-component component element)))

(re-frame/reg-event-fx
 ::render-app
 [(re-frame/inject-cofx ::app-element)]
 (fn [{element ::app-element
       db :db} _]
   {::render {:element element
              :component [views/app]}}))
