(ns com.yetanalytics.dave.vis
  "Specifications of DAVE visualiazation prototypes"
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.vis.line :as line]
            [com.yetanalytics.dave.vis.scatter :as scatter]
            [com.yetanalytics.dave.vis.bar :as bar]
            [com.yetanalytics.dave.vis.pie :as pie]
            ;; call in the fn return specs
            [com.yetanalytics.dave.func.ret :as func-ret]))

(def registry
  ;; map of vis ids (keywords) to vega specs and other vis information
  {::line/base     {:title "Line"
                    :vega-spec line/base
                    :datum-spec (s/keys :req-un
                                        [:datum/x
                                         :datum/y]
                                        :opt-un
                                        [:datum/c])}
   ::line/timeline {:title "Timeline"
                    :vega-spec line/timeline
                    :datum-spec (s/keys :req-un
                                        [:datum/x
                                         :datum/y]
                                        :opt-un
                                        [:datum/c])}
   ::bar/base      {:title "Bar"
                    :vega-spec bar/base
                    :datum-spec (s/keys :req-un
                                        [:datum/x
                                         :datum/y]
                                        :opt-un
                                        [:datum/c])}
   ::pie/base      {:title "Pie"
                    :vega-spec pie/base
                    :datum-spec (s/and
                                 (s/keys :req-un
                                         [:datum/x
                                          :datum/y])
                                 (fn no-category
                                   [datum]
                                   (not (contains? datum :c))))}
   ::scatter/base {:title "Scatter"
                   :vega-spec scatter/base
                   :datum-spec (s/keys :req-un
                                       [:datum/x
                                        :datum/y]
                                       :opt-un
                                       [:datum/c])}
   ::scatter/time-scatter {:title "Time Scatter"
                           :vega-spec scatter/time-scatter
                           :datum-spec (s/keys :req-un
                                               [:datum/x
                                                :datum/y]
                                               :opt-un
                                               [:datum/c])}
   ::scatter/time-scatter-point {:title "Time Scatter (Point Y-Axis)"
                                 :vega-spec scatter/time-scatter-point
                                 :datum-spec (s/keys :req-un
                                                     [:datum/x
                                                      :datum/y
                                                      :datum/c])}})

(s/def ::id
  (s/and qualified-keyword?
         #(contains? (set (keys registry)) %)))
