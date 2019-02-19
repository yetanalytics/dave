(ns com.yetanalytics.dave.func.state
  "Common specs and functions for stateful funcs"
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as sgen]
            [com.yetanalytics.dave.util.spec :as su]
            [xapi-schema.spec :as xs]
            [com.yetanalytics.dave.func.util :as u]
            [#?(:clj clj-time.core
                :cljs cljs-time.core) :as t]
            [#?(:clj clj-time.coerce
                :cljs cljs-time.coerce) :as tc]))

(s/def ::statement-count
  su/index-spec)

(s/def ::timestamp-domain
  su/inst-domain-spec)

(s/def ::stored-domain
  su/inst-domain-spec)

(def spec
  (s/keys :opt-un [::statement-count
                   ::timestamp-domain
                   ::stored-domain]))

(s/fdef step-state
  :args (s/cat :state spec
               :statement ::xs/lrs-statement)
  :fn (fn [{{{statement-count-in :statement-count} :state
             {timestamp-in :statement/timestamp
              stored-in :statement/stored
              :as statement} :statement} :args
            {statement-count-out :statement-count
             timestamp-domain-out :timestamp-domain
             stored-domain-out :stored-domain
             } :ret}]
        (let [
              stored (tc/to-date-time stored-in)
              timestamp (tc/to-date-time timestamp-in)
              [sdo0 sdo1] (mapv tc/to-date-time stored-domain-out)
              [tdo0 tdo1] (mapv tc/to-date-time timestamp-domain-out)]
          (and (= statement-count-out ((fnil inc 0) statement-count-in))
               ;; Statement is contained in both out domains
               (t/within? sdo0 sdo1 stored)
               (t/within? tdo0 tdo1 timestamp))))
  :ret spec)

(defn step-state
  "Given a statement successfully processed by a func, update that func's common
  state."
  [state
   {:strs [timestamp
           stored]
    :as statement}]
  (-> state
      (update :statement-count (fnil inc 0))
      (cond->
        timestamp
        (update :timestamp-domain u/update-domain timestamp)
        stored
        (update :stored-domain u/update-domain stored))))
