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

(def partial-spec
  (s/keys :opt-un [::query
                   ::auth
                   ::more]))
