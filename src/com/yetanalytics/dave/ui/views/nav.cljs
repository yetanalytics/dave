(ns com.yetanalytics.dave.ui.views.nav
  "Header, footer and breadcrumb nav components"
  (:require [re-frame.core :refer [dispatch subscribe]]
            [clojure.string :as cs]
            [goog.string :refer [format]]
            [goog.string.format]
            ;; TODO: remove
            #_[cljs.pprint :refer [pprint]]))


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
   ; [:h2.title
   ;  "Data Analytics and Visualization Environment for xAPI and the Total Learning Architecture"]
   #_[:p.description
    "If the objective is to analyze, interpret, and visualize micro-level behavior-driven learning, we need a framework for analysis and visualization which aligns with xAPI, xAPI Profiles, and the Total Learning Architecture (TLA)."]
   #_[:button
    {:on-click #(dispatch [:wizard/start])}
    "Create Your First Workbook"]])




(defn crumb
  "A single breadcrumb box"
  [{:keys [title text active?
           href]}]
  [:div
   {:class (when active? "active")}
   [:a {:href (when href
                href)}

    [:div.title title]
    #_[:div.text text]]])

(defn breadcrumbs
  "Based on context/path, display the DAVE breadcrumb nav to the user."
  []
  (let [context @(subscribe [:nav/context])
        [?workbook
         ?analysis
         #_#_?question
         ?visualization] @(subscribe [:nav/path-items])]
    [:div.breadcrumbs
     [:div.breadcrumbcorner ;; inner
      [crumb
       {:title "Library"
        :text "DAVE provides a framework for increasing the efficiency of implementing learning analytics and creating data visualizations. "
        :active? (= :root context)
        :href "#/"}]
      [crumb
       {:title (if ?workbook
                 (format "Workbook: %s" (:title ?workbook))
                 "Workbooks")
        :text (if ?workbook
                (:description ?workbook)
                "Workbooks wrap your questions and visualizations into an easily accessible space. ")
        :active? (= :workbook context)
        :href (when ?workbook
                (format "#/workbooks/%s" (:id ?workbook)))}]
      ;; TODO: figure out contextual behaviour for questions/vis
      [crumb
       {:title   (if ?analysis
                   (format "Analysis %d" (inc (:index ?analysis)))
                   "Analyses")
        :text    (if ?analysis
                   (:text ?analysis)
                   "Analyses feature custom queries and data viz to gain insight.")
        :active? (= :analysis context)
        :href    (when ?analysis
                   (format "#/workbooks/%s/analyses/%s"
                           (:id ?workbook)
                           (:id ?analysis)))}]]]))


(defn topmenu
  []
  [:div.topmenu
   [:a {:href "#/"}
    [:img {:src "img/dev/DAVElogo.png"}]]

    [:p "Data Analytics and Visualization Environment"
    [:br]"for xAPI and the Total Learning Architecture"]

   [:div.iconflex
   [:a.headerlink {:href "https://github.com/yetanalytics/dave"}
    [:img#topmenuicon {:src "img/dev/Githublogo.png"}] "Contribute on GitHub"]]


   ])

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
   [:a {:href "#"} [:img {:src "img/dev/white_logo.png"}]]
   [:div.spacer]
   [:a#license {:href "https://github.com/yetanalytics/dave/blob/master/LICENSE"}
    "License"]])
