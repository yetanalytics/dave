(ns com.yetanalytics.dave.workbook.data
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.util.spec :as su]
            [com.yetanalytics.dave.workbook.data.file :as file]
            [com.yetanalytics.dave.workbook.data.lrs :as lrs]
            [xapi-schema.spec :as xs]
            [com.yetanalytics.dave.workbook.data.state :as state]))

(s/def ::type
  #{::file
    ::lrs})

(s/def ::title
  string?)

;; track state
(s/def ::state
  state/spec)

;; Is the data currently loading?
(s/def ::loading?
  boolean?)

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
   :req-un [::title]
   :opt-un [::loading?
            ::state
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
