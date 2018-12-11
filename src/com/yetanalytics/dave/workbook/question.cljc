(ns com.yetanalytics.dave.workbook.question
  "Questions contain a specific question about a dataset, a reference to a
  function to get the answer, any user-configurable constants the function
  requires, and any number of visualizations to represent the result."
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.util.spec :as su]
            [com.yetanalytics.dave.workbook.question.visualization :as v]
            [com.yetanalytics.dave.func :as func]
            [dave.tag :as tag]))

(s/def ::id
  uuid?)

(s/def ::text
  su/string-not-empty-spec)

;; The dave function that this question uses to get its data
(s/def ::function
  (s/and (s/keys :req-un [::func/id]
                 :opt-un [::func/args])
         ;; Validate that the args are OK
         (fn valid-args [{:keys [id args]}]
           (nil? (func/explain-args id args)))))

(s/def ::visualizations
  (s/and (s/map-of ::v/id
                   v/visualization-spec)
         (comp su/sequential-indices? vals)))

(s/def ::index
  su/index-spec)

(def question-spec
  (s/keys :req-un [::id
                   ::text
                   ::visualizations
                   ::index
                   ::tag/tags]
          :opt-un [::function]))
