(ns com.yetanalytics.dave.workbook.question.visualization
  "Visualizations contain a reference to a vis spec and any additional user
  controls for that vis."
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.util.spec :as su]))

(s/def ::id
  su/string-not-empty-spec)

(def visualization-spec
  (s/keys :req-un [::id]))
