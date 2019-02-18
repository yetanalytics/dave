(ns com.yetanalytics.dave.util.spec
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as sgen]
            [xapi-schema.spec :as xs]
            ))

(def string-not-empty-spec
  (s/and string?
         not-empty))

(def index-spec
  (s/int-in 0 #?(:clj Integer/MAX_VALUE
                 :cljs js/Infinity)))

(def datelike-spec
  (s/or :inst inst?
        :epoch (s/with-gen pos-int?
                 (fn []
                   (sgen/fmap inst-ms
                              (s/gen inst?))))
        :string ::xs/timestamp))


(s/fdef sequential-indices?
  :args (s/cat :maps (s/every map?))
  :ret boolean?)

(defn sequential-indices?
  [maps]
  (every? (fn [[idx idx']]
            (= idx idx'))
          (map-indexed vector
                       (sort (map :index maps)))))

;; Generators
(defn raw-min-max-gen []
  (sgen/bind
   (sgen/double* {:infinite? false
                  :NaN? false})
   (fn [mn]
     (sgen/bind
      (sgen/double* {:infinite? false
                     :NaN? false
                     :min mn})
      (fn [mx]
        (sgen/tuple
         (sgen/double* {:infinite? false
                        :NaN? false
                        :min mn
                        :max mx})
         (sgen/return mn)
         (sgen/return mx)))))))
