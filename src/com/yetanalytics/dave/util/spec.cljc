(ns com.yetanalytics.dave.util.spec
  (:require [clojure.spec.alpha :as s]))

(def string-not-empty-spec
  (s/and string?
         not-empty))

(def index-spec
  (s/int-in 0 #?(:clj Integer/MAX_VALUE
                 :cljs js/Infinity)))

(def slug-spec
  (s/and string?
         not-empty
         #(re-find #"^[a-z0-9\-]+$" %)))

(s/fdef sequential-indices?
  :args (s/cat :maps (s/every map?))
  :ret boolean?)

(defn sequential-indices?
  [maps]
  (every? (fn [[idx idx']]
            (= idx idx'))
          (map-indexed vector
                       (sort (map :index maps)))))
