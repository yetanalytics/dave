(ns com.yetanalytics.dave.vis
  "Specifications of DAVE visualiazation prototypes"
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.vis.line :as line]
            [com.yetanalytics.dave.vis.bar :as bar]
            [com.yetanalytics.dave.vis.pie :as pie]
            ;; call in the fn return specs
            [com.yetanalytics.dave.func.ret :as func-ret]))

(def registry
  ;; map of vis ids (keywords) to vega specs and other vis information
  {::line/base     {:vega-spec line/base
                    :datum-spec (s/keys :req-un
                                        [:datum/x
                                         :datum/y]
                                        :opt-un
                                        [:datum/c])}
   ::line/timeline {:vega-spec line/timeline
                    :datum-spec (s/keys :req-un
                                        [:datum/x
                                         :datum/y]
                                        :opt-un
                                        [:datum/c])}
   ::bar/base      {:vega-spec bar/base
                    :datum-spec (s/keys :req-un
                                        [:datum/x
                                         :datum/y]
                                        :opt-un
                                        [:datum/c])}
   ::pie/base      {:vega-spec pie/base
                    :datum-spec (s/and
                                 (s/keys :req-un
                                         [:datum/x
                                          :datum/y])
                                 (fn no-category
                                   [datum]
                                   (not (contains? datum :c))))}
   })

(s/def ::id
  (s/and qualified-keyword?
         #(contains? (set (keys registry)) %)))
