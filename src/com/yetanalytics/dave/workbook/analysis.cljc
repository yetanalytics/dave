(ns com.yetanalytics.dave.workbook.analysis
  (:require [clojure.spec.alpha                                    :as s]
            [com.yetanalytics.dave.util.spec                       :as su]
            [com.yetanalytics.dave.workbook.question.visualization :as v]))

(s/def ::id
  uuid?)

(s/def ::title
  su/string-not-empty-spec)

(s/def ::index
  su/index-spec)

(s/def ::description
  su/string-not-empty-spec)

(s/def ::query
  su/string-not-empty-spec)

(s/def ::vega
  su/string-not-empty-spec)

(s/def ::visualization
  (s/or :viz v/visualization-spec
        :str string?))

(def analysis-spec
  (s/keys :req-un [::id
                   ::title
                   ::description
                   ::index
                   ::query
                   ::vega]
          :opt-un [::visualization]))
