(ns com.yetanalytics.dave.workbook
  "Workbooks reference an xAPI dataset and contain questions that use the data
  as a basis. They also contain contextual + display information."
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.workbook.question :as question]
            [com.yetanalytics.dave.util.spec :as su]
            [com.yetanalytics.dave.workbook.data :as data]))

(s/def ::id
  uuid?)

(s/def ::title
  su/string-not-empty-spec)

(s/def ::description
  su/string-not-empty-spec)

(s/def ::questions
  (s/and (s/map-of
          ::question/id
          question/question-spec)
         (comp su/sequential-indices? vals)))

(s/def ::data
  data/data-spec)

(def workbook-spec
  (s/keys :req-un [::id
                   ::title
                   ::description
                   ::questions
                   ]
          :opt-un [::data]))
