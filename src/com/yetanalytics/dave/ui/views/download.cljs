(ns com.yetanalytics.dave.ui.views.download
  (:require [goog.string :refer [format]]
            [goog.string.format]))

(defn download-text
  [label text]
  [:a {:href (format
              "data:application/octet-stream;charset=utf-16;base64,%s"
              (js/btoa text))}
   label])
