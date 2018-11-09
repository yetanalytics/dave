(ns com.yetanalytics.dave.util.spec
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as sgen]))

(def string-not-empty-spec
  (s/and string?
         not-empty))
