(ns com.yetanalytics.dave.ui.views.nav
  "Header, footer and breadcrumb nav components"
  (:require [re-frame.core :refer [dispatch subscribe]]
            [clojure.string :as cs]
            [goog.string :refer [format]]
            [goog.string.format]
            ;; TODO: remove
            [cljs.pprint :refer [pprint]]
            ))

(defn crumb [{:keys [title text active?
                     href]}]
  [:div
   {:class (when active? "active")}
   [:a {:href (when href
                href)}

    [:div.title title]
    [:div.text text]]])

(defn breadcrumbs
  "Based on context/path, display the DAVE breadcrumb nav to the user."
  []
  (let [context @(subscribe [:nav/context])
        [?workbook
         ?question
         ?visualization] @(subscribe [:nav/path-items])]
    [:div.breadcrumbs
     [:div ;; inner
      [crumb
       {:title "DAVE"
        :text "Some text about the workbook list"
        :active? (= :root context)
        :href "#/"}]
      [crumb
       {:title "Workbook"
        :text "Some text about workbooks"
        :active? (= :workbook context)
        :href (when ?workbook
                (format "#/workbooks/%s" (:id ?workbook)))}
       ]
      [crumb
       {:title "Question"
        :text "Some text about questions"
        :active? (= :question context)
        :href (when ?question
                ;; We know that if a question is there, a workbook is too
                (format "#/workbooks/%s/questions/%s"
                        (:id ?workbook)
                        (:id ?question)))}]
      [crumb
       {:title "Visualization"
        :text "Some text about visualizations"
        :active? (= :visualization context)
        :href (when ?visualization
                (format "#/workbooks/%s/questions/%s/visualizations/%s"
                        (:id ?workbook)
                        (:id ?question)
                        (:id ?visualization)))}]]]))

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
