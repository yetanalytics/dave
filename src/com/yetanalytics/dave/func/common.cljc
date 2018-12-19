(ns com.yetanalytics.dave.func.common
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as sgen]
            [com.yetanalytics.dave.util.spec :as su]))

(s/fdef scale
  :args (s/with-gen (s/cat :raw number?
                           :min number?
                           :max number?)
          su/raw-min-max-gen)
  :ret (s/double-in :min 0.0
                    :max 100.0))


(defn scale
  "Given a number (raw) and its native domain (min and max), scale raw to the
  domain of 0.0..100.0 DAVE 2.6.6"
  [raw min max]
  (let [rng (- max min)]
    (if (< 0 rng)
      (double
       (* 100
          (/ (- raw min)
             rng)))
      100.0)))
