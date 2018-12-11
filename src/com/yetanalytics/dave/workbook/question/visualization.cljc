(ns com.yetanalytics.dave.workbook.question.visualization
  "Visualizations contain a reference to a vis spec and any additional user
  controls for that vis."
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.util.spec :as su]
            [dave.tag :as tag]))

(s/def ::id
  uuid?)

(s/def ::index
  su/index-spec)

(def visualization-spec
  (s/keys :req-un [::id
                   ::index
                   ::tag/tags]))
