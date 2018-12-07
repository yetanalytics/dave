(ns com.yetanalytics.dave.func.util
  (:require [clojure.spec.alpha :as s :include-macros true]
            [xapi-schema.spec :as xs]
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
