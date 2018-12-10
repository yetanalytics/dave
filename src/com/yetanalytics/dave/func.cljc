(ns com.yetanalytics.dave.func
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as sgen]
            [xapi-schema.spec :as xs]
            [com.yetanalytics.dave.func.ret :as ret]
            [com.yetanalytics.dave.func.common :as common]
            [com.yetanalytics.dave.func.util :as util]
            [clojure.walk :as w]))

(s/fdef success-timeline
  :args (s/cat
         :statements
         (s/every (s/with-gen ::xs/lrs-statement
                    (fn []
                      (sgen/fmap (fn [[s act id score]]
                                   (assoc-in
                                    (assoc-in (assoc s "object" act)
                                              ["object" "id"]
                                              id)
                                    ["result" "score"] score))
                                 (sgen/tuple
                                  (s/gen ::xs/lrs-statement)
                                  (s/gen ::xs/activity)
                                  (sgen/elements ["http://adlnet.gov/expapi/verbs/passed"
                                                  "https://w3id.org/xapi/dod-isd/verbs/answered"
                                                  "http://adlnet.gov/expapi/verbs/completed"])
                                  (sgen/fmap
                                   w/stringify-keys
                                   (s/gen (s/keys :req-un
                                                  [:score/min
                                                   :score/max
                                                   :score/raw])))))))))
  :ret ::ret/time-score)

(defn success-timeline
  "DAVE Section 2"
  [statements]
  (->> statements
       (filter (fn [{{:strs [id]} "verb"
                     {success "success"} "result"}]
                 (and (contains? #{"http://adlnet.gov/expapi/verbs/passed"
                                   "https://w3id.org/xapi/dod-isd/verbs/answered"
                                   "http://adlnet.gov/expapi/verbs/completed"}
                                 id)
                      (boolean? success))))
       (map (fn [{timestamp "timestamp"
                  {{:strs [raw min max]} "score"} "result"}]
              [timestamp (common/scale raw min max)]))))

(s/fdef difficult-questions
  :args (s/cat
         :statements
         (s/every ::xs/lrs-statement))
  :ret ::ret/category-count)

(defn difficult-questions
  "DAVE Section 3"
  [statements]
  (for [[activity-id ss]
        (group-by
         #(get-in % ["object" "id"])
         (filter (fn [{{o-type "objectType"
                        {a-type "type"} "definition"} "object"
                       {success "success"} "result"}]
                   (and
                    ;; An activity
                    (contains? #{"Activity" nil}
                               o-type)
                    ;; An interaction activity
                    (= a-type "http://adlnet.gov/expapi/activities/cmi.interaction")
                    ;; Failure
                    (false? success)))
                 statements))]
    [activity-id (count ss)]))

(s/fdef completion-rate
  :args (s/cat
         :statements
         (s/every (s/with-gen ::xs/lrs-statement
                    (fn []
                      (sgen/fmap (fn [[s act id]]
                                   (assoc-in (assoc s "object" act)
                                             ["object" "id"]
                                             id))
                                 (sgen/tuple
                                  (s/gen ::xs/lrs-statement)
                                  (s/gen ::xs/activity)
                                  (sgen/elements ["https://example.com/activity/a"
                                                  "https://example.com/activity/b"
                                                  "https://example.com/activity/c"]))))))
         :time-unit #{:second
                      :minute
                      :hour
                      :day
                      :week
                      :month
                      :year})
  :ret ::ret/category-rate)

(defn completion-rate
  "DAVE Section 4"
  [statements time-unit]
  (for [[activity-id ss] (group-by #(get-in % ["object" "id"])
                                   (filter
                                    (fn [s]
                                      (let [otype (get-in s ["object" "objectType"])]
                                        (contains? #{"Activity" nil}
                                                   otype)))
                                    statements))
        :let [s-count (count ss)]
        :when (< 1 s-count)
        :let [stamps (map #(.getTime
                            (util/timestamp->inst
                             (get % "timestamp")))
                          ss)
              min-ms (apply min stamps)
              max-ms (apply max stamps)]

        :when (not= min-ms max-ms)

        :let [delta-seconds (quot
                             (- max-ms min-ms)
                             1000)
              units (/ delta-seconds
                       (case time-unit
                         :second 1
                         :minute 60
                         :hour 3600
                         :day 86400
                         :week 604800
                         :month 2592000
                         :year 31536000))
              rate (double (/ s-count
                              units))]]
    [activity-id rate]))

