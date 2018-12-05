(ns com.yetanalytics.dave.ui.views.nav
  "Header, footer and breadcrumb nav components"
  (:require [re-frame.core :refer [dispatch subscribe]]

            ;; TODO: remove
            [cljs.pprint :refer [pprint]]
            ))

(defn breadcrumbs
  []
  (let [path-items @(subscribe [:nav/path-items])]
    (into [:div.breadcrumbs]
          (for [item path-items]
            ^{:key (:id item)}
            [:div
             [:pre (with-out-str
                     (pprint item))]]))))

(defn top-bar-links
  []

  (into [:ul.top-bar-links]
        (for [[title href] [["Menu" "#/"]
                            ["More Info" "#/"]
                            ["Contribute" "#/"]
                            ["Google Group" "#/"]]]
          [:li [:a {:href href}
                title]])))

(defn top-bar
  []
  [:header.top-bar
   [:div ;row
    [:section
     [top-bar-links]]]])

(defn footer
  []
  [:footer
   ])
