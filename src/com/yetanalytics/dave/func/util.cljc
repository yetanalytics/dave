(ns com.yetanalytics.dave.func.util
  (:require [clojure.spec.alpha :as s :include-macros true]
            [xapi-schema.spec :as xs]
            [#?(:clj clj-time.core
                :cljs cljs-time.core) :as t]
            [#?(:clj clj-time.format
                :cljs cljs-time.format) :as tf]
            [#?(:clj clj-time.coerce
                :cljs cljs-time.coerce) :as tc]
            [#?(:clj clj-time.periodic
                :cljs cljs-time.periodic) :as tp]
            )
  #?(:clj (:import [java.util Date TimeZone]
                   [java.time
                    Instant
                    OffsetDateTime]
                   [java.time.format DateTimeFormatter])))



#?(:clj (set! *warn-on-reflection* true))

#?(:clj (def ^DateTimeFormatter iso-fmt
          DateTimeFormatter/ISO_DATE_TIME))

(s/fdef timestamp->inst
  :args (s/cat :timestamp ::xs/timestamp)
  :ret inst?)

#?(:clj
   (defn timestamp->inst
     ^Date [^String timestamp]
     (Date/from (.toInstant (OffsetDateTime/parse timestamp iso-fmt))))
   :cljs
   (defn timestamp->inst
     [timestamp]
     (js/Date. timestamp)))

(s/fdef inst->timestamp
  :args (s/cat :inst inst?)
  :ret ::xs/timestamp)

#?(:clj (defn inst->timestamp
          [^Date inst]
          (.toString ^Instant (.toInstant inst)))
   :cljs (defn inst->timestamp
           [inst]
           (.toISOString inst)))

(s/def :time-bucket-statements.ret.each/period-start
  ::xs/timestamp)

(s/def :time-bucket-statements.ret.each/period-end
  ::xs/timestamp)

(s/def :time-bucket-statements.ret.each/statements
  (s/every ::xs/lrs-statements))

(s/fdef time-bucket-statements
  :args (s/cat :statements (s/every ::xs/lrs-statements)
               :time-unit #{:second
                            :minute
                            :hour
                            :day
                            :week
                            :month
                            :year})
  :ret (s/every (s/keys :req-un [:time-bucket-statements.ret.each/period-start
                                 :time-bucket-statements.ret.each/period-end
                                 :time-bucket-statements.ret.each/statements])))

(defn time-bucket-statements
  "Given statements and a time-unit keyword, bucket the statements into periods
  of that time unit.
  Options:
    * elide? don't include periods w/o statements

  Returns a vector of maps with:
    * :period-start - iso-8601 stamp of the period start
    * :period-end - same, for end
    * :statements - vector of statements, sorted by timestamp (ascending) for
      the given period."
  [statements time-unit & {:keys [elide?]
                           :or {elide? false}}]
  (let [time-unit-like (case time-unit
                         :second (t/seconds 1)
                         :minute (t/minutes 1)
                         :hour   (t/hours 1)
                         :day    (t/days 1)
                         :week   (t/weeks 1)
                         :month  (t/months 1)
                         :year   (t/years 1))
        statements-sorted
        (sort-by #(-> % (get "timestamp") timestamp->inst)
                 statements)
        [stamp-min
         stamp-max
         :as domain] ((juxt (comp #(get % "timestamp") first)
                            (comp #(get % "timestamp") last))
                      statements-sorted)
        ;; Pad and make a seq from the domain
        pseq (tp/periodic-seq (t/minus (tc/to-date-time stamp-min)
                                       time-unit-like)
                              (t/plus (tc/to-date-time stamp-max)
                                      time-unit-like)
                              time-unit-like)
        [rest-s buckets] (reduce
                          (fn [[ss buckets] start]
                            (let [end (t/plus start time-unit-like)
                                  interval (t/interval start end)
                                  [in-period rest-ss] (split-with (comp
                                                                   (partial t/within? interval)
                                                                   tc/to-date-time
                                                                   #(get % "timestamp"))
                                                                  ss)]
                              (if (and elide? (empty? in-period))
                                [rest-ss buckets]
                                [rest-ss
                                 (conj buckets
                                       {:period-start (tf/unparse (tf/formatters :date-time) start)
                                        :period-end   (tf/unparse (tf/formatters :date-time) end)
                                        :statements (into [] in-period)})])))
                          [statements-sorted []]
                          pseq)]
    buckets))

(s/fdef format-time-unit
  :args (s/cat :d (s/or :inst inst?
                        :stamp ::xs/timestamp)
               :time-unit #{:second
                            :minute
                            :hour
                            :day
                            :week
                            :month
                            :year})
  :ret string?)

(defn format-time-unit
  "Format a date object for the given time-unit"
  [d
   time-unit]
  (tf/unparse (tf/formatters
               (case time-unit
                 :second :date-hour-minute-second
                 :minute :date-hour-minute
                 :hour   :date-hour
                 :day    :date
                 :week   :week-date
                 :month  :year-month
                 :year   :year))
              (tc/to-date-time d)))
