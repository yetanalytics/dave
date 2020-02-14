(ns com.yetanalytics.dave
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.workbook :as workbook]))

;; DAVE Object Specs
(s/def ::workbook
  workbook/workbook-spec)
