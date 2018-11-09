(ns com.yetanalytics.dave.workbook
  "Workbooks reference an xAPI dataset and contain questions that use the data
  as a basis. They also contain contextual + display information."
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.workbook.question :as question]
            [com.yetanalytics.dave.util.spec :as su]))

(s/def ::id
  su/string-not-empty-spec)

(s/def ::title
  su/string-not-empty-spec)

(s/def ::description
  su/string-not-empty-spec)

(s/def ::questions
  (s/map-of
   ::question/id
   question/question-spec))

(def workbook-spec
  (s/keys :req-un [::id
                   ::title
                   ::description
                   ::questions]))
