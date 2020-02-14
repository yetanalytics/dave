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
  ::d/query)

(s/fdef parse-query
  :args (s/cat :query-str ::query)
  :ret ::query-data)

(defn parse-query
  [query-str]
  (try (d/normalize-query (edn/read-string query-str))
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
  [{vis :visualization
    result :result}]
  (update vis :data (fnil conj [])
          {:name "table"
           :values (into [] result)}))
