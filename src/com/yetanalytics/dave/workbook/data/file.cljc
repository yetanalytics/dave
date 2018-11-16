(ns com.yetanalytics.dave.workbook.data.file
  (:require
   [clojure.spec.alpha :as s]
   [com.yetanalytics.dave.util.spec :as su]))

(s/def ::uri
  (s/and string?
         not-empty))

(def partial-spec
  (s/keys :req-un [::uri]))
