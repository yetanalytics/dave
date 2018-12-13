(ns com.yetanalytics.dave.func
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as sgen]
            [xapi-schema.spec :as xs]
            [com.yetanalytics.dave.func.ret :as ret]
            [com.yetanalytics.dave.func.common :as common]
            [com.yetanalytics.dave.func.util :as util]
            [clojure.walk :as w]
            #?@(:cljs [[goog.string :refer [format]]
                       [goog.string.format]])))

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
  :ret ::ret/result)

(defn success-timeline
  "DAVE Section 2"
  [statements]
  {:specification
   {:x {:type :time
        :label "Timestamp"}
    :y {:type :decimal
        :label "Score"
        :domain [0.0 100.0]}}
   :values
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
               {:x (.getTime (util/timestamp->inst timestamp))
                :y (common/scale raw min max)}))
        (into []))})


(s/fdef difficult-questions
  :args (s/cat
         :statements
         (s/every ::xs/lrs-statement))
  :ret ::ret/result)

(defn difficult-questions
  "DAVE Section 3"
  [statements]
  {:specification
   {:x {:type :category
        :label "Activity"}
    :y {:type :count
        :label "Success Count"}}
   :values
   (into []
         (for [[[activity-id
                 activity-name] ss]
               (group-by
                (juxt #(get-in % ["object" "id"])
                      #(get-in % ["object" "definition" "name" "en-US"]))
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
           {:x (or activity-name activity-id)
            :y (count ss)}))})

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
  :ret ::ret/result)

(defn completion-rate
  "DAVE Section 4"
  [statements time-unit]
  {:specification
   {:x {:type :category
        :label "Activity"}
    :y {:type :decimal
        :label (format "Completions per %s" (name time-unit))}}
   :values
   (into []
         (for [[[activity-id activity-name] ss]
               (group-by
                (juxt #(get-in % ["object" "id"])
                      #(get-in % ["object" "definition" "name" "en-US"]))
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
           {:x (or activity-name
                   activity-id)
            :y rate}))})

(s/fdef followed-recommendations
  :args (s/cat
         :statements
         (s/every
          (s/with-gen ::xs/lrs-statement
            (fn []
              (sgen/fmap (fn [[s act v-id follow?]]
                           (-> s
                               (assoc "verb"
                                      {"id" v-id})
                               (assoc "object" act)
                               (cond->
                                   (and follow?
                                        (= "http://adlnet.gov/expapi/verbs/launched"))
                                 (assoc-in ["context" "statement"]
                                           #?(:clj (str (java.util.UUID/randomUUID))
                                              :cljs (str (random-uuid)))))))
                         (sgen/tuple
                          (s/gen ::xs/lrs-statement)
                          (s/gen ::xs/activity)
                          (sgen/elements ["http://adlnet.gov/expapi/verbs/launched"
                                          "https://w3id.org/xapi/dod-isd/verbs/recommended"])
                          (sgen/boolean))))))
         :time-unit #{:second
                      :minute
                      :hour
                      :day
                      :week
                      :month
                      :year})

  :ret ::ret/result)

(defn followed-recommendations
  "Dave Section 5"
  [statements time-unit]
  (let [buckets (util/time-bucket-statements statements time-unit)]
    {:specification
     {:x {:label "Period"
          #_:format #_(case time-unit
                       :second
                       "%Y-%m-%dT%H:%M:%S"
                       :minute
                       "%Y-%m-%dT%H:%M"
                       :hour
                       "%Y-%m-%dT%H"
                       :day
                       "%Y-%m-%d"
                       :week
                       "%YW%V"
                       :month
                       "%Y-%m"
                       :year
                       "%Y")}
      :y {:type :count
          :label "Statement Count"}
      :c {:type :category}}
     :values
     (into []
           (for [{:keys [period-start
                         period-end
                         statements]
                  :as bucket} buckets
                 vtype [:recommended :launched :followed]]
             {:x (str (util/format-time-unit
                       period-start
                       time-unit)
                      " - "
                      (util/format-time-unit
                       period-end
                       time-unit))
              :y (count
                  (filter
                   (case vtype
                     :recommended
                     #(= "https://w3id.org/xapi/dod-isd/verbs/recommended"
                         (get-in % ["verb" "id"]))
                     :launched
                     #(= "http://adlnet.gov/expapi/verbs/launched"
                         (get-in % ["verb" "id"]))
                     :followed
                     #(and (= "http://adlnet.gov/expapi/verbs/launched"
                              (get-in % ["verb" "id"]))
                           ;; a statement ref exists (assumed to be recommendation)
                           (get-in % ["context" "statement"])))
                   statements))
              :c (name vtype)}))}))


