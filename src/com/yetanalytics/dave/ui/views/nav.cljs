(ns com.yetanalytics.dave.ui.views.nav
  "Header, footer and breadcrumb nav components"
  (:require [re-frame.core :refer [dispatch subscribe]]

            ;; TODO: remove
            [cljs.pprint :refer [pprint]]
            ))

(defn crumb [title text active?]
  [:div
   {:class (when active? "active")}
   [:div.title title]
   [:div.text text]])

(defn breadcrumbs
  "Based on context/path, display the DAVE breadcrumb nav to the user."
  []
  (let [context @(subscribe [:nav/context])]
    [:div.breadcrumbs
     [:div ;; inner
      [crumb
       "DAVE"
       "Some text about the workbook list"
       (= :root context)]
      [crumb
       "Workbook"
       "Some text about workbooks"
       (= :workbook context)]
      [crumb
       "Question"
       "Some text about questions"
       (= :question context)]
      [crumb
       "Visualization"
       "Some text about visualizations"
       (= :visualization context)]
      ]]))

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
