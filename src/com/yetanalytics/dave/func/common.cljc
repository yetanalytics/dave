(ns com.yetanalytics.dave.func.common
  (:require [clojure.spec.alpha :as s]))

(s/fdef scale
  :args (s/cat :raw number?
               :min number?
               :max number?)
  :ret (s/double-in :min 0.0
                    :max 100.0))


(defn scale
  "Given a number (raw) and its native domain (min and max), scale raw to the
  domain of 0.0..100.0 DAVE 2.6.6"
  [raw min max]
  (double
   (/ (* 100 (- raw min))
      (- max min))))
