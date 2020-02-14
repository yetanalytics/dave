(ns com.yetanalytics.dave.datalog.builtins
  #?(:clj (:import [java.util Date TimeZone]
                   [java.time
                    Instant
                    OffsetDateTime]
                   [java.time.format DateTimeFormatter])))

#?(:clj (def ^DateTimeFormatter iso-fmt
          DateTimeFormatter/ISO_DATE_TIME))

#?(:clj
   (defn timestamp->inst
     ^Date [^String timestamp]
     (Date/from (.toInstant (OffsetDateTime/parse timestamp iso-fmt))))
   :cljs
   (defn timestamp->inst
     [timestamp]
     (js/Date. timestamp)))

#?(:clj (defn inst->timestamp
          [^Date inst]
          (.toString ^java.time.Instant (.toInstant inst)))
   :cljs (defn inst->timestamp
           [inst]
           (.toISOString inst)))

(defn timestamp->unix
  [stamp]
  (-> stamp
      timestamp->inst
      inst-ms))

(def builtins
  [[:timestamp->unix timestamp->unix]])
