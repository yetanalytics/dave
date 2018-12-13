(ns com.yetanalytics.dave.vis.pie
  "Specifications of DAVE visualiazation prototypes for pie charts."
  (:require [clojure.spec.alpha :as s]))

(def base
  {:$schema "https://vega.github.io/schema/vega/v4.json",
   :width 400,
   :height 400,
   :autosize "pad"
   :signals
   [{:name "startAngle", :value 0}
    {:name "endAngle", :value 6.29}
    {:name "padAngle", :value 0}
    {:name "innerRadius", :value 0}
    {:name "cornerRadius", :value 0}
    {:name "sort", :value false}],
   :data
   [{:name "table",
     :values [{:x "A", :y 30} {:x "B", :y 50} {:x "C", :y 20}],
     :transform [{:type "pie", :field "y", :sort true}]}],
   :scales
   [{:name "color",
     :type "ordinal",
     :domain {:data "table", :field "x"},
     :range {:scheme "category20"}}],
   :marks
   [{:name "piearc",
     :type "arc",
     :from {:data "table"},
     :encode
     {:enter
      {:fill {:scale "color", :field "x"},
       :x {:signal "width / 2"},
       :y {:signal "height / 2"}},
      :update
      {:startAngle {:field "startAngle"},
       :endAngle {:field "endAngle"},
       :padAngle {:signal "padAngle"},
       :innerRadius {:signal "innerRadius"},
       :outerRadius {:signal "width / 2"},
       :cornerRadius {:signal "cornerRadius"}}}}
    {:type "text",
     :from {:data "piearc"},
     :encode
     {:enter
      {:x {:field "x"},
       :y {:field "y"},
       :fill {:value "white"},
       :radius {:signal "datum.outerRadius / 2"},
       :theta
       {:signal
        "datum.startAngle + ((datum.endAngle - datum.startAngle) / 2)"},
       :text {:field "datum.y"}}}}],
   :legends [{:fill "color"}]})
