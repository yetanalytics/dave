(ns com.yetanalytics.dave.workbook.data.lrs
  (:require
   [clojure.spec.alpha :as s]
   [com.yetanalytics.dave.util.spec :as su]
   [xapi-schema.spec.resources :as xsr]
   [com.yetanalytics.dave.workbook.data.lrs.auth :as auth]))

(s/def ::query
  :xapi.statements.GET.request/params)

(s/def ::more
  :xapi.statements.GET.response.statement-result/more)

(s/def ::auth
  auth/auth-spec)

(s/def ::endpoint
  (s/and string?
         not-empty))

(s/def ::title
  (s/and string?
         not-empty))

(def partial-spec
  (s/keys
   :req-un [::endpoint]
   :opt-un [::query
            ::auth
            ::more
            ::title]))
