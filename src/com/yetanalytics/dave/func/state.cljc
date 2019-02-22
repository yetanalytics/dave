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

;; Range of timestamps, tuple.
(s/def ::timestamp-domain
  su/inst-domain-spec)

;; range of stored stamps, tuple. Used to query for funcs that should update
(s/def ::stored-domain
  su/inst-domain-spec)

;; Since multiple statements can share a stored time, we need to store all
;; statement IDs for the latest stored time (checkpoint) so we can maintain
;; idempotency/monotonicity.
(s/def :checkpoint/stored
  :statement/stored)

(s/def :checkpoint/ids
  (s/every :statement/id
           :kind set?
           :into #{}))

(s/def ::checkpoint
  (s/keys :req-un [:checkpoint/stored
                   :checkpoint/ids]))

(def spec
  (s/keys :opt-un [::statement-count
                   ::timestamp-domain
                   ::stored-domain
                   ::checkpoint]))

(s/fdef update-checkpoint
  :args (s/cat :checkpoint ::checkpoint
               :statement ::xs/lrs-statement)
  :ret ::checkpoint)

(defn update-checkpoint
  "For a given statement, if its stored time is EXACTLY equal to the checkpoint
  stored stamp, add it to the ids set. If it is later, replace the stamp and
  create a new set with only the statement id."
  [{checkpoint-stored :stored
    :as checkpoint}
   {:strs [id stored] :as statement}]
  (if (and checkpoint (= stored checkpoint-stored))
    (update checkpoint :ids conj id)
    {:stored stored
     :ids #{id}}))

(s/fdef after-checkpoint?
  :args (s/cat :checkpoint ::checkpoint
               :statement ::xs/lrs-statement)
  :ret boolean?)

(defn after-checkpoint?
  "Returns true if this statement falls after the checkpoint and should be
  processed"
  [{checkpoint-stored :stored
    ids :ids
    :as checkpoint}
   {:strs [id stored] :as statement}]
  (or (nil? checkpoint)
      (and (= checkpoint-stored
              stored)
           (not (contains? ids id)))
      (t/after? (tc/to-date-time stored)
                (tc/to-date-time checkpoint-stored))))

(s/fdef accept?
  :args (s/cat :state spec
               :statement ::xs/lrs-statement)
  :ret boolean?)

(defn accept?
  "Should the func accept this statement given its state?"
  [state
   statement]
  (after-checkpoint? (:checkpoint state)
                     statement))

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
   {:strs [id
           timestamp
           stored]
    :as statement}]
  (-> state
      (update :statement-count (fnil inc 0))
      ;; TODO: we should throw when these aren't present
      (cond->
        timestamp
        (update :timestamp-domain u/update-domain timestamp)
        stored
        (update :stored-domain u/update-domain stored))
      (update :checkpoint update-checkpoint statement)))

(s/fdef earliest-last-stored
  :args (s/cat :states (s/every spec))
  :ret ::xs/timestamp)

(defn earliest-last-stored
  "Given a seq of states, get the inst representing the earliest last-stored
  timestamp as a string"
  [states]
  (tc/to-string
   (if-let [last-storeds (not-empty (keep
                                     #(get-in % [:stored-domain 1]
                                              (get-in % [:checkpoint :stored]))
                                     states))]
     (apply u/min-inst last-storeds)
     (t/epoch))))

(s/fdef force-last-stored
  :args (s/cat :state spec
               :datelike su/datelike-spec)
  :ret spec)

(defn force-last-stored
  "Given a datelike, force its last stored date to be the datelike. Sets the
  first stored to the epoch if not present. Reinitializes the checkpoint"
  [state datelike]
  (-> state
      (update :stored-domain
              (fn [[s e] d]
                [(or s
                     (tc/to-date (t/epoch)))
                 d])
              (tc/to-date datelike))
      (assoc :checkpoint
             {:stored (tc/to-string datelike)})))
