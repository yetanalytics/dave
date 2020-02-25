(ns com.yetanalytics.dave.workbook.analysis
  (:require [clojure.spec.alpha                                    :as s]
            [com.yetanalytics.dave.util.spec                       :as su]
            [com.yetanalytics.dave.datalog                         :as d]
            [datascript.query                                      :as dq]
            [clojure.edn                                           :as edn]
            [clojure.walk                                          :as w]
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
  ::d/query)

(s/fdef parse-query
  :args (s/cat :query-str ::query)
  :ret ::query-data)

(defn parse-query
  [query-str]
  (try (let [q (d/normalize-query (edn/read-string query-str))]
         ;; just try a full parse for any extra errors
         (dq/memoized-parse-query q)
         ;; but return the query
         q)
       (catch #?(:clj Exception
                 :cljs js/Error) e
         (throw (ex-info (str "Query Parse Error - " (ex-message e))
                         {:type ::query-parse-error
                          :query-str query-str}
                         e)))))

(s/def ::query-parse-error
  ;; TODO: specify and make data
  string?)


(s/def ::vega string?)

(s/def ::visualization
  map?)

(s/fdef parse-vega
  :args (s/cat :vega ::vega)
  :ret ::visualization)

(defn parse-vega
  [vega]
  (try #?(:clj (json/read-str vega :key-fn keyword)
          :cljs (js->clj (.parse js/JSON vega)
                         :keywordize-keys true))
       (catch #?(:clj Exception
                 :cljs js/Error) e
         (throw (ex-info (str "Vega Parse Error - " (ex-message e))
                         {:type ::vega-parse-error
                          :vega-json vega}
                         e)))))

(s/fdef vis-fields
  :args (s/cat :vis ::visualization)
  :ret (s/every string?))

(defn vis-fields
  "List the referred fields in a visualization"
  [vis]
  (into []
        (distinct
         (w/postwalk
          (fn [x]
            (cond (map? x)
                  (reduce-kv
                   (fn [acc k v]
                     (cond (and (= :field k)
                                (= "result" (:data x)))
                           (conj acc v)
                           (vector? v)
                           (into acc (flatten v))
                           :else acc))
                   []
                   x)
                  :else x))
          vis))))

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
                   ::vega-parse-error
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
    (try (dissoc (assoc extant
                        :query query
                        :query-data (parse-query query))
                 :query-parse-error)
         (catch #?(:clj Exception
                   :cljs js/Error) e
           (merge (dissoc extant :query-data)
                  {:query query
                   :query-parse-error
                   (ex-message e)})))))

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
    (try (dissoc (assoc extant
                        :vega vega
                        :visualization (parse-vega vega))
                 :vega-parse-error)
         (catch #?(:clj Exception
                   :cljs js/Error) e
           (merge (dissoc extant :visualization)
                  {:vega vega
                   :vega-parse-error
                   (ex-message e)})))))

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
  [vis result query]
  (update vis
          :data
          #(into %2 %1)
          [((d/result-vega-mapper query) result)]))

(defn ensure-analysis
  "Just a helper to make sure everything in an analysis is as parsed as can be"
  [{:keys [query query-data query-parse-error
           vega visualization vega-parse-error]
    :as analysis}]
  (cond-> analysis
    ;; If there is a query but no data or error, make it happen
    (and query
         (not (or query-data query-parse-error)))
    (-> (dissoc :query)
        (update-query {:query query}))
    ;; same for vega
    (and vega
         (not (or visualization vega-parse-error)))
    (-> (dissoc :vega)
        (update-vega {:vega vega}))))
