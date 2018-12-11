(ns com.yetanalytics.dave.workbook.data
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.util.spec :as su]
            [com.yetanalytics.dave.workbook.data.file :as file]
            [com.yetanalytics.dave.workbook.data.lrs :as lrs]
            [xapi-schema.spec :as xs]
            [dave.tag :as tag]))

(s/def ::type
  #{::file
    ::lrs})

(s/def ::statements
  (s/every ::xs/statement))

(s/def ::title
  string?)

(s/def :error/message
  string?)

(s/def :error/type
  qualified-keyword?)

(s/def ::error
  (s/keys :req-un [:error/message
                   :error/type]))

(s/def ::errors
  (s/every ::error))

(def data-common-spec
  (s/keys
   :req-un [::title
            ::tag/tags]
   :opt-un [::statements
            ::errors]))

(defmulti data-type :type)

(defmethod data-type ::file [_]
  (s/merge data-common-spec
           file/partial-spec))

(defmethod data-type ::lrs [_]
  (s/merge data-common-spec
           lrs/partial-spec))

(def data-spec
  (s/multi-spec data-type :type))
