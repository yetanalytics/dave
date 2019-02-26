(ns com.yetanalytics.dave.ui.views.picker
  (:require [reagent.core :as r]
            [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.dave.ui.views.vega :as vega]))

(defn choice
  [idx {:keys [label
               img-src
               vega-spec] :as choice}]
  [:li
   {:on-click #(dispatch [:picker/pick idx])}
   (cond
     img-src
     [:div
      [:img {:src img-src}]]
     vega-spec
     [vega/vega vega-spec]
     :else (throw (ex-info "No image or vega spec provided!"
                           {:type ::no-image
                            :choice choice})))
   [:div
    [:span.minorbutton label]]])

(defn choice-list
  []
  (let [choices @(subscribe [:picker/choices])]
    (into [:ul]
          (for [[idx c] (map-indexed vector choices)]
            ^{:key (str "picker-choice-" idx)}
            [choice idx c]))))

(defn picker
  []
  (let [title @(subscribe [:picker/title])]
  [:div.picker
           {:class (when title
                     "active")}
   [:div.picker-header
    [:h1 title
    [:i.material-icons.dismiss
     {:on-click #(dispatch [:picker/dismiss])}
     "close"]]
    ]
   [choice-list]]))
