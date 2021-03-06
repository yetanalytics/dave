(ns com.yetanalytics.dave.workbook.data.lrs.auth
  (:require
   [clojure.spec.alpha :as s]
   ))

(s/def ::type #{::http-basic})

(def auth-common-spec
  (s/keys :req-un [::type]))

(defmulti auth-type :type)

(s/def :com.yetanalytics.dave.workbook.data.lrs.auth.http-basic/username
  (s/and string?
         not-empty))

(s/def :com.yetanalytics.dave.workbook.data.lrs.auth.http-basic/password
  (s/and string?
         not-empty))

(defmethod auth-type ::http-basic [_]
  (s/merge auth-common-spec
           (s/keys :req-un
                   [:com.yetanalytics.dave.workbook.data.lrs.auth.http-basic/username
                    :com.yetanalytics.dave.workbook.data.lrs.auth.http-basic/password])))

(def auth-spec
  (s/multi-spec auth-type :type))
