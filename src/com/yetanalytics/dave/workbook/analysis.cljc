(ns com.yetanalytics.dave.workbook.analysis
  (:require [clojure.spec.alpha                                    :as s]
            [com.yetanalytics.dave.util.spec                       :as su]
            [com.yetanalytics.dave.workbook.question.visualization :as v]
            [com.yetanalytics.dave.datalog                         :as d]
            [clojure.edn                                           :as edn]
            [clojure.pprint :refer [pprint] ]))

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
  (s/or :viz v/visualization-spec
        :str string?))

(def analysis-spec
  (s/keys :req-un [::id
                   ::text
                   ::index]
          :opt-un [::query
                   ::query-data
                   ::query-parse-error
                   ::vega
                   ::visualization]))