(s/def ::function
  ifn?)

(s/def ::fspec
  s/spec?)

(def func-spec
  (s/keys :req-un [::function
                   ::fspec]))

(def registry
  "A map of function keyword to implementation. Each function is a map
  containing:
    * :function - a reference to the function
    * :fspec - the function spec, used to extract specs + introspect."
  {::success-timeline
   {:function success-timeline
    :fspec (s/get-spec `success-timeline)}
   ::difficult-questions
   {:function difficult-questions
    :fspec (s/get-spec `difficult-questions)}
   ::completion-rate
   {:function completion-rate
    :fspec (s/get-spec `completion-rate)}})

(s/def ::id
  (s/with-gen qualified-keyword?
    (fn []
      (sgen/elements (keys registry)))))

(s/fdef get-func
  :args (s/cat :id ::id)
  :ret func-spec)

(defn get-func
  "Gets a DAVE function map for the specified keyword or throws."
  [id]
  (if-let [func (get registry id)]
    func
    (throw (ex-info "DAVE Function not found"
                    {:type ::func-not-found
                     :id id}))))

(comment
  (get-func ::difficult-questions) ;; => {:function ..., :fspec ...}
  )

(s/fdef get-func-args-spec
  :args (s/cat :id ::id)
  :ret s/spec?)

(defn get-func-args-spec
  "Get the spec of a DAVE function's arguments"
  [id]
  (-> (get-func id)
      :fspec
      :args))

(comment
  (get-func-args-spec ::difficult-questions) ;; => #object[cljs.spec.alpha.t_cljs$spec$alpha9474]
  )

(s/fdef get-func-ret-spec
  :args (s/cat :id ::id)
  :ret s/spec?)

(defn get-func-ret-spec
  "Get the ret spec for a given DAVE function."
  [id]
  (-> (get-func id)
      :fspec
      :ret))

(comment
  (get-func-ret-spec ::difficult-questions) ;; => #object[cljs.spec.alpha.t_cljs$spec$alpha9300]
  )

(s/fdef get-func-ret-spec-k
  :args (s/cat :id ::id)
  :ret qualified-keyword?)

(defn get-func-ret-spec-k
  "Get the keyword of the registered ret spec for a given DAVE function."
  [id]
  (-> (get-func-ret-spec id)
      meta
      ::s/name))

(comment
  (get-func-ret-spec-k ::difficult-questions) ;; => :com.yetanalytics.dave.func.ret/category-count
  )

;; Function arg maps are attached to questions, and applied to the function.
;; They are expected to be suitable for use with s/unform
(s/def ::args
  map?)

(s/fdef explain-args*
  :args (s/cat :func func-spec
               :args-map ::args)
  :ret (s/nilable map?))

(defn explain-args*
  [{:keys [fspec] :as func} args-map]
  (let [args-spec (:args fspec)]
    (s/explain-data
     args-spec
     (s/unform args-spec (merge {:statements []}
                                args-map)))))

(s/fdef explain-args
  :args (s/cat :id ::id
               :args-map ::args)
  :ret (s/nilable map?))

(defn explain-args
  "Check a supplied args map against a DAVE function, returns nil if OK, or a
  spec error map if there are problems. If id is not found, throws."
  [id args-map]
  (explain-args* (get-func id) args-map))

(comment
  (explain-args ::completion-rate {:time-unit :parsec}) ;; => {:cljs.spec.alpha/problems [{:path [:time-unit], :pred #{:day :hour :week :second :month :year :minute} ... ;; failed
  (explain-args ::completion-rate {:time-unit :minute}) ;; => nil ;; OK!
  )

(s/fdef apply-func
  :args (s/cat :id ::id
               :args-map map?
               :statements (s/every ::xs/lrs-statement)))

(defn apply-func
  "Apply a DAVE function given a function key, args map (maybe nil) and a
  coll of statements"
  [id args-map statements]
  (let [func (get-func id)
        fspec (:fspec func)]
    (apply
     (:function func)
     statements
     (s/unform (:args fspec)
               args-map))))
