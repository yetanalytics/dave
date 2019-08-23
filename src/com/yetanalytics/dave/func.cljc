(ns com.yetanalytics.dave.func
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as sgen]
            [xapi-schema.spec :as xs]
            [com.yetanalytics.dave.func.ret :as ret]
            [com.yetanalytics.dave.func.common :as common]
            [com.yetanalytics.dave.func.util :as util]
            [com.yetanalytics.dave.util.spec :as su]
            [clojure.walk :as w]
            [#?(:clj clj-time.core
                :cljs cljs-time.core) :as t]
            [#?(:clj clj-time.coerce
                :cljs cljs-time.coerce) :as tc]
            [#?(:clj clj-time.periodic
                :cljs cljs-time.periodic) :as tp]
            [#?(:clj clj-time.format
                :cljs cljs-time.format) :as tf]
            #?@(:cljs [[goog.string :refer [format]]
                       [goog.string.format]])))

(defprotocol AFunc
  "Dave Funcs specify reducible xAPI caluclations."
  (init [this]
    "Set/reset to initial state.")
  (relevant? [this statement]
    "Should return true if the statement is valid basis data for the func.
     Otherwise, returns false.")
  (accept? [this statement]
    "If the func can consider this statement given its current state/data,
     returns true. Otherwise, returns false.")
  (step [this statement]
    "Given a novel statement, update state. Returns the (possibly modified) record.")
  (result [this] [this args]
    "Output the result data given the current state of the function."))

(defn ->invocable
  "Make a func invocable as a clj(s) function.
  Attaches the last state of the func as meta key ::func"
  [func]
  (reify
    #?(:clj clojure.lang.IFn :cljs IFn)
    (#?(:clj invoke
        :cljs -invoke) [_ statements]
      (let [func' (reduce step func
                          (filter (partial relevant? func)
                                  statements))]
        (vary-meta
         (result func')
         assoc ::func func')))
    (#?(:clj invoke
        :cljs -invoke) [_ statements args]
      (let [func' (reduce step func
                          (filter (partial relevant? func)
                                  statements))]
        (vary-meta
         (result func' args)
         assoc ::func func')))
    #?(:clj (applyTo [this [statements ?args]]
                     (if ?args
                       (this statements ?args)
                       (this statements))))))

(s/fdef success-timeline
  :args (s/cat
         :statements
         (s/every
          (s/with-gen ::xs/lrs-statement
            (fn []
              (sgen/fmap (fn [[id stamp verb-id [act-id act-name] [s-raw
                                                                   s-min
                                                                   s-max] success?]]
                           {"id" id
                            "actor" {"objectType" "Agent"
                                     "mbox" "mailto:xapi@example.com"}
                            "verb" {"id" verb-id}
                            "object" {"objectType" "Activity"
                                      "id" act-id
                                      "definition" {"name" {"en-US" act-name}}}
                            "timestamp" stamp
                            "stored" stamp
                            "authority" {"objectType" "Agent"
                                         "account" {"homePage" "https://example.com"
                                                    "name" "username"}}
                            "version" "1.0.3"
                            "result" {"score" {"raw" s-raw
                                               "min" s-min
                                               "max" s-max}
                                      "success" (if (= verb-id
                                                       "http://adlnet.gov/expapi/verbs/passed")
                                                  true
                                                  success?)}})
                         (sgen/tuple
                          ;; id
                          (sgen/fmap str (sgen/uuid))
                          ;; timestamp/stored
                          (sgen/fmap (fn [i]
                                       (util/inst->timestamp
                                        #?(:clj (java.util.Date. i)
                                           :cljs (js/Date. i))))
                                     ;; one year span
                                     (sgen/large-integer*
                                      {:min 1546300800000
                                       :max 1577836800000}))
                          ;; verb id
                          (sgen/elements ["http://adlnet.gov/expapi/verbs/passed"
                                          "https://w3id.org/xapi/dod-isd/verbs/answered"
                                          "http://adlnet.gov/expapi/verbs/completed"])
                          ;; activity id/name tuple
                          (sgen/fmap (fn [x]
                                       [(str "https://example.com/activities/" x)
                                        (str "Activity " x)])
                                     (sgen/elements ["a" "b" "c"]))
                          ;; score
                          (su/raw-min-max-gen)
                          #_(sgen/fmap
                           w/stringify-keys
                           (s/gen (s/keys :req-un
                                          [:score/min
                                           :score/max
                                           :score/raw])))
                          ;; success?
                          (sgen/boolean)
                          ))))))
  :ret ::ret/result)

(defrecord SuccessTimeline [state]
  AFunc
  (init [this]
    (assoc-in this [:state :successes] (sorted-map)))
  (relevant? [_ {{:strs [id]} "verb"
                 {success "success"
                  {score-min "min"
                   score-max "max"
                   score-raw "raw"} "score"} "result"}]
    (and (contains? #{"http://adlnet.gov/expapi/verbs/passed"
                      "https://w3id.org/xapi/dod-isd/verbs/answered"
                      "http://adlnet.gov/expapi/verbs/completed"}
                    id)
         (true? success)
         (and score-min score-max score-raw)))
  (accept? [_ statement]
    ;; there are no such cases for this fn
    true)
  (step [this {timestamp "timestamp"
                {actor-name "name"
                 actor-mbox "mbox"
                 actor-mbox-s1s "mbox_sha1sum"
                 actor-openid "openid"
                 {aa-name "name"
                  aa-home-page "homePage"
                  :as actor-account} "account"} "actor"
                {:strs [id]} "verb"
                {success "success"
                 {score-min "min"
                  score-max "max"
                  score-raw "raw"} "score"} "result"}]
    (let [unix-stamp (.getTime (util/timestamp->inst timestamp))]
      (assoc-in this
                [:state :successes unix-stamp]
                {:x unix-stamp
                 :y (common/scale score-raw score-min score-max)
                 :c (or actor-name
                        actor-mbox
                        actor-mbox-s1s
                        actor-openid
                        aa-name
                        "unidentified")})))
  (result [_]
    {:specification
     {:x {:type :time
          :label "Timestamp"}
      :y {:type :decimal
          :label "Score"
          :domain [0.0 100.0]}}
     :values (into [] (vals (:successes state)))})
  (result [this _]
    (result this)))

(def success-timeline
  (->invocable
   (init
    (map->SuccessTimeline {}))))

(s/fdef difficult-questions
  :args (s/cat
         :statements
         (s/every
          (s/with-gen ::xs/lrs-statement
            (fn []
              (sgen/fmap (fn [[id stamp [act-id act-name] success?]]
                           {"id" id
                            "actor" {"objectType" "Agent"
                                     "mbox" "mailto:xapi@example.com"}
                            "verb" {"id" "https://w3id.org/xapi/dod-isd/verbs/answered"}
                            "object" {"objectType" "Activity"
                                      "id" act-id
                                      "definition" {"name" {"en-US" act-name}
                                                    "type" "http://adlnet.gov/expapi/activities/cmi.interaction"}}
                            "timestamp" stamp
                            "stored" stamp
                            "authority" {"objectType" "Agent"
                                         "account" {"homePage" "https://example.com"
                                                    "name" "username"}}
                            "version" "1.0.3"
                            "result" {"success" success?}})
                         (sgen/tuple
                          ;; id
                          (sgen/fmap str (sgen/uuid))
                          ;; timestamp/stored
                          (sgen/fmap (fn [i]
                                       (util/inst->timestamp
                                        #?(:clj (java.util.Date. i)
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
                          ;; success?
                          (sgen/boolean)
                          ))))))
  :ret ::ret/result)

(defrecord DifficultQuestions [state]
  AFunc
  (init [this]
    (assoc-in this [:state :failures] {}))
  (relevant? [_ {{o-type "objectType"
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
  (accept? [_ statement]
    ;; there are no such cases for this fn
    true)
  (step [this {{id "id"
                 {name-lmap "name"} "definition"} "object"}]
    (update-in this
               [:state :failures
                [id (get name-lmap
                         "en-US"
                         (-> name-lmap
                             (->> (sort-by key))
                             first
                             val))]]
               (fnil inc 0)))
  (result [_]
    {:specification
     {:x {:type :category
          :label "Activity"}
      :y {:type :count
          :label "Success Count"}}
     :values (into []
                   (for [[[activity-id
                           activity-name]
                          s-count]
                         (:failures state)]
                     {:x (or activity-name activity-id)
                      :y s-count}))})
  (result [this _]
    (result this)))

(def difficult-questions
  (->invocable
   (init
    (map->DifficultQuestions {}))))

(s/def ::time-unit
  #{:second
    :minute
    :hour
    :day
    :week
    :month
    :year})

(s/def :completion-rate/args
  (s/keys :req-un [::time-unit]))

(s/fdef completion-rate
  :args (s/cat
         :statements
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
                                       (util/inst->timestamp
                                        #?(:clj (java.util.Date. i)
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
         :args (s/? (s/nilable :completion-rate/args)))
  :ret ::ret/result)

(defrecord CompletionRate [state]
  AFunc
  (init [this]
    (assoc-in this [:state :completions] {}))
  (relevant? [_ {{v-id "id"} "verb"
                 {o-type "objectType"} "object"
                 {completion "completion"} "result"}]
    (and
     ;; An activity
     (contains? #{"Activity" nil}
                o-type)
     (or (contains? #{"http://adlnet.gov/expapi/verbs/passed"
                      "https://w3id.org/xapi/dod-isd/verbs/answered"
                      "http://adlnet.gov/expapi/verbs/completed"}
                    v-id)
         (true? completion))))
  (accept? [_ statement]
    ;; there are no such cases for this fn
    true)
  (step [this {{id "id"
                 {name-lmap "name"} "definition"} "object"
                timestamp "timestamp"}]
    (update-in this
               [:state :completions
                [id (get name-lmap
                         "en-US"
                         (-> name-lmap
                             (->> (sort-by key))
                             first
                             val))]]
               (fn [m]
                 (-> (or m {})
                     (update :domain util/update-domain timestamp)
                     (update :statement-count (fnil inc 0))))))
  (result [this]
    (result this {:time-unit :day}))
  (result [this {:keys [time-unit]
                 :or {time-unit :day}}]
    (let [{:keys [completions]} state]
      {:specification {:x {:type :category
                           :label "Activity"}
                       :y {:type :decimal
                           :label (format "Completions per %s" (name time-unit))}}
       :values
       (into []
             (for [[[activity-id activity-name]
                    {[s e] :domain
                     :keys [statement-count]}] completions
                   :when (< 1 statement-count)
                   :let [range-secs (quot
                                     (- (tc/to-long e)
                                        (tc/to-long s))
                                     1000)]

                   :when (< 0 range-secs)

                   :let [units (/ range-secs
                                  (case time-unit
                                    :second 1
                                    :minute 60
                                    :hour 3600
                                    :day 86400
                                    :week 604800
                                    :month 2592000
                                    :year 31536000))
                         rate (if (not= 0 units)
                                (double (/ statement-count
                                           units))
                                0.0)]]
               {:x (or activity-name
                       activity-id)
                :y rate}))})))

(def completion-rate
  (->invocable
   (init
    (map->CompletionRate {}))))


(s/def :followed-recommendations/args
  (s/keys :req-un [::time-unit]))

(s/fdef followed-recommendations
  :args (s/cat
         :statements
         (s/every
          (s/with-gen ::xs/lrs-statement
            (fn []
              (sgen/fmap (fn [[id stamp [act-id act-name] vtype ref-id]]
                           (cond-> {"id" id
                                    "actor" {"objectType" "Agent"
                                             "mbox" "mailto:xapi@example.com"}
                                    "verb" {"id" (if (= vtype :recommended)
                                                   "https://w3id.org/xapi/dod-isd/verbs/recommended"
                                                   "http://adlnet.gov/expapi/verbs/launched")}
                                    "object" {"objectType" "Activity"
                                              "id" act-id
                                              "definition" {"name" {"en-US" act-name}}}
                                    "timestamp" stamp
                                    "stored" stamp
                                    "authority" {"objectType" "Agent"
                                                 "account" {"homePage" "https://example.com"
                                                            "name" "username"}}
                                    "version" "1.0.3"}
                             (= vtype
                                :followed)
                             (assoc "context" {"statement" ref-id})))
                         (sgen/tuple
                          ;; id
                          (sgen/fmap str (sgen/uuid))
                          ;; timestamp/stored
                          (sgen/fmap (fn [i]
                                       (util/inst->timestamp
                                        #?(:clj (java.util.Date. i)
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
                          ;; vtype
                          (sgen/elements [:recommended :launched :followed])

                          ;; ref-id
                          (sgen/fmap str (sgen/uuid)))))))
         :args (s/? (s/nilable :followed-recommendations/args)))

  :ret ::ret/result)

(defrecord FollowedRecommendations [state]
  AFunc
  (init [this]
    (assoc-in this [:state :statements] (sorted-map)))
  (relevant? [_ {{v-id "id"} "verb"}]
    (contains? #{"https://w3id.org/xapi/dod-isd/verbs/recommended"
                 "http://adlnet.gov/expapi/verbs/launched"}
               v-id))
  (accept? [_ statement]
    ;; there are no such cases for this fn
    true)
  (step [this {id "id"
                timestamp "timestamp"
                {v-id "id"} "verb"
                {?sr "statement"} "context"}]
    (assoc-in this
              [:state :statements
               [(tc/to-date timestamp) id]]
              (case v-id
                "http://adlnet.gov/expapi/verbs/launched"
                (if ?sr
                  :followed
                  :launched)
                "https://w3id.org/xapi/dod-isd/verbs/recommended"
                :recommended)))
  (result [this]
    (result this {:time-unit :day}))
  (result [this args]
    (let [{:keys [time-unit]
           :or {time-unit :month}} args
          {:keys [statements]} state
          p-start (some-> statements
                          first
                          ffirst)
          p-end (some-> statements
                        last
                        ffirst)]
      {:specification
       {:x {:label "Period"}
        :y {:type :count
            :label "Statement Count"}
        :c {:type :category}}
       :values
       (into []
             (when (and p-start
                        p-end
                        (not= p-start p-end))
               (let [period-like (case time-unit
                                   :second (t/seconds 1)
                                   :minute (t/minutes 1)
                                   :hour   (t/hours 1)
                                   :day    (t/days 1)
                                   :week   (t/weeks 1)
                                   :month  (t/months 1)
                                   :year   (t/years 1))]
                 (:acc
                  (reduce
                   (fn [{:keys [ss] :as m} ps]
                     (let [pe (t/plus ps period-like)
                           within? (partial t/within?
                                            ps
                                            pe)
                           [p-ss rest-ss] (split-with
                                           (comp within? tc/to-date-time first)
                                           ss)
                           period-label (str (util/format-time-unit
                                              ps
                                              time-unit)
                                             " - "
                                             (util/format-time-unit
                                              pe
                                              time-unit))
                           {:keys [launched
                                   recommended
                                   followed]} (merge {:launched 0
                                                      :recommended 0
                                                      :followed 0}
                                                     (frequencies
                                                      (map second
                                                           p-ss)))]
                       (-> m
                           (assoc :ss rest-ss)
                           (update :acc
                                   conj
                                   {:x period-label
                                    :y launched
                                    :c "Launched"}
                                   {:x period-label
                                    :y recommended
                                    :c "Recommended"}
                                   {:x period-label
                                    :y followed
                                    :c "Followed"})))
                     )
                   {:ss (map
                         (fn [[[ts _] kind]]
                           [ts kind])
                         statements)
                    :acc []}
                   (tp/periodic-seq (tc/to-date-time p-start)
                                    (tc/to-date-time p-end)
                                    period-like))))))})))

(def followed-recommendations
  (->invocable
   (init
    (map->FollowedRecommendations {}))))

(s/def :learning-path/actor-ifi
  (s/alt :agent (s/or :mbox         :agent/mbox
                      :account      :agent/account
                      :mbox-sha1sum :agent/mbox_sha1sum
                      :open-id      :agent/openid)
         :group (s/or :mbox         :group/mbox
                      :account      :group/account
                      :mbox-sha1sum :group/mbox_sha1sum
                      :open-id      :group/openid)))

(s/def :learning-path/args
  (s/keys :req-un [::time-unit]
          :opt-un [:learning-path/actor-ifi]))

(s/fdef learning-path
  :args (s/cat
         :statements
         (s/every
          (s/with-gen ::xs/lrs-statement
            (fn []
              (sgen/fmap (fn [[id agent verb stamp [act-id act-name]]]
                           {"id" id
                            ;; TODO: or group
                            "actor" agent
                            "verb" verb
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
                          ;; agent creation
                          (sgen/fmap
                           (fn [n]
                             {"name" (format "Agent %d" n)
                              "mbox" (format "mailto:agent%d@example.com" n)
                              "objectType" "Agent"})
                           (sgen/elements [1 2 3]))
                          ;; verb
                          (sgen/fmap (fn [[id label]]
                                       {"id" id
                                        "display" {"en-US" label}})
                                     (sgen/elements {"http://adlnet.gov/expapi/verbs/passed"           "passed"
                                                     "http://adlnet.gov/expapi/verbs/completed"        "completed"
                                                     "https://w3id.org/xapi/dod-isd/verbs/answered"    "answered"
                                                     "https://w3id.org/xapi/dod-isd/verbs/recommended" "recommended"
                                                     "http://adlnet.gov/expapi/verbs/launched"         "launched"}))
                          ;; timestamp/stored
                          (sgen/fmap (fn [i]
                                       (util/inst->timestamp
                                        #?(:clj (java.util.Date. i)
                                           :cljs (js/Date. i))))
                                     ;; one year span
                                     (sgen/large-integer*
                                      {:min 1546300800000
                                       :max 1577836800000}))
                          ;; activity id/name tuple
                          (sgen/fmap (fn [x]
                                       [(str "https://example.com/activities/" x)
                                        (str "Activity " x)])
                                     (sgen/elements ["a" "b" "c" "d" "e" "f"])))))))
         :args (s/? (s/nilable :learning-path/args)))
  :ret ::ret/result)

(defrecord LearningPath [state]
  AFunc
  (init [this]
    ;; set/reset to init state
    (assoc-in this [:state :learners] {}))
  (relevant? [_ statement]
    ;; "Should return true if the statement is valid basis data for the func. Otherwise, returns false."
    ;; -  Not going to support statements with substatements or statement references at this time
    (let [{:strs [object]} statement
          object-type (common/get-helper object "objectType")]
      (case object-type
        "SubStatement" false
        "StatementRef" false
        true)))
  (accept? [_ statement]
    ;; "If the func can consider this statement given its current state/data, returns true. Otherwise, returns false."
    (if (-> statement
            (common/get-helper "actor")
            common/parse-actor
            common/handle-actor
            not-empty)
      ;; we have usable identity vs don't have it
      true
      false))
  (step [this statement]
    ;; "Given a novel statement, update state. Returns the (possibly modified) record."
    (let [{:keys [timestamp id actor verb object]} (common/parse-statement-simple statement)
          {:keys [verb-id verb-name]}               verb
          relevant-actor-info                       (common/handle-actor actor)
          many-actor-agents?                        (-> relevant-actor-info first vector?)
          [obj-name id-or-members]                  (common/handle-object object)
          obj-member-names                          (when (vector? id-or-members)
                                                      (apply str (interpose "," (map first id-or-members))))
          object-display                            (or obj-member-names obj-name)]
      (if many-actor-agents?
        (do
          (doseq [[member-name member-ifi] relevant-actor-info]
            ;; update `this` for each member of the group using `data`
            (let [data (vector timestamp id member-name verb-name object-display)]
              (update-in
               this
               [:state :learners member-ifi]
               (fn [old new-data]
                 ;; ensure vector if no data has been recorded for the member given their ifi
                 (let [accum (or (not-empty old) [])]
                   (conj accum new-data)))
               data)))
          ;; make sure `this` is returned
          this)
        (let [[actor-name actor-ifi] relevant-actor-info
              data                   (vector timestamp id actor-name verb-name object-display)]
          (update-in
           this
           [:state :learners actor-ifi]
           (fn [old new-data]
             ;; ensure vector if no data has been recorded for the actor given their ifi
             (let [accum (or (not-empty old) [])]
               (conj accum new-data)))
           data)))))
  (result [this]
    ;; "Output the result data given the current state of the function."
    (let [data-per-actor (get-in this [:state :learners])
          [_ ifi]        (last ;; highest n of statements
                          (sort-by first (reduce-kv (fn [accum a-ifi a-data]
                                                      (conj accum [(count a-data) a-ifi]))
                                                    [] data-per-actor)))]
      ;; set actor IFI based on who has the most data
      (result this {:time-unit :hour
                    :actor-ifi ifi})))
  (result [this args]
    (let [data-per-actor         (get-in this [:state :learners])
          time-unit              (:time-unit args :hour) ;; set default to catch `args` being nil
          ;; TODO: This definitely shouldn't just pick an actor.
          ifi                    (or (not-empty (:actor-ifi args))
                                     ;; if `actor-ifi` was not set by args
                                     ;; - result should only return data for a single actor
                                     (let [[_ the-ifi]
                                           (last ;; highest n of statements
                                            (sort-by first
                                                     (reduce-kv
                                                      (fn [accum a-ifi a-data]
                                                        (conj accum [(count a-data) a-ifi]))
                                                      [] data-per-actor)))]
                                       the-ifi))
          actors-data            (get data-per-actor ifi)
          ;; actors-data looks like:
          ;; [
          ;; [timestamp-i id-i actor-name verb-name-i object-display-i]
          ;; ...
          ;; [timestamp-j id-j actor-name verb-name-j object-display-j]
          ;;  ]
          chronological          (sort-by first actors-data)
          [p-start]              (first chronological)
          [p-end]                (last chronological)
          values                 (or (when (and p-start
                                                p-end
                                                (not= p-start p-end))
                                       (let [time-unit-like (case time-unit
                                                              :second (t/seconds 1)
                                                              :minute (t/minutes 1)
                                                              :hour   (t/hours 1)
                                                              :day    (t/days 1)
                                                              :week   (t/weeks 1)
                                                              :month  (t/months 1)
                                                              :year   (t/years 1))
                                             pseq (tp/periodic-seq (t/minus (tc/to-date-time p-start)
                                                                            time-unit-like)
                                                                   (t/plus (tc/to-date-time p-end)
                                                                           time-unit-like)
                                                                   time-unit-like)
                                             [_ buckets] (reduce
                                                          (fn [[d ack] start]
                                                            (let [end (t/plus start time-unit-like)
                                                                  interval (t/interval start end)
                                                                  [in-period rest-d] (split-with
                                                                                      (comp
                                                                                       (partial t/within? interval)
                                                                                       tc/to-date-time
                                                                                       #(first %))
                                                                                      d)]
                                                              (if (empty? in-period)
                                                                [rest-d ack]
                                                                ;; parse cordinates from `chronological`
                                                                (let [cords (mapv
                                                                             (fn [[unix-ts stmt-id actr-name vrb-name obj-disply]]
                                                                               {:x unix-ts
                                                                                :y vrb-name
                                                                                :c obj-disply})
                                                                             in-period)]
                                                                  ;; add them to the accumulator
                                                                  [rest-d (conj ack
                                                                                {:period-start (tf/unparse
                                                                                                (tf/formatters :date-time)
                                                                                                start)
                                                                                 :period-end   (tf/unparse
                                                                                                (tf/formatters :date-time)
                                                                                                end)
                                                                                 :cords cords})]))))
                                                          [chronological []]
                                                          pseq)]
                                         buckets))
                                     [])]
      ;; `values` also contains `:period-start` and `:period-end`
      ;; - only parsing out `:cords`
      {:values (reduce into [] (mapv :cords values))})))

(def learning-path
  (->invocable
   (init
    (map->LearningPath {}))))

(s/def ::function
  record?)

(s/def ::fspec
  (s/with-gen s/spec?
    (fn []
      (sgen/return (s/get-spec `success-timeline)))))

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
;; Some args are strings.
(s/def ::args-string
  (s/coll-of keyword? :kind set? :into #{}))

;; Funcs have a default question text that can be suggested to the user
(s/def ::question-text-default
  (s/and string?
         not-empty))

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
    :function (init (map->SuccessTimeline {})) ;; success-timeline
    :fspec (s/get-spec `success-timeline)
    :args-default {}
    :args-enum {}
    :question-text-default "When do learners do their best work?"}
   ::difficult-questions
   {:title "Difficult Questions"
    :doc "Plots interaction activity ids against the number of failed attempts for that activity."
    :function (init (map->DifficultQuestions {}))
    :fspec (s/get-spec `difficult-questions)
    :args-default {}
    :args-enum {}
    :question-text-default "What activities are most difficult?"}
   ::completion-rate
   {:title "Completion Rate"
    :doc "Plots activity ids against the rate of completions per given time unit."
    :function (init (map->CompletionRate {}))
    :fspec (s/get-spec `completion-rate)
    :args-default {:time-unit :day}
    :args-enum {:time-unit #{:second
                             :minute
                             :hour
                             :day
                             :week
                             :month
                             :year}}
    :question-text-default
    "What activities are completed the most?"}
   ::followed-recommendations
   {:title "Followed Recommendations"
    :doc "Buckets statements into periods (time ranges) by statement timestamp. Within each bucket, counts the number of recommendations, launches and follows expressed."
    :function (init (map->FollowedRecommendations {}))
    :fspec (s/get-spec `followed-recommendations)
    :args-default {:time-unit :month}
    :args-enum {:time-unit #{:second
                             :minute
                             :hour
                             :day
                             :week
                             :month
                             :year}}
    :question-text-default
    "How often are recommendations followed?"}
   ::learning-path
   {:title "Learning Path"
    :doc "" ;; TODO: Will add
    :function (init (map->LearningPath {}))
    :fspec (s/get-spec `learning-path)
    :args-default {:time-unit :month}
    :args-enum {:time-unit #{:second
                             :minute
                             :hour
                             :day
                             :week
                             :month
                             :year}}
    :question-text-default
    "" ;; TODO: Will add
    }})

;; Keep a set of the default text so we know to only replace if it isn't custom
(def question-text-defaults
  (into #{}
        (map :question-text-default
             (vals registry))))

(def func-spec
  (s/with-gen
    (s/keys :req-un [::function
                     ::fspec
                     ::title
                     ::args-default
                     ::args-enum
                     ::question-text-default]
            :opt-un [::doc])
    (fn []
      (sgen/elements (vals registry)))))

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

;; TODO: deprecate!
(defn apply-func
  "Apply a DAVE function given a function key, args map (maybe nil) and a
  coll of statements"
  [id args-map statements]
  (let [{:keys [function fspec
                args-default]} (get-func id)
        ]
    (apply
     (->invocable function)
     statements
     (s/unform (:args fspec)
               (merge args-default
                      args-map)))))