(s/def ::function
  ifn?)

(s/def ::fspec
  s/spec?)

(s/def ::title
  (s/and string?
         not-empty))

(s/def ::doc
  (s/and string?
         not-empty))

;; default args get merged with the provided ones.
(s/def ::args-default
  (s/map-of keyword?
            (s/with-gen identity
              (fn []
                (sgen/keyword-ns)))))

;; Some args have predefined choices, list them here
(s/def ::args-enum
  (s/map-of keyword?
            (s/every
             keyword?)))

(def func-spec
  (s/keys :req-un [::function
                   ::fspec
                   ::title
                   ::args-default
                   ::args-enum]
          :opt-un [::doc]))

(def registry
  "A map of function keyword to implementation. Each function is a map
  containing:
    * :function - a reference to the function
    * :fspec - the function spec, used to extract specs + introspect.
    * :title - a human-readable name for the function
    * :doc - a longer human-readable description of what the function does"
  {::success-timeline
   {:title "Success Timeline"
    :doc "Plots the timestamp of successful statements against the score of their result."
    :function success-timeline
    :fspec (s/get-spec `success-timeline)
    :args-default {}
    :args-enum {}}
   ::difficult-questions
   {:title "Difficult Questions"
    :doc "Plots interaction activity ids against the number of failed attempts for that activity."
    :function difficult-questions
    :fspec (s/get-spec `difficult-questions)
    :args-default {}
    :args-enum {}}
   ::completion-rate
   {:title "Completion Rate"
    :doc "Plots activity ids against the rate of failed attempts per given time unit."
    :function completion-rate
    :fspec (s/get-spec `completion-rate)
    :args-default {:time-unit :day}
    :args-enum {:time-unit #{:second
                             :minute
                             :hour
                             :day
                             :week
                             :month
                             :year}}}
   ::followed-recommendations
   {:title "Followed Recommendations"
    :doc "Buckets statements into periods (time ranges) by statement timestamp. Within each bucket, counts the number of recommendations, launches and follows expressed."
    :function followed-recommendations
    :fspec (s/get-spec `followed-recommendations)
    :args-default {:time-unit :month}
    :args-enum {:time-unit #{:second
                             :minute
                             :hour
                             :day
                             :week
                             :month
                             :year}}}})

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
  (get-func ::difficult-questions)) ;; => {:function ..., :fspec ...}


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
  (get-func-args-spec ::difficult-questions)) ;; => #object[cljs.spec.alpha.t_cljs$spec$alpha9474]


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
  (get-func-ret-spec ::difficult-questions)) ;; => #object[cljs.spec.alpha.t_cljs$spec$alpha9300]


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
  (get-func-ret-spec-k ::difficult-questions)) ;; => :com.yetanalytics.dave.func.ret/category-count


;; Function arg maps are attached to questions, and applied to the function.
;; They are expected to be suitable for use with s/unform
(s/def ::args
  map?)

(s/fdef explain-args*
  :args (s/cat :func func-spec
               :args-map ::args)
  :ret (s/nilable map?))

(defn explain-args*
  [{:keys [fspec args-default] :as func} args-map]
  (let [args-spec (:args fspec)]
    (s/explain-data
     args-spec
     (s/unform args-spec
               (merge {:statements []}
                      args-default
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
  (explain-args ::completion-rate)) ;; => nil ;; OK, because the default arg (:day) was added


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
