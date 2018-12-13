(ns com.yetanalytics.dave.vis.bar
  "Specifications of DAVE visualiazation prototypes for bar charts."
  (:require [clojure.spec.alpha :as s]))

(def base
  "Basic vega vertical bar w/optional category grouping"
  {:axes
   [{:orient "bottom", :scale "xscale"
     :tickSize 0 :labelPadding 4 :zindex 1
     :labelAngle 60
     :labelAlign "left"}
    {:orient "left", :scale "yscale"}],
   :width 500,
   :autosize "fit"
   :scales
   [{:name "xscale",
     :type "band",
     :domain {:data "table", :field "x"},
     :range "width",
     :padding 0.05,
     :round true}
    {:name "yscale",
     :domain {:data "table", :field "y"},
     :nice true,
     :range "height"}
    {:name "color",
     :type "ordinal",
     :range "category",
     :domain {:data "table", :field "c"}}],
   :padding 5,
   :marks
   [{:type "group"
     :from {:facet
            {:name "facet", :data "table", :groupby "x"}}
     :encode
     {:enter {:x {:scale "xscale" :field "x"}}}
     :signals
     [{:name "width" :update "bandwidth('xscale')"}]
     :scales
     [{:name "pos"
       :type "band"
       :range "width"
       :domain {:data "facet" :field "c"}}]
     :marks
     [{:name "bars",
       :type "rect"
       :from {:data "facet"},
       :encode
       {:enter
        {:x {:scale "pos", :field "c"},
         :width {:scale "pos", :band 1},
         :y {:scale "yscale", :field "y"},
         :y2 {:scale "yscale", :value 0}
         :fill {:scale "color" :field "c"}},
        ;; :update {:fill {:signal "bar_color"}},
        ;; :hover {:fill {:value "red"}}
        }}
      {:type "text"
       :from {:data "bars"}
       :encode
       {:enter {:x {:field "x" :offset {:field "width" :mult 0.5}}
                :y {:field "y2" :offset -5}
                :fill {:value "white"}
                :align {:value "right"}
                :baseline {:value "middle"}
                :text {:field "datum.y"}}}}]}],
   :$schema "https://vega.github.io/schema/vega/v4.json",
   #_:signals
   #_[{:name "tooltip",
     :value {},
     :on
     [{:events "rect:mouseover", :update "datum"}
      {:events "rect:mouseout", :update "{}"}]}
    {:name "bar_color"
     :value "steelblue"}],
   :height 200,
   :data
   [{:name "table",
     :values
     [{:x "A", :y 28 :c 1} {:x "A", :y 40 :c 2}
      {:x "B", :y 55 :c 1} {:x "B", :y 24 :c 2}
      {:x "C", :y 43 :c 1} {:x "C", :y 96 :c 2}
      ]}]})
