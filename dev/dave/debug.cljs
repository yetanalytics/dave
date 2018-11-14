(ns ^:figwheel-load dave.debug
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   [re-frame.core :as re-frame :refer [dispatch subscribe]]
   [goog.string :refer [format]]
   [goog.string.format]))

(defonce instrument!
  (do (.log js/console "Instrumenting dave..."
            (stest/instrumentable-syms))
      (stest/instrument)))

(s/def ::expand? boolean?)

(def debug-state-spec
  (s/keys :opt-un [::expand?]))

(re-frame/reg-event-db
 :debug/toggle!
 (fn [db _]
   (update-in db [:debug :expand?] not)))

(re-frame/reg-fx
 ::log
 (fn [args]
   (apply (.-log js/console) args)))

(re-frame/reg-event-fx
 :debug/log
 (fn [_ [_ & args]]
   {::log args}))

(re-frame/reg-sub
 ::state
 (fn [db _]
   (:debug db)))

(re-frame/reg-sub
 :debug/expand?
 (fn [_ _]
   (re-frame/subscribe [::state]))
 (fn [state _]
   (:expand? state false)))

(defn textfile-dump
  [label text]
  [:a {:href (format
              "data:application/octet-stream;charset=utf-16;base64,%s"
              (js/btoa text))}
   label])

(defn debug-bar
  []
  (let [expand? @(subscribe [:debug/expand?])]
    (into
     [:header.mdc-top-app-bar.mdc-top-app-bar--dense.dave-debug-bar
      [:div.mdc-top-app-bar__row
       [:section.mdc-top-app-bar__section.mdc-top-app-bar__section--align-start
        {:role "toolbar"}
        [:a.material-icons.mdc-top-app-bar__action-item
         {:aria-label (if expand? "Contract" "Expand")
          :alt (if expand? "Contract" "Expand")
          :on-click #(dispatch [:debug/toggle!])}
         (if expand? "expand_less" "expand_more")]
        [:span.mdc-top-app-bar__title
         "DEBUG"]
        [:a.material-icons.mdc-top-app-bar__action-item
         {:aria-label "Home"
          :alt "Home"
          :href "#/"}
         "home"]
        [:a.material-icons.mdc-top-app-bar__action-item
         {:aria-label "Reset DB"
          :alt "Reset DB"
          :on-click #(dispatch [:db/reset!])}
         "refresh"]
        ]]
      ]
     (when expand?
       [[:div.mdc-top-app-bar__row
         [:section.mdc-top-app-bar__section.mdc-top-app-bar__section--align-start
          [:span (format "path: %s"
                         @(subscribe [:nav/path]))]]]
        [:div.mdc-top-app-bar__row
         [:section.mdc-top-app-bar__section.mdc-top-app-bar__section--align-start
          [:span (format "context: %s"
                         @(subscribe [:nav/context]))]]]
        [:div.mdc-top-app-bar__row
         [:section.mdc-top-app-bar__section.mdc-top-app-bar__section--align-start
          [:span (format "focus: %s"
                         @(subscribe [:nav/focus-id])
                         )]]]
        [:div.mdc-top-app-bar__row
         [:section.mdc-top-app-bar__section.mdc-top-app-bar__section--align-start
          [textfile-dump "Dump EDN" @(subscribe [:db/edn-str])]]]
        [:div.mdc-top-app-bar__row
         [:section.mdc-top-app-bar__section.mdc-top-app-bar__section--align-start
          [textfile-dump "Dump Transit" @(subscribe [:db/transit-str])]]]]))))
