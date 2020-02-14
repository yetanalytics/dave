(ns com.yetanalytics.dave.workbook.data.state
  "State of received xAPI Data. Used in tracking for Workbook Data"
  (:require
   [clojure.spec.alpha :as s]
   [com.yetanalytics.dave.datalog :as d]
   [xapi-schema.spec :as xs]
   [com.yetanalytics.dave.util.spec :as su]
   [#?(:clj clj-time.core
        :cljs cljs-time.core) :as t]
   [#?(:clj clj-time.coerce
        :cljs cljs-time.coerce) :as tc]))

(s/def ::db
  ::d/db)

;; Assumption: Statements are provided contiguously, in order.
;;
(s/def ::statement-idx
  ;; Index of last statement from epoch or since. -1 means no statement
  ;; (= (inc statement-idx) statement-count)
  (s/int-in -1 #?(:clj Integer/MAX_VALUE
                  :cljs js/Infinity)))

(s/def ::stored-domain
  ;; First and last stored stamps observed
  (s/tuple ::xs/timestamp
           ::xs/timestamp))

(s/def ::timestamp-domain
  ;; First and last timestamp stamps observed
  (s/tuple ::xs/timestamp
           ::xs/timestamp))

(def spec
  (s/keys
   :req-un [::statement-idx
            ::db]
   :opt-un [::stored-domain
            ::stored-timestamp]))

(def init-state
  {:statement-idx -1
   :db (d/empty-db)})

(defn- update-domain
  [[s e] k ss]
  [(or s
       (-> ss
           first
           (get k)))
   (-> ss
       last
       (get k))])

(s/fdef update-state
  :args (s/cat :state spec
               :statements ::xs/lrs-statements)
  :ret spec)

(defn update-state
  [state statements]
  (-> state
      (update :db (fnil d/transact (d/empty-db)) statements)
      (update :statement-idx
              + (count statements))
      (cond->
          (not-empty statements)
        (-> (update :stored-domain
                    update-domain
                    "stored"
                    statements)
            (update :timestamp-domain
                    update-domain
                    "timestamp"
                    statements)))))

(s/fdef statement-count
  :args (s/cat :state spec)
  :ret su/index-spec)

(defn statement-count
  [{:keys [statement-idx]}]
  (inc statement-idx))


(s/fdef accept?
  :args (s/cat :state spec
               :idx-range
               (s/tuple su/index-spec
                        su/index-spec))
  :ret boolean?)

(defn accept?
  [{:keys [statement-idx]}
   [first-statement-idx
    last-statement-idx]]

  (and
   (< statement-idx last-statement-idx)
   (>= (inc statement-idx) first-statement-idx)))

(s/fdef trim
  :args (s/cat :state spec
               :idx-range
               (s/tuple su/index-spec
                        su/index-spec)
               :statements ::xs/lrs-statements)
  :ret (s/tuple
        (s/tuple su/index-spec
                 su/index-spec)
        ::xs/lrs-statements))

(defn trim
  [{:keys [statement-idx]}
   [fidx lidx]
   statements]
  (let [fidx' (if (>= statement-idx fidx)
                (inc statement-idx)
                fidx)]
    [
     [fidx'
      lidx]

     (into []
           (drop (- fidx'
                    fidx))
           statements)]))
