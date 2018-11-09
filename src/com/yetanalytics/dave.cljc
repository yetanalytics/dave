(ns com.yetanalytics.dave
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.workbook :as workbook]))

;; TODO: remove this boiler
(println "This text is printed from src/com/yetanalytics/dave.cljs. Go ahead and edit it and see reloading in action.")
(defn multiply [a b] (* a b))


;; DAVE Object Specs
(s/def ::workbook
  workbook/workbook-spec)
