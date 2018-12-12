(ns com.yetanalytics.dave.workbook.question.visualization
  "Visualizations contain a reference to a vis spec and any additional user
  controls for that vis."
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.util.spec :as su]
            [com.yetanalytics.dave.vis :as vis]
            [com.yetanalytics.dave.func.ret :as ret]))

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

(s/fdef prepare-vega-spec
  :args (s/cat :result ::ret/result
               :vis-id ::vis/id
               :vis-args :vis/args)
  :ret map? ;; vega spec map
  )


;; TODO: do stuff w/specification
;; very naive
(defn prepare-vega-spec
  [{:keys [specification
           values] :as result}
   vis-id
   vis-args]
  (if-let [{:keys [vega-spec
                   datum-spec] :as vis} (get vis/registry vis-id)]
    (assoc-in vega-spec
              [:data 0 :values] values)
    (throw (ex-info "Unknown visualization prototype!"
                    {:type ::unknown-vis
                     :vis-id vis-id}))))
