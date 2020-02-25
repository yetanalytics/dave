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

(def transform-fn #?(:cljs {:abs js/Math.abs :acos js/Math.acos
                            :acosh js/Math.acosh :asin js/Math.asin
                            :asinh js/Math.asinh :atan js/Math.atan :atan2 js/Math.atan2
                            :atanh js/Math.atanh :cbrt js/Math.cbrt :ceil js/Math.ceil
                            :clz32 js/Math.clz32 :cos js/Math.cos :cosh js/Math.cosh
                            :e js/Math.e :exp js/Math.exp :expm1 js/Math.expm1
                            :floor js/Math.floor :fround js/Math.fround
                            :hypot js/Math.hypot :imul js/Math.imul :ln10 js/Math.ln10
                            :ln2 js/Math.ln2 :log js/Math.log :log10 js/Math.log10
                            :log10e js/Math.log10e :log1p js/Math.log1p
                            :log2 js/Math.log2 :log2e js/Math.log2e :max js/Math.max
                            :min js/Math.min :pi js/Math.pi :pow js/Math.pow
                            :random js/Math.random :round js/Math.round
                            :set-e! js/Math.set-e! :set-ln10! js/Math.set-ln10!
                            :set-ln2! js/Math.set-ln2! :set-log10e! js/Math.set-log10e!
                            :set-log2e! js/Math.set-log2e! :set-pi! js/Math.set-pi!
                            :set-sqrt12! js/Math.set-sqrt12!
                            :set-sqrt2! js/Math.set-sqrt2! :sign js/Math.sign
                            :sin js/Math.sin :sinh js/Math.sinh :sqrt js/Math.sqrt
                            :sqrt12 js/Math.sqrt12 :sqrt2 js/Math.sqrt2 :tan js/Math.tan
                            :tanh js/Math.tanh :to-source js/Math.to-source
                            :trunc js/Math.trunc}
                     :clj {}))

(defn math-transform
  [transform & args]
  (apply (get transform-fn transform) args))


(def builtins
  [[:timestamp->unix timestamp->unix]
  [:math-transform math-transform]])
