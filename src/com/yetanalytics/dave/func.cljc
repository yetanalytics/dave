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

(s/def ::ret-spec
  qualified-keyword?)

(s/def ::fspec
  s/spec?)

(def func-spec
  (s/keys :req-un [::function
                   ::fspec
                   ::ret-spec]))

(def registry
  "A map of function keyword to implementation. Each function is a map
  containing:
    * :function - a reference to the function
    * :fspec - the function spec, used to extract specs + introspect
    * :ret-spec - the keyword under which the return spec is registered. Useful
      for matching functions to vis."
  {::success-timeline
   {:function success-timeline
    :fspec (s/get-spec `success-timeline)
    :ret-spec ::ret/time-score}
   ::difficult-questions
   {:function difficult-questions
    :fspec (s/get-spec `difficult-questions)
    :ret-spec ::ret/category-count}
   ::completion-rate
   {:function completion-rate
    :fspec (s/get-spec `completion-rate)
    :ret-spec ::ret/category-rate}})
