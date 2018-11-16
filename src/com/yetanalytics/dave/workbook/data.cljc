(ns com.yetanalytics.dave.workbook.data
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.util.spec :as su]
            [com.yetanalytics.dave.workbook.data.file :as file]
            [com.yetanalytics.dave.workbook.data.lrs :as lrs]
            [xapi-schema.spec :as xs]))

(s/def ::type
  #{::file
    ::lrs})

(s/def ::statements
  (s/every ::xs/statement))

(def data-common-spec
  (s/keys :opt-un [::statements]))

(defmulti data-type :type)

(defmethod data-type ::file [_]
  (s/merge data-common-spec
           file/partial-spec))

(defmethod data-type ::lrs [_]
  (s/merge data-common-spec
           lrs/partial-spec))

(def data-spec
  (s/multi-spec data-type :type))
