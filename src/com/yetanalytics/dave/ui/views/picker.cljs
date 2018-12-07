(ns com.yetanalytics.dave.ui.views.picker
  (:require [reagent.core :as r]
            [re-frame.core :refer [dispatch subscribe]]))

(defn choice
  [idx {:keys [label img-src] :as choice}]
  [:li
   {:on-click #(dispatch [:picker/pick idx])}
   [:div
    [:img {:src img-src}]]
   [:div
    [:span label]]])

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
    (cond-> [:div.picker
             {:class (when title
                       "active")}
             [:i.material-icons.dismiss
              {:on-click #(dispatch [:picker/dismiss])}
              "close"]]
      title (conj
             [:h2 title]
             [choice-list]))))


(defn debug-button
  []
  [:button
   {:on-click #(dispatch [:picker/offer {:title "Pick something" :choices [{:label "Label"
                                                                            :img-src "https://material-components.github.io/material-components-web-catalog/static/media/photos/3x2/1.jpg"}]}])}
   "Picker debug"])
(comment
  [:li
   [:div
    [:img {:src "https://material-components.github.io/material-components-web-catalog/static/media/photos/3x2/1.jpg"}]]
   [:div
    [:span "Label"]]]

  (dispatch [:picker/offer {:title "Pick something" :choices [{:label "Label"
                                                               :img-src "https://material-components.github.io/material-components-web-catalog/static/media/photos/3x2/1.jpg"}]}])
  )
