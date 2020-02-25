(ns com.yetanalytics.dave.workbook
  "Workbooks reference an xAPI dataset and contain questions that use the data
  as a basis. They also contain contextual + display information."
  (:require [clojure.spec.alpha                      :as s]
            [com.yetanalytics.dave.workbook.analysis :as analysis]
            [com.yetanalytics.dave.util.spec         :as su]
            [com.yetanalytics.dave.workbook.data     :as data]))

(s/def ::id
  uuid?)

(s/def ::title
  su/string-not-empty-spec)

(s/def ::description
  su/string-not-empty-spec)

(s/def ::analyses
  (s/and (s/map-of ::analysis/id
                   analysis/analysis-spec)
         (comp su/sequential-indices? vals)))

(s/def ::data
  data/data-spec)

(def workbook-spec
  (s/keys :req-un [::id
                   ::title
                   ::description]

          :opt-un [::data
                   ::analyses]))
