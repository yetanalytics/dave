(ns com.yetanalytics.dave.ui.util.vega
  "Some utils for vega that shouldn't be tied to views."
  (:require [cljsjs.vega]
            [cljsjs.vega-tooltip]))

(defn parse-error
  "Returns any vega parse errors that might crash the vis"
  [spec]
  (try (.parse js/vega (clj->js spec))
       nil
       (catch js/Error e
         (ex-message e))))
