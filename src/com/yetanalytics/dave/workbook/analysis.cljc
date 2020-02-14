(ns com.yetanalytics.dave.workbook.analysis
  (:require [clojure.spec.alpha                                    :as s]
            [com.yetanalytics.dave.util.spec                       :as su]
            [com.yetanalytics.dave.datalog                         :as d]
            [clojure.edn                                           :as edn]
            [clojure.pprint :refer [pprint] ]
            #?(:clj [clojure.data.json :as json])))

(s/def ::id
  uuid?)

(s/def ::text
  su/string-not-empty-spec)

(s/def ::index
  su/index-spec)

(s/def ::query string?)

(s/def ::query-data
  (s/and (s/conformer (fn [x]
                        (if (string? x)
                          (try (edn/read-string x)
                               (catch #?(:clj Exception
                                         :cljs js/Error) e
                                 ::s/invalid))
                          x))
                      (fn [x]
                        (if (string? x)
                          x
                          (with-out-str (pprint x)))))
         ::d/query))

(s/def ::query-parse-error
  ;; TODO: specify and make data
  string?)


(s/def ::vega string?)

(s/def ::visualization
  (s/and
   (s/conformer (fn [x]
                  (if (string? x)
                    (try #?(:clj (json/read-str x)
                            :cljs (js->clj (.parse js/JSON x)
                                           :keywordize-keys true)
                            )
                         (catch #?(:clj Exception
                                   :cljs js/Error) e
                           ::s/invalid))
                    x))
                (fn [x]
                  (if (string? x)
                    x
                    #?(:clj (json/write-str x)
                       :cljs (.stringify js/JSON
                                         (clj->js x))))))
   map?))

(s/def ::vega-parse-error
  string?)


(s/def ::result
  any?)

(def analysis-spec
  (s/keys :req-un [::id
                   ::text
                   ::index]
          :opt-un [::query
                   ::query-data
                   ::query-parse-error
                   ::vega
                   ::visualization
                   ::result]))

(def form-spec
  (s/keys :opt-un [::query ::vega]))

(s/fdef update-query
  :args (s/cat :extant analysis-spec
               :form form-spec)
  :ret analysis-spec)

(defn update-query
  [{extant-query :query
    :as extant}
   {query :query
    :as form}]
  (if (= extant-query
         query)
    extant
    (let [query-data (s/conform ::query-data query)]
      (if (= ::s/invalid query-data)
        (merge (dissoc extant :query-data)
               {:query query
                :query-parse-error
                (s/explain-str ::query-data
                               query)})
        (dissoc (assoc extant
                       :query query
                       :query-data query-data)
                :query-parse-error)))))

(s/fdef update-vega
  :args (s/cat :extant analysis-spec
               :form form-spec)
  :ret analysis-spec)

(defn update-vega
  [{extant-vega :vega
    :as extant}
   {vega :vega
    :as form}]
  (if (= extant-vega
         vega)
    extant
    (let [vega-data (s/conform ::visualization vega)]
      (if (= ::s/invalid vega-data)
        (merge (dissoc extant :visualization)
               {:vega vega
                :vega-parse-error
                (s/explain-str ::visualization vega)})
        (dissoc (assoc extant
                       :vega vega
                       :visualization vega-data)
                :vega-parse-error)))))

(s/fdef update-analysis
  :args (s/cat :extant analysis-spec
               :form form-spec)
  :ret analysis-spec)

(defn update-analysis
  [{:as extant}
   {query :query
    vega :vega
    :as form}]
  (cond-> (merge extant (dissoc form :query :vega))
    query (update-query form)
    vega (update-vega form)))

(s/fdef result-vega-spec
  :args (s/cat :analysis analysis-spec)
  :ret (s/nilable ::visualization))

(defn result-vega-spec
  "Given an analysis, return a combined vega vis"
  [{vis :visualization
    result :result}]
  (when (and vis result)
    (update vis :data (fnil conj [])
            {:name "table"
             :values (into [] result)})))
