(ns com.yetanalytics.dave.workbook.question
  "Questions contain a specific question about a dataset, a reference to a
  function to get the answer, any user-configurable constants the function
  requires, and any number of visualizations to represent the result."
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.util.spec :as su]
            [com.yetanalytics.dave.workbook.question.visualization :as v]))

(s/def ::id
  uuid?)

(s/def ::text
  su/string-not-empty-spec)

(s/def ::visualizations
  (s/map-of ::v/id
            v/visualization-spec))

(def question-spec
  (s/keys :req-un [::id
                   ::text
                   ::visualizations]))
