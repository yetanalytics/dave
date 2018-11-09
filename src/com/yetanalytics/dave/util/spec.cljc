(ns com.yetanalytics.dave.util.spec
  (:require [clojure.spec.alpha :as s]))

(def string-not-empty-spec
  (s/and string?
         not-empty))
