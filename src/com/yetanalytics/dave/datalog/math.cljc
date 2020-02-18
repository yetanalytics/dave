(ns com.yetanalytics.dave.datalog.math)

(def transform-fn {:log js/Math.log
               :abs js/Math.abs})

(defn transform
  [format data]
  ((get transform-fn format) data))

(transform :abs 4)
