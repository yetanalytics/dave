(ns com.yetanalytics.dave.func
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as sgen]
            [xapi-schema.spec :as xs]
            [com.yetanalytics.dave.func.common :as common]
            [com.yetanalytics.dave.func.util :as util]))

(s/fdef success-timeline
  :args (s/cat
         :statements
         (s/every ::xs/statement))
  :ret (s/every
        (s/tuple ::xs/timestamp
                 (s/double-in
                  :min 0.0
                  :max 100.0))))

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
         (s/every ::xs/statement))
  :ret (s/every
        (s/tuple :activity/id
                 pos-int?)))

(defn difficult-questions
  "DAVE Section 3"
  [statements]
  (seq
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
            statements))))





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
  :ret (s/every
        (s/tuple :activity/id
                 (s/double-in :min 0.0
                              :infinite? false
                              :NaN? false))))

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
