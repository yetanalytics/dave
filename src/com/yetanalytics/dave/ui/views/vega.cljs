(ns com.yetanalytics.dave.ui.views.vega
  "Render Vega charts from specifications + options.
  Main component is com.yetanalytics.dave.ui.views.vega/vega.

  In a reagent component, it can be used like:
  [vega <vega spec + data>
   :signals-in {\"bar_color\" [:debug/bar-color]} ;; signals in from subs
   ;; signals out to handlers
   :signals-out {\"tooltip\" [:debug/log \"tooltip state:\"]
                 \"bar_color\" [:debug/log \"bar color out:\"]}
   ;; dom + vega events out to handlers
   :events-out {\"click\" [:debug/log \"click event:\"]}
   ;; Other options:
   :renderer \"canvas\" ;; use canvas rather than SVG
   :hover? false ;; don't initialize hovering
   :log-level :debug ;; set log level (default is :warn)
  ]"
  (:require
   [cljsjs.vega]
   [cljsjs.vega-tooltip]
   [reagent.core :as r]
   [reagent.ratom :as ratom]
   [re-frame.core :refer [dispatch subscribe]]
   [clojure.spec.alpha :as s]
   [com.yetanalytics.dave.ui.app.io :as io]))

(s/def ::chart
  #(instance? js/vega.View %))

(def re-frame-vec-spec
  (s/and vector?
         (s/cat :id qualified-keyword?
                :restv (s/* identity))))

(s/def ::qvec
  re-frame-vec-spec)

(s/def ::hvec
  re-frame-vec-spec)

(s/def ::signal-name string?)
(s/def ::event-type string?)


(s/def ::signals-in
  (s/nilable
   (s/map-of ::signal-name
             ::qvec)))

(s/def ::signal-trackers
  (s/every
   #(instance? reagent.ratom.Reaction %)
   :kind vector?
   :into []))


(s/fdef signal-trackers-init
  :args (s/cat :chart ::chart
               :signals-in ::signals-in)
  :ret ::signal-trackers)

(defn signal-trackers-init
  "Given a Vega View (chart) and a map of signal names to re-frame subscription
  query vectors, subscribe to each sub and create a tracker that sets the
  signal value from that of the sub. Returns a vector of trackers."
  [chart signals-in]
  (into []
        (for [[signal-name qvec] signals-in
              :let [sub (subscribe qvec)]]

          (r/track! (fn []
                      (let [v @sub]
                        (.signal chart
                                 signal-name
                                 v)
                        (.run chart)))))))

(s/fdef signal-trackers-cleanup
  :args (s/cat :signal-trackers ::signal-trackers)
  :ret nil?)

(defn signal-trackers-cleanup
  "Given a vector of trackers, dispose of them all."
  [signal-trackers]
  (doseq [tracker signal-trackers]
    (r/dispose! tracker)))

(s/def ::signals-out
  (s/nilable
   (s/map-of ::signal-name
             ::hvec)))

(s/fdef signal-listeners-init!
  :args (s/cat :chart ::chart
               :signals-out ::signals-out)
  :ret nil?)

(defn signal-listeners-init!
  "Given a Vega View (chart) and a map of signal names to partial re-frame
  handler vectors, add a listener for each signal that calls the handler with
  the signal name and value appended."
  [chart signals-out]
  (doseq [[signal-name hvec] signals-out]
    (.addSignalListener chart
                        signal-name
                        (fn [n v]
                          (dispatch (conj hvec n v))))))

(s/def ::events-out
  (s/nilable
   (s/map-of ::event-type
             ::hvec)))

(s/fdef event-listeners-init!
  :args (s/cat :chart ::chart
               :events-out
               ::events-out)
  :ret nil?)

(defn event-listeners-init!
  "Given a Vega View (chart) and a map of event types to partial re-frame
  handler vectors, add a listener for each event that calls the handler with
  the event type, event, and Vega scenegraph item (if applicable)."
  [chart events-out]
  (doseq [[event-type hvec] events-out]
    (.addEventListener
     chart
     event-type
     (fn [event scenegraph-item]
       (dispatch (conj hvec
                       event-type
                       event
                       scenegraph-item))))))

(s/fdef cleanup!
  :args (s/cat :component #(instance? js/Object %))
  :ret nil?)

(defn cleanup!
  "Clean up a component's chart and associated listeners."
  [component]
  (let [{:keys [chart
                signal-trackers]}
        (r/state component)]
    (signal-trackers-cleanup signal-trackers)
    (.finalize chart)
    nil))

(s/def :vega.dataset/name
  (s/and string?
         not-empty))

(s/def :vega.dataset/values
  (s/or :maps (s/every map?
                       :kind vector?
                       :into [])
        :bare-numbers
        (s/every number?
                 :kind vector?
                 :into [])))

(s/def :vega/dataset
  (s/keys :req-un [:vega.dataset/name
                   :vega.dataset/values]))
(s/def ::data
  (s/every
   :vega/dataset
   :kind vector?
   :into []))

(s/def ::dset-map
  (s/map-of :vega.dataset/name
            :vega/dataset))

(s/fdef dset-map
  :args (s/cat :data
               ::data)
  :ret ::dset-map)

(defn- dset-map
  "Coerce a vector of Vega datasets into a map by dataset name."
  [data]
  (into {}
        (map (fn [{dataset-name :name
                   :as dataset}]
               [dataset-name
                dataset]))
        data))

(s/fdef did-mount
  :args (s/cat :this #(instance? js/Object %)))

(s/def ::log-level
  (s/and
   (s/conformer
    (fn [x]
      (if (keyword? x)
        (get {:error js/vega.Error
              :warn js/vega.Warn
              :info js/vega.Info
              :debug js/vega.Debug}
             x
             ::s/invalid)
        x))
    (fn [x]
      (get {js/vega.Error :error
            js/vega.Warn :warn
            js/vega.Info :info
            js/vega.Debug :debug} x)))
   #{js/vega.Error
     js/vega.Warn
     js/vega.Info
     js/vega.Debug}))

(defn export-fn
  [chart export-type]
  (let [extension (condp = export-type
                    :png "png"
                    :svg "svg"
                    "png")]
    (.then (.toImageURL chart
                        extension)
           (fn [url]
             (let [link (js/document.createElement "a")
                   _    (.setAttribute link "href" url)
                   _    (.setAttribute link "target" "_blank")
                   _    (.setAttribute link "download" (str "dave-export."
                                                            extension))]
               (.click link))))))

(defn did-mount
  "React lifecycle handler: set up a vega chart and any requested signal/event
  ports."
  [this]
  (let [[_ spec & {:keys [signals-in
                          signals-out
                          events-out
                          renderer
                          hover?
                          log-level]
                   :or {renderer "svg"
                        hover? true
                        log-level :warn}}] (r/argv this)
        el        (aget (.-childNodes (r/dom-node this)) 1)
        el-width  (.-offsetWidth el)
        el-height (.-offsetHeight el)
        {spec-width :width
         spec-height :height} spec
        [width
         height] [spec-width spec-height] #_(if (= spec-width spec-height)
                        [spec-width
                         spec-height]
                        [el-width
                         el-height])
        runtime (.parse js/vega (clj->js spec))
        tooltip-handler (js/vegaTooltip.Handler.)
        chart (-> (js/vega.View. runtime)
                  (.logLevel (s/conform ::log-level
                                        log-level))

                  (.width width)
                  (.height height)
                  (.tooltip (.-call tooltip-handler))
                  (.renderer renderer)
                  (.initialize el)
                  (cond-> hover? .hover))
        header-el   (aget (.-childNodes (r/dom-node this)) 0)
        png-button  (js/document.getElementById "export-viz-png")
        _           (set! (.-onclick png-button) (fn [e]
                                                   (.preventDefault e)
                                                   (.stopPropagation e)
                                                   (export-fn chart :png)))
        svg-button  (js/document.getElementById "export-viz-svg")
        _           (set! (.-onclick svg-button) (fn [e]
                                                   (.preventDefault e)
                                                   (.stopPropagation e)
                                                   (export-fn chart :svg)))]
    (signal-listeners-init! chart signals-out)
    (event-listeners-init! chart events-out)
    (r/set-state this
                 {:signal-trackers
                  (signal-trackers-init
                   chart
                   signals-in)
                  :chart (.run chart)})))

(s/fdef did-update
  :args (s/cat :this #(instance? js/Object %)))

(defn did-update
  "React lifecycle handler: given new data (and possibly other attributes),
  attempt to update the chart. If non-data changes are made, will clean up and
  call the mount handler."
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
        (cleanup! this)
        (did-mount this)))))

;; vega spec
;; TODO: maybe a spec that reaches out to json schema?
(s/def ::spec
  map?)

(s/def ::renderer
  #{"svg" "canvas"})

(s/def ::hover?
  boolean?)

(defn vega-render
  "Dummy render fn for vega charts"
  [spec & {:keys [signals-in
                  signals-out
                  events-out
                  renderer
                  hover?
                  log-level
                  workbook-id
                  analysis-id] :as options}]
  [:div
   [:div.flex-container
    [:h4.header-title "Data Visualization"]
    [:div.spacer]
    [:button.minorbutton.header-button
     {:on-click (fn [e]
                  (.preventDefault e)
                  (.stopPropagation e)
                  (dispatch [:workbook.analysis/run
                             workbook-id
                             analysis-id]))}
     "Run"]
    [:button#export-viz-png.minorbutton
     {}
     "Export PNG"]
    [:button#export-viz-svg.minorbutton
     {}
     "Export SVG"]
    [:button.minorbutton
     {:on-click (fn [e]
                  (io/export-file e
                                  (js/Blob. [@(subscribe [:workbook.analysis/result-vega-spec])]
                                            (clj->js {:type "application/json"}))
                                  "result.json"))}
     "Export JSON"]]
   [:div.dave-vega-container]])

(def vega
  (r/create-class
   {:reagent-render
    vega-render
    :component-did-mount
    did-mount
    :component-did-update
    did-update
    :component-will-unmount
    cleanup!}))



;; TODO: remove, demo stuff only
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
      :update {:fill {:signal "bar_color"}},
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
      {:events "rect:mouseout", :update "{}"}]}
    {:name "bar_color"
     :value "steelblue"}],
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
