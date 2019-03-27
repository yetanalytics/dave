(ns com.yetanalytics.dave.vis.line
  "Specifications of DAVE visualiazation prototypes for line/area charts."
  (:require [clojure.spec.alpha :as s]))

(def base
  "Basic Vega Line chart with grouping."
  {:axes [{:orient "bottom", :scale "x"
           :labelAngle 60
           :labelAlign "left"
           :labelLimit 112
           :labelOverlap true
           :labelSeparation -35}
          {:orient "left", :scale "y"}],
   :width 500,
   :height 200,
   :autosize "fit"
   :scales
   [{:name "x",
     :type "point",
     :range "width",
     :domain {:data "table", :field "x"}}
    {:name "y",
     :type "linear",
     :range "height",
     :nice true,
     :zero true,
     :domain {:data "table", :field "y"}}
    {:name "color",
     :type "ordinal",
     :range "category",
     :domain {:data "table", :field "c"}}],
   :padding 5,
   :marks
   [{:type "group",
     :from {:facet {:name "series", :data "table", :groupby "c"}},
     :marks
     [{:type "line",
       :from {:data "series"},
       :encode
       {:enter
        {:x {:scale "x", :field "x"},
         :y {:scale "y", :field "y"},
         :stroke {:scale "color", :field "c"},
         :strokeWidth {:value 2}},
        :update
        {:interpolate {:signal "interpolate"}, :fillOpacity {:value 1}},
        :hover {:fillOpacity {:value 0.5}}}}]}],
   :$schema "https://vega.github.io/schema/vega/v4.json",
   :signals
   [{:name "interpolate",
     :value "linear",
     #_:bind
     #_{:input "select",
      :options
      ["basis"
       "cardinal"
       "catmull-rom"
       "linear"
       "monotone"
       "natural"
       "step"
       "step-after"
       "step-before"]}}],
   :data
   [{:name "table",
     :values
     [{:x 0, :y 28, :c 0}
      {:x 0, :y 20, :c 1}
      {:x 1, :y 43, :c 0}
      {:x 1, :y 35, :c 1}
      {:x 2, :y 81, :c 0}
      {:x 2, :y 10, :c 1}
      {:x 3, :y 19, :c 0}
      {:x 3, :y 15, :c 1}
      {:x 4, :y 52, :c 0}
      {:x 4, :y 48, :c 1}
      {:x 5, :y 24, :c 0}
      {:x 5, :y 28, :c 1}
      {:x 6, :y 87, :c 0}
      {:x 6, :y 66, :c 1}
      {:x 7, :y 17, :c 0}
      {:x 7, :y 27, :c 1}
      {:x 8, :y 68, :c 0}
      {:x 8, :y 16, :c 1}
      {:x 9, :y 49, :c 0}
      {:x 9, :y 25, :c 1}]}]})

(def timeline
  (-> base
      (assoc-in [:scales 0 :type] "time")
      ;; some example data
      (assoc-in [:data 0 :values]
                [{:x 1544635781376, :y 1, :c 0} {:x 1544635781376, :y 3, :c 1}
                 {:x 1544635798007, :y 2, :c 0} {:x 1544635798007, :y 1, :c 1}
                 {:x 1544635807957, :y 1, :c 0} {:x 1544635807957, :y 3, :c 1}])))
