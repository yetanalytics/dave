(ns com.yetanalytics.dave.ui.views.picker
  )

(defn image-list
  []
  [:ul.image-list
   [:li
    [:div
     [:img {:src "https://material-components.github.io/material-components-web-catalog/static/media/photos/3x2/1.jpg"}]]
    [:div
     [:span "Label"]]]
   [:li
    [:div
     [:img {:src "https://material-components.github.io/material-components-web-catalog/static/media/photos/3x2/1.jpg"}]]
    [:div
     [:span "Label"]]]
   [:li
    [:div
     [:img {:src "https://material-components.github.io/material-components-web-catalog/static/media/photos/3x2/1.jpg"}]]
    [:div
     [:span "Label"]]]])
