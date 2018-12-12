(ns com.yetanalytics.dave.workbook.question.visualization
  "Visualizations contain a reference to a vis spec and any additional user
  controls for that vis."
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.util.spec :as su]
            [com.yetanalytics.dave.vis :as vis]))

(s/def ::id
  uuid?)

(s/def ::index
  su/index-spec)

(s/def :vis/id
  #(contains? (keys vis/registry) %))

(s/def :vis/args map?)

(s/def ::vis
  (s/keys :req-un [::vis/id
                   :vis/args]))

(def visualization-spec
  (s/keys :req-un [::id
                   ::index
                   ]
          :opt-un [::vis]))
