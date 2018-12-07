(ns com.yetanalytics.dave.workbook.data.lrs.auth
  (:require
   [clojure.spec.alpha :as s]
   ))

(s/def ::type #{::http-basic})

(def auth-common-spec
  (s/keys :req-un [::type]))

(defmulti auth-type :type)

(s/def :com.yetanalytics.dave.workbook.data.lrs.auth.http-basic/username
  string?)

(s/def :com.yetanalytics.dave.workbook.data.lrs.auth.http-basic/password
  string?)

(defmethod auth-type ::http-basic [_]
  (s/merge auth-common-spec
           (s/keys :req [:com.yetanalytics.dave.workbook.data.lrs.auth.http-basic/username
                         :com.yetanalytics.dave.workbook.data.lrs.auth.http-basic/password])))

(def auth-spec
  (s/multi-spec auth-type :type))
