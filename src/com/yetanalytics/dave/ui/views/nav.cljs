(ns com.yetanalytics.dave.ui.views.nav
  "Header, footer and breadcrumb nav components"
  (:require [re-frame.core :refer [dispatch subscribe]]
            [clojure.string :as cs]
            [goog.string :refer [format]]
            [goog.string.format]
            ;; TODO: remove
            [cljs.pprint :refer [pprint]]))


(defn hometitle
  []
  [:div.workbookinfo
   [:p.hometitle "Workbook"]
   [:p.workbookdesc "Workbooks wrap your data in a group so they can be broken down into more informal details. Select one workbook to get started, and next select the specific question you want answered."]
   [:div.tag
    [:p " Total Workbooks 2"]]])


(defn app-description
  "Small description of the app that appears on every page and allows the user
   to launch the new work wizard."
  []
  [:div.app-description
   [:h2.title
    "Data Analytics and Visualization Efficiency Framework for xAPI and the Total Learning Architecture"]
   [:p.description
    "If the objective is to analyze, interpret, and visualize micro-level behavior-driven learning, we need a framework for analysis and visualization which aligns with xAPI, xAPI Profiles, and the Total Learning Architecture (TLA)."]
   [:button "Create Your Own Report"]])




(defn crumb
  "A single breadcrumb box"
  [{:keys [title text active?
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
     [:div.breadcrumbcorner ;; inner
      [crumb
       {:title "DAVE"
        :text "DAVE provides a framework for increasing the efficiency of implementing learning analytics and creating data visualizations. Search learning domain problem types and access the relevant information needed to create meaningful data visualizations. Tweak the visualization to meet your needs and issue a report."
        :active? (= :root context)
        :href "#/"}]
      [crumb
       {:title (if ?workbook
                 (format "Workbook: %s" (:title ?workbook))
                 "Workbooks")
        :text (if ?workbook
                (:description ?workbook)
                "Workbooks wrap your functions and visualizations into an easily accessible space. ")
        :active? (= :workbook context)
        :href (when ?workbook
                (format "#/workbooks/%s" (:id ?workbook)))}]

      ;; TODO: figure out contextual behaviour for questions/vis
      [crumb
       {:title (if ?question
                 (format "Question %d" (inc (:index ?question)))
                 "Questions")
        :text (if ?question
                (:text ?question)
                "Questions feature learning-domain problems. ")
        :active? (= :question context)
        :href (when ?question
                ;; We know that if a question is there, a workbook is too
                (format "#/workbooks/%s/questions/%s"
                        (:id ?workbook)
                        (:id ?question)))}]
      [crumb
       {:title (if ?visualization
                 (format "Visualization %d" (inc (:index ?visualization)))
                 "Visualizations")
        ;; TODO: something
        :text "Visualizations make insights accessible to a wide audience."
        :active? (= :visualization context)
        :href (when ?visualization
                (format "#/workbooks/%s/questions/%s/visualizations/%s"
                        (:id ?workbook)
                        (:id ?question)
                        (:id ?visualization)))}]]]))


(defn topmenu
  []
  [:div.topmenu
   [:img {:src "/img/dev/dave_logo.png"}]
   [:a {:href "#/"} "About"]
   [:a {:href "#/"} "Contribute"]
   [:a {:href "#/"} "Contact"]
   [:a {:href "#/"} "Yet Analytics"]])

(defn top-bar-links
  "The links in the top app bar"
  []
  [:div.menuitems
   (into [:ul.top-bar-links]
         (for [[title href] [
                             ["Menu" "#/"]
                             ["More Info" "#/"]
                             ["Contribute" "#/"]
                             ["Google Group" "#/"]]]
           [:li [:a {:href href}
                 title]]))])

(defn top-bar
  "The top bar of the application"
  []
  [:header.top-bar
   [:div ;row
    [:section
     [top-bar-links]]]])

(defn footer
  "The footer at the bottom of the app."
  []
  [:footer
   [:img {:src "/img/dev/white_text_logo.png"}]
   [:a {:href "#/"} "About"]
   [:a {:href "#/"} "More Info"]
   [:a {:href "#/"} "Contribute"]
   [:a {:href "#/"} "Contact"]
   [:a {:href "#/"} "Yet Analytics"]])
