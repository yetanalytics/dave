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

;; Open spec for state, should be merged w/individual func
(def common-spec
  (s/keys :opt-un [::statement-count
                   ::timestamp-domain
                   ::stored-domain]))

(s/fdef update-domain
  :args (s/cat :domain (s/nilable su/inst-domain-spec)
               :datelike su/datelike-spec)
  :fn (fn [{{[s e :as ?domain-in] :domain
             d :datelike} :args
            [s' e' :as domain-out] :ret}]
        (let [s' (tc/to-date-time s')
              e' (tc/to-date-time e')
              d (tc/to-date-time (s/unform su/datelike-spec d))]
          (and
           ;; d is contained within the result
           (t/within? s'
                      e'
                      d)
           (or
            ;; No domain, making a new one
            (nil? ?domain-in)
            ;; The domain didn't change, as the statement fell in established
            ;; domain
            (= ?domain-in domain-out)
            ;; The domain did change...
            (let [s (tc/to-date-time s)
                  e (tc/to-date-time e)]
              ;; let's make sure it grew in one direction
              ;; or another
              (and
               (or (t/before? s' s)
                   (t/after? e' e))
               ;; Bounds check
               (t/within? s' e' s)
               (t/within? s' e' e)))))))
  :ret su/inst-domain-spec)

(defn update-domain
  [domain datelike]
  (if domain
    (-> domain
        (update 0 u/min-inst datelike)
        (update 1 u/max-inst datelike))
    (let [dt (tc/to-date datelike)]
      [dt dt])))

(s/fdef step-state
  :args (s/cat :state common-spec
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
  :ret common-spec)

(defn step-state
  "Given a statement successfully processed by a func, update that func's common
  state."
  [state
   {:strs [timestamp
           stored]
    :as statement}]
  (-> state
      (update :statement-count (fnil inc 0))
      (update :timestamp-domain update-domain timestamp)
      (update :stored-domain update-domain stored)))
