(ns user
  (:require [clojure.repl :refer [doc source apropos]]
            [clojure.pprint :refer [pprint pp]]
            [com.yetanalytics.dave.filter-scratch.json :as j]
            [com.yetanalytics.dave.filter-scratch.json.path :as path]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.data.json :as json]
            [clojure.string :as cs])
  (:import [com.jayway.jsonpath InvalidPathException PathNotFoundException
            Option
            Configuration
            Configuration$ConfigurationBuilder
            DocumentContext
            JsonPath
            Criteria
            Filter
            Predicate
            PathNotFoundException]
           [java.time Instant]
           [com.bazaarvoice.jolt JoltTransform Transform ContextualTransform SpecDriven JsonUtils Shiftr Chainr]
           [clojure.java.api Clojure]
           [clojure.lang IFn]
           #_[com.yetanalytics.dave.filter_scratch Clojurr]))

(set! *warn-on-reflection* true)


(defn nested-group-by+
  [[kf & more :as kfs] coll & [leaf-fn]]
  (if (seq kfs)
    (reduce-kv (fn [acc k vs]
                 (assoc acc k (if (seq more)
                                (nested-group-by+ more vs leaf-fn)
                                ((or leaf-fn identity) vs))))
               {}
               (group-by kf coll))
    coll))


#_(time (nested-group-by+ [even?] (range 10)))


(defn transform
  [spec input]
  (.. Chainr
      (fromSpec spec)
      (transform input)))


(comment
  (def statements
    (with-open [r (io/reader  "../../dave/resources/public/data/dave/ds.json")]
      (doall (json/read r))))


  (-> statements
        first
        #_(assoc-in ["object" "definition" "name" "zz-ZZ"] "foo")
        (->> (transform [;; no-ops
                         {"operation" "shift",
                          "spec"
                          {"@" ""}}
                         {"operation" "shift",
                          "spec"
                          {"*" "&"}}
                         {"operation" "com.yetanalytics.dave.filter_scratch.jolt.Clojurr"
                          "spec"
                          {"ns" "clojure.core"
                           "name" "identity"
                           }}

                         ]))

        #_clojure.pprint/pprint)
  )

(comment




  (defn normalized-path->clj
    [s]
    (-> s
        (subs 1)
        (cs/escape {\' \"})
        (->> (format "[%s]"))
        read-string
        flatten
        vec))

  (defn compile-path
    ^JsonPath [^String path]
    (JsonPath/compile path ^"[Lcom.jayway.jsonpath.Predicate;" (into-array Predicate [])))

  (defn select
    [^String path json & {:keys [always-return-list
                                 suppress-exceptions
                                 as-path-list
                                 default-path-leaf-to-null
                                 require-properties]
                          :or {always-return-list true}}]
    (cond->> (.. JsonPath
                 (using
                  (.. Configuration
                      defaultConfiguration
                      (setOptions
                       (into-array Option (cond-> []
                                            always-return-list (conj Option/ALWAYS_RETURN_LIST)
                                            suppress-exceptions (conj Option/SUPPRESS_EXCEPTIONS)
                                            as-path-list        (conj Option/AS_PATH_LIST)
                                            default-path-leaf-to-null (conj Option/DEFAULT_PATH_LEAF_TO_NULL)
                                            require-properties (conj Option/REQUIRE_PROPERTIES)
                                            )))))
                 (parse json)
                 (read (compile-path path))
                 #_(read path ^"[Lcom.jayway.jsonpath.Predicate;" (into-array Predicate [])))
      as-path-list (map normalized-path->clj)))


  (defmacro kw-or-str-spec
    "Return a spec that denotes a keyword that could also be a string"
    [k]
    `(s/and (s/conformer #(get {~k ~k
                                ~(name k) ~k}
                               %
                               ::s/invalid)
                         name)
            #{~k}))

  (s/def ::filter
    (s/or :path string?
          :and
          (s/map-of (kw-or-str-spec :and)
                    (s/every ::filter
                             :kind vector?
                             :into []
                             :min-count 1)
                    :count 1)
          :or
          (s/map-of (kw-or-str-spec :or)
                    (s/every ::filter
                             :kind vector?
                             :into []
                             :min-count 1)
                    :count 1)
          :not
          (s/map-of (kw-or-str-spec :not)
                    ::filter
                    :count 1)))

  (comment
    (s/conform ::filter {"and" ["foo" "bar"]})


    )

  (defn compile-filter-pred
    [filter-json]
    (if (string? filter-json)
      (fn [json]
        (not (empty? (remove nil?
                             (select filter-json json
                                     :suppress-exceptions true)))))
      (let [{and-f :and
             or-f :or
             not-f :not} filter-json]
        (cond and-f
              (apply every-pred
                     (map compile-filter-pred
                          and-f))
              or-f
              (apply some-fn
                     (map compile-filter-pred
                          or-f))
              not-f
              (complement (compile-filter-pred not-f))))))

  (defn filter-json
    "Filter based on per-object JSONPath predicates"
    [filter-json json]
    (filter
     (compile-filter-pred filter-json)
     json))




  #_(defn filter-1
      "Just use Jayway's built-ins to filter the sequence.
  Returns a transducer if json is not provided."
      [path & rest-args]
      (apply filter
             #(not-empty (select % path :suppress-exceptions true))
             rest-args))

  (defn series
    [coll named-paths]
    (map
     (fn [json]
       (reduce-kv
        (fn [m k v]
          (assoc m k
                 (let [ret (if (coll? v)
                             (some #(select json % :always-return-list false) v)
                             (select json v :always-return-list false))]
                   (if (coll? ret)
                     (throw (ex-info "No collection as datum value!"
                                     {:type ::coll-as-series-datum-value
                                      :named-paths named-paths}))
                     ret))))
        {}
        named-paths))
     coll))

  (defn bucket
    [coll key-path & [val-path]]
    (map (fn [[k v]]
           {"key" k
            "val" v})
         (nested-group-by+ [#(select % key-path
                                     :always-return-list false)]
                           coll
                           (if val-path
                             #(select % val-path
                                      :always-return-list false)
                             identity))))
  (defn cut-stamp
    [time-unit stamp]
    (subs stamp 0 (case time-unit
                    :second 19
                    :minute 16
                    :hour 13
                    :day 10
                    :month 7
                    :year 4)))

  (defn bucket-time
    "Bucket by timestamp field, natively"
    [coll timestamp-path time-unit]
    (map (fn [[k v]]
           {"key" k
            "val" v})
         (nested-group-by+ [(comp
                             (partial cut-stamp time-unit)
                             #(select % timestamp-path
                                      :always-return-list false))]
                           coll)))

  ;; Try some jolt transforms
  #_(defn transform-from*
      ^Chainr [spec]
      (.. Chainr (fromSpec spec)))

  #_(def transform-from
      (memoize transform-from*))

  (defn transform
    [spec input]
    #_(.transform ^Chainr (transform-from spec) input)
    (.. Chainr
        (fromSpec spec)
        (transform input)))

  ;; Custom Jolt transform?
  #_(gen-class :name "com.yetanalytics.Clojurr"
               :implements [SpecDriven Transform]
               :prefix "clojurr-"
               :init "init"
               #_:methods
               #_["transform" [Object] Object]
               :state "state")

  #_(defn clojurr-init
      [^Object {:strs [^String ns ^String name args] :as spec}]
      (require (read-string ns))
      (let [v (Clojure/var ns name)]
        (assert (ifn? v)
                "Must be a clojure function implementing IFn")
        [[] {:var v
             :args (into []
                         args)}]))
  #_(declare com.yetanalytics.Clojurr)
  #_(defn clojurr-transform
      [this ^Object json]
      (let [{:keys [^IFn var args]} (.state ^com.yetanalytics.Clojurr this)]
        (apply var json args)))

  #_(deftype Clojurr [^Object spec]
      SpecDriven
      Transform
      (transform ))

  #_(let [^IFn plus (Clojure/var "clojure.core" "+")]
      (apply plus [1 2 3]))

  (comment
    (def input
      (with-open [r (io/reader (io/resource "jolt/sample/input.json"))]
        (json/read r)))

    (def spec
      (with-open [r (io/reader (io/resource "jolt/sample/spec.json"))]
        (json/read r)))

    (def output
      (with-open [r (io/reader (io/resource "jolt/sample/output.json"))]
        (json/read r)))


    (clojure.pprint/pprint spec)

    (let [chainr (Chainr/fromSpec spec)]
      (= output (.transform chainr input)))

    (time (transform [{"operation" "shift",
                       "spec"
                       {"rating"
                        {"primary" {"value" "Rating"},
                         "*"
                         {"value" "SecondaryRatings.&1.Value",
                          "$" "SecondaryRatings.&.Id"}}}}
                      {"operation" "default",
                       "spec" {"Range" 5, "SecondaryRatings" {"*" {"Range" 5}}}}]

                     {"rating" {"primary" {"value" 3}, "quality" {"value" 3}}}))




    (clojure.pprint/pprint input)
    )

  (comment

    (cut-stamp :year "2020-01-16T19:17:20.633765Z")

    (def statements
      (with-open [r (io/reader  "../../dave/resources/public/data/dave/ds.json")]
        (doall (json/read r))))

    (-> statements first (get-in []))
    (clojure.pprint/pprint
     (transform [#_{"operation" "shift",
                    "spec"
                    {"object"
                     {"id" "object.id"
                      "objectType"
                      {"Activity"
                       {"@(2,definition)"
                        {"name"
                         {"*" "object.definition.name"}
                         "description"
                         {"*" "object.definition.description"}
                         "*" "object.definition.&"}
                        "@(2,objectType)" "object.objectType"}
                       "@" "object.objectType"
                       }
                      }
                     #_{"definition"
                        {"name"
                         {"*" "object.definition.name.&4"}
                         "description"
                         {"*" "object.definition.description"}
                         "*" "object.definition.&"}
                        "*" "object.&"}
                     "verb"
                     {"display"
                      {"*" "verb.display"}
                      "*" "verb.&"}
                     "*" "&"}}
                 {"operation" "shift",
                  "spec"
                  {"*" ""}}]
                (-> statements
                    first
                    #_(assoc-in ["object" "definition" "name" "en-GB"] "cheerio")
                    #_(assoc-in ["object" "objectType"] "Foo")
                    )))

    (-> statements
        first
        (assoc-in ["object" "definition" "name" "zz-ZZ"] "foo")
        (->> (transform [;; no-ops
                         {"operation" "shift",
                          "spec"
                          {"@" ""}}
                         {"operation" "shift",
                          "spec"
                          {"*" "&"}}

                         ;; [a v o]
                         #_{"operation" "shift"
                            "spec"
                            {"actor"
                             {"mbox" "[0]"
                              "mbox_sha1sum" "[0]"
                              "openid" "[0]"
                              "account" "[0]"
                              }
                             "verb"
                             {"id" "[1]"}
                             "object"
                             {"id" "[2]"}}}

                         ;; display or ID/IFI
                         #_{"operation" "shift"
                          "spec"
                          {"actor"
                           {"name" "a"
                            "mbox" "a"
                            "mbox_sha1sum" "a"
                            "openid" "a"
                            "account" "a"
                            }
                           "verb"
                           {"display"
                            {"en-US" "v"
                             "*" "v"}
                            "id" "v"}
                           "object"
                           {"definition" {"name"
                                          {"en-US" "o"
                                           "*" "o"}}
                            "id" "o"}}}
                         #_{"operation" "cardinality"
                          "spec"
                          {"*" "ONE"}}
                         #_{"operation" "shift"
                          "spec"
                          {"a" "[0]"
                           "v" "[1]"
                           "o" "[2]"}}
                         ]))

        clojure.pprint/pprint)


    ;; success timeline
    (-> statements
        (select (str
                 "$[?("
                 "(@.verb.id == 'http://adlnet.gov/expapi/verbs/passed' || @.verb.id == 'https://w3id.org/xapi/dod-isd/verbs/answered' || @.verb.id == 'http://adlnet.gov/expapi/verbs/completed')"
                 "&& @.result.success == true"
                 "&& @.result.score.scaled"
                 ")]"))
        count
        #_(series
           {:x "$.timestamp"
            :y "$.result.score.scaled"
            :z ["$.actor.name"
                "$.actor.mbox"]}))

    (-> statements
        (filter-json {:and ["$.result[?(@.success == true)]"
                            "$.result.score.scaled"
                            {:or ["$.verb[?(@.id == 'http://adlnet.gov/expapi/verbs/passed')]"
                                  "$.verb[?(@.id == 'https://w3id.org/xapi/dod-isd/verbs/answered')]"
                                  "$.verb[?(@.id == 'http://adlnet.gov/expapi/verbs/completed')]"]}]})
        count)


    (print (json/write-str {:and ["$.result[?(@.success == true)]"
                                  "$.result.score.scaled"
                                  {:or ["$.verb[?(@.id == 'http://adlnet.gov/expapi/verbs/passed')]"
                                        "$.verb[?(@.id == 'https://w3id.org/xapi/dod-isd/verbs/answered')]"
                                        "$.verb[?(@.id == 'http://adlnet.gov/expapi/verbs/completed')]"]}]}))
    ;; difficult questions
    (-> statements
        (select (str
                 "$[?("
                 "(@.object.objectType == 'Activity' || !(@.object.objectType))"
                 "&& (@.object.definition.type == 'http://adlnet.gov/expapi/activities/cmi.interaction')"
                 "&& (@.result.success == false)"
                 ")]"))

        (bucket
         "$.object.definition.name.en-US"
         #_"$.length()")
        (series
         {:x "$.key"
          :y "$.val.length()"}))

    ;; completion rate ;; WRONG
    (-> statements
        (select (str
                 "$[?("
                 "(@.object.objectType == 'Activity' || !(@.object.objectType))"
                 "&& (@.result.completion == true || (@.verb.id == 'http://adlnet.gov/expapi/verbs/passed' || @.verb.id == 'https://w3id.org/xapi/dod-isd/verbs/answered' || @.verb.id == 'http://adlnet.gov/expapi/verbs/completed'))"
                 ")]"))
        (bucket-time
         "$.timestamp"
         :day)
        (series
         {:x "$.key"
          :y "$.val.length()"})
        )


    (into #{} (select statements "$.*.verb.id"))







    (let [statements (with-open [r (io/reader (io/resource "statements/kokea.json"))]
                       (json/read r))
          successful (filter-1
                      "$.result[?(@.success == true)]"
                      statements)
          score-gt-zero (filter-1
                         "$.result[?(@.score.scaled > 0.0)]"
                         statements)
          successful-and-score-gt-zero (filter-1
                                        "$.result[?(@.success == true && @.score.scaled > 0.0)]"
                                        statements)]
      (= (set successful-and-score-gt-zero)
         (clojure.set/intersection (set successful) (set score-gt-zero))))


    (count (filter-1
            "$.result[?(@.success == true)]"
            kokea))

    (count (select kokea "$.*.result[?(@.success == true)]"))

    (count (select kokea "$.*.result[?(@.score.scaled > 0.0)]"))

    (count (select kokea "$.*.result[?(@.success == true && @.score.scaled > 0.0)]"))

    (select kokea "$.*.result.score.scaled.min()")
    (filter #(select ))

    (select kokea "$[0].foo")


    )
  )
