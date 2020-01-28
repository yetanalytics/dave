(ns com.yetanalytics.json-transforms
  (:require [clojure.java.io :as io]
            [clojure.datafy :as d]
            [cheshire.core :as json]
            [cheshire.factory :as fac]
            [clojure.core.async :as a])
  (:import [org.jsfr.json
            JacksonParser
            ParsingContext
            JsonPathListener
            SurfingConfiguration
            SurfingConfiguration$Builder
            JsonSurfer
            JsonSurferJackson]
           [org.jsfr.json.path
            JsonPath]
           [org.jsfr.json.compiler
            JsonPathCompiler]
           [org.jsfr.json.resolver
            DocumentResolver PoJoResolver]
           [com.fasterxml.jackson.databind.node POJONode]
           [org.jsfr.json.provider
            JsonProvider
            JavaCollectionProvider JacksonProvider]))

(set! *warn-on-reflection* true)

(def ^JsonSurfer surfer
  ;; JsonSurferJackson/INSTANCE
  (JsonSurfer. JacksonParser/INSTANCE JavaCollectionProvider/INSTANCE))

(defn compile-path*
  ^JsonPath [^String path]
  (JsonPathCompiler/compile path))

(def compile-path
  (memoize compile-path*))

(defn surf-seq
  [source ^String path]
  (iterator-seq
   (.iterator surfer
              (io/reader source)
              ^JsonPath (compile-path path))))

(defn surf-chan
  "Given a source and path, return a channel that will recieve all surfing
  results"
  [source ^String path & [out-chan]]
  (let [out-chan (or out-chan (a/chan))
        reader (io/reader source)]
    (a/thread
      (loop [xs (iterator-seq
                 (.iterator surfer
                            reader
                            ^JsonPath (compile-path path)))]
        (if-some [x (first xs)]
          (do (a/>!! out-chan x)
              (recur (rest xs)))
          (do (a/close! out-chan)
              (.close reader)))))
    out-chan))



(comment

  (def statements (io/resource "ds.json"))

  (-> statements
      (surf-seq "$[?(@.actor.name == 'Learner 4')?(@.object.id == 'https://xapinet.org/dave/activities/competency/1')]")
      #_distinct
      #_(->> (take 10))
      count )


  {"actor" {"name" "Learner 4", "mbox" "mailto:learner.4@dave.com", "objectType" "Agent"}, "stored" "2018-12-12T08:55:00.000Z", "verb" {"display" {"en-US" "started"}, "id" "https://xapinet.org/dave/verbs/started"}, "id" "c38eecfd-ff9c-43c0-91b4-d2503aeeefe3", "object" {"definition" {"name" {"en-US" "competency 1"}, "description" {"en-US" "entry level content"}, "type" "https://xapinet.org/dave/activity-type/competency"}, "id" "https://xapinet.org/dave/activities/competency/1", "objectType" "Activity"}, "timestamp" "2018-12-12T07:55:00.000Z"}
  (json/p)


  (require '[clojure.pprint :refer [pprint]])


  (-> JsonPathListener
      d/datafy
      clojure.pprint/pprint)
  (time (a/<!! (a/into [] (surf-chan statements "$.*.id"))))
  #_(def ^SurfingConfiguration config
      (-> surfer
          .configBuilder
          (.bind "$[0]"
                 ^"[Lorg.jsfr.json.JsonPathListener;"
                 (into-array JsonPathListener
                             [(reify JsonPathListener
                                (^void onValue [_ ^Object v ^ParsingContext ctx]
                                 (println v #_(d/datafy v))))]))
          .build))
  )
