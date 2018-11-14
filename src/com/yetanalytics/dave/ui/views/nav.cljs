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
