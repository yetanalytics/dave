(ns com.yetanalytics.dave.ui.views.vega
  (:require [reagent.core :as r]))

;; TODO: remove
(def bar-spec-demo
  {:axes
   [{:orient "bottom", :scale "xscale"}
    {:orient "left", :scale "yscale"}],
   :width 400,
   :scales
   [{:name "xscale",
     :type "band",
     :domain {:data "table", :field "category"},
     :range "width",
     :padding 0.05,
     :round true}
    {:name "yscale",
     :domain {:data "table", :field "amount"},
     :nice true,
     :range "height"}],
   :padding 5,
   :marks
   [{:type "rect",
     :from {:data "table"},
     :encode
     {:enter
      {:x {:scale "xscale", :field "category"},
       :width {:scale "xscale", :band 1},
       :y {:scale "yscale", :field "amount"},
       :y2 {:scale "yscale", :value 0}},
      :update {:fill {:value "steelblue"}},
      :hover {:fill {:value "red"}}}}
    {:type "text",
     :encode
     {:enter
      {:align {:value "center"},
       :baseline {:value "bottom"},
       :fill {:value "#333"}},
      :update
      {:x {:scale "xscale", :signal "tooltip.category", :band 0.5},
       :y {:scale "yscale", :signal "tooltip.amount", :offset -2},
       :text {:signal "tooltip.amount"},
       :fillOpacity
       [{:test "datum === tooltip", :value 0} {:value 1}]}}}],
   :$schema "https://vega.github.io/schema/vega/v4.json",
   :signals
   [{:name "tooltip",
     :value {},
     :on
     [{:events "rect:mouseover", :update "datum"}
      {:events "rect:mouseout", :update "{}"}]}],
   :height 200,
   :data
   [{:name "table",
     :values
     [{:category "A", :amount 28}
      {:category "B", :amount 55}
      {:category "C", :amount 43}
      {:category "D", :amount 91}
      {:category "E", :amount 81}
      {:category "F", :amount 53}
      {:category "G", :amount 19}
      {:category "H", :amount 87}]}]})

(defn- dset-map [data]
  (into {}
        (map (fn [{dataset-name :name
                   :as dataset}]
               [dataset-name
                dataset]))
        data))

(defn- did-mount
  [this]
  (let [spec (-> this r/argv second)
        el (r/dom-node this)
        runtime (.parse js/vega (clj->js spec))]
    (r/set-state this
                 {:chart (-> (js/vega.View. runtime)
                             (.logLevel js/vega.Warn)
                             (.renderer "svg")
                             (.initialize el)
                             .hover
                             .run)})))

(defn- did-update
  [this old-argv]
  (let [{:keys [chart]} (r/state this)
        new-spec (-> this r/argv second)
        old-spec (second old-argv)]
    (if (= (dissoc new-spec :data) ;; if the only thing that changed is data
           (dissoc old-spec :data))
      (let [new-data-map (dset-map (:data new-spec))
            old-data-map (dset-map (:data old-spec))]
        ;; Detect series changes, dumb updates
        (doseq [[dataset-name
                 new-dataset] new-data-map
                :when (not= new-dataset (get old-data-map dataset-name))
                :let [changeset (-> (.changeset js/vega)
                                    (.remove (constantly true))
                                    (.insert (clj->js
                                              (:values new-dataset))))]]
          (.change chart
                   dataset-name
                   changeset))
        (.run chart))
      ;; Otherwise, vega doesn't provide a good solution. Sooo, we completely
      ;; re-render the chart. Takes 2-3 ms for a simple bar chart.
      (do
        ;; Clean up Listeners, etc.
        (.finalize chart)
        (did-mount this)))))

(def vega
  (r/create-class
   {:reagent-render
    (fn [spec]
      [:div])
    :component-did-mount
    did-mount
    :component-did-update
    did-update}))
