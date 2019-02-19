(ns com.yetanalytics.dave.func.util
  (:require [clojure.spec.alpha :as s :include-macros true]
            [clojure.spec.gen.alpha :as sgen]
            [xapi-schema.spec :as xs]
            [com.yetanalytics.dave.util.spec :as su]
            [#?(:clj clj-time.core
                :cljs cljs-time.core) :as t]
            [#?(:clj clj-time.format
                :cljs cljs-time.format) :as tf]
            [#?(:clj clj-time.coerce
                :cljs cljs-time.coerce) :as tc]
            [#?(:clj clj-time.periodic
                :cljs cljs-time.periodic) :as tp])
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
  :args (s/cat :inst (s/inst-in #inst "1970"
                                #inst "3000"))
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
  (s/every ::xs/lrs-statement))

(s/fdef time-bucket-statements
  :args (s/cat :statements
               (s/every
                (s/with-gen ::xs/lrs-statement
                  (fn []
                    (sgen/fmap (fn [[id stamp [act-id act-name]]]
                                 {"id" id
                                  "actor" {"objectType" "Agent"
                                           "mbox" "mailto:xapi@example.com"}
                                  "verb" {"id" "https://w3id.org/xapi/dod-isd/verbs/answered"}
                                  "object" {"objectType" "Activity"
                                            "id" act-id
                                            "definition" {"name" {"en-US" act-name}}}
                                  "timestamp" stamp
                                  "stored" stamp
                                  "authority" {"objectType" "Agent"
                                               "account" {"homePage" "https://example.com"
                                                          "name" "username"}}
                                  "version" "1.0.3"})
                               (sgen/tuple
                                ;; id
                                (sgen/fmap str (sgen/uuid))
                                ;; timestamp/stored
                                (sgen/fmap (fn [i]
                                             (inst->timestamp
                                              #?(:clj (java.util.Date. ^Long i)
                                                 :cljs (js/Date. i))))
                                           ;; one year span
                                           (sgen/large-integer*
                                            {:min 1546300800000
                                             :max 1577836800000}))
                                ;; activity id/name tuple
                                (sgen/fmap (fn [x]
                                             [(str "https://example.com/activities/" x)
                                              (str "Activity " x)])
                                           (sgen/elements ["a" "b" "c"]))

                                )))))
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
  (if (seq statements)
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
      buckets)
    []))

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

(s/fdef min-inst
  :args (s/cat :d1
               su/datelike-spec
               :d2
               su/datelike-spec
               :more
               (s/? (s/* su/datelike-spec)))
  :ret su/datelike-spec)

(defn min-inst
  "Given datelikes, return the earliest, as an inst"
  ([d]
   (tc/to-date d))
  ([d1 d2]
   (tc/to-date
    (t/earliest (tc/to-date-time d1)
                (tc/to-date-time d2))))
  ([d1 d2 & more]
   (tc/to-date
    (t/earliest
     (map tc/to-date-time
          (concat [d1 d2]
                  more))))))

(s/fdef max-inst
  :args (s/cat :d1
               su/datelike-spec
               :d2
               su/datelike-spec
               :more
               (s/? (s/* su/datelike-spec)))
  :ret su/datelike-spec)

(defn max-inst
  "Given datelikes, return the latest, as an inst"
  ([d]
   (tc/to-date d))
  ([d1 d2]
   (tc/to-date
    (t/latest (tc/to-date-time d1)
              (tc/to-date-time d2))))
  ([d1 d2 & more]
   (tc/to-date
    (t/latest
     (map tc/to-date-time
          (concat [d1 d2]
                  more))))))

(s/def ::leaf-fn
  (s/with-gen ifn?
    (fn []
      (sgen/elements
       [count
        identity
        frequencies]))))

(s/def ::map-fn
  #{hash-map
    array-map
    sorted-map})

(s/def ::drop-nil-keys?
  boolean?)

(s/fdef nested-group-by+
  :args (s/cat :kfs
               (s/every
                (s/or :vector vector?
                      :function (s/with-gen ifn?
                                  (fn []
                                    (sgen/elements
                                     [odd?
                                      even?]))))
                :kind vector?
                :into [])
               :coll (s/with-gen coll?
                       (fn []
                         (sgen/vector (sgen/int))))
               :options (s/keys*
                         :opt-un
                         [::leaf-fn
                          ::map-fn
                          ::drop-nil-keys?]))
  :ret (s/nilable coll?))

(defn nested-group-by+
  "Recursively runs group-by on a collection according to a vector of key
  functions (or get-in path vectors). The result is a nested map with
  partitioned data. Entries with nil/empty values are always removed.

  Options:
    :leaf-fn - function to run on leaf nodes, for instance count
    :map-fn - function used to create maps, default is hash-map
    :drop-nil-keys? - if true, entries with nil keys are removed.
  Credit to @gtrak for the original."
  [[kf & more :as kfs]
   coll
   & {:keys [leaf-fn
             map-fn
             drop-nil-keys?]
      :or {leaf-fn identity
           map-fn hash-map
           drop-nil-keys? false}}]
  (if (seq kfs)
    (reduce-kv
     (fn [m k vs]
       (if (and (true? drop-nil-keys?)
                (nil? k))
         m
         ;; If the value is empty or nil, we drop it
         (if-let [v (if (seq more)
                      (not-empty (nested-group-by+ more vs
                                                   :leaf-fn leaf-fn
                                                   :map-fn map-fn
                                                   :drop-nil-keys?
                                                   drop-nil-keys?))
                      (and (seq vs) (leaf-fn vs)))]
           (assoc m k v)
           m)))
     (map-fn)
     (group-by (if (vector? kf)
                 #(get-in % kf)
                 kf) coll))
    coll))
