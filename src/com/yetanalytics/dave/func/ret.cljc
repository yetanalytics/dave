(ns com.yetanalytics.dave.func.ret
  (:require [clojure.spec.alpha :as s]
            [xapi-schema.spec :as xs]))

;; New, generalized returns
(s/def :datum/x
  (s/or :string string?
        :number number?))

(s/def :datum/y
  (s/or :string string?
        :number number?))

(s/def :datum/c
  string?)

(s/def ::datum
  (s/keys :req-un [:datum/x]
          :opt-un [:datum/y
                   :datum/c]))

(s/def ::values
  (s/every ::datum))

(s/def ::domain
  (s/tuple number? number?))

(s/def ::label string?)

(s/def ::format string?)

(s/def ::type
  ;; Used to help vis choose if/how to display
  ;; shows intent
  #{:decimal ;; show places
    :count ;; show as int
    :category ;; use to group, etc outside of normal :c
    :time ;; let vis know it is timestamps
    })

(s/def ::specification
  (s/map-of #{:x :y :c}
            (s/keys :opt-un [::type
                             ::domain
                             ::label
                             ::format])))

(s/def ::result
  (s/keys :req-un [::values]
          :opt-un [::specification]))

(comment

  ;; Scalars
  (s/def ::score-scaled-0-100
    (s/double-in
     :min 0.0
     :max 100.0))

  (s/def ::count
    (s/int-in 0 #?(:clj java.lang.Integer/MAX_VALUE
                   :cljs js/Number.MAX_VALUE)))

  (s/def ::rate
    (s/double-in :min 0.0
                 :infinite? false
                 :NaN? false))

  (s/def ::category ;; label
    (s/and string?
           not-empty))

  ;; Collections

  ;; tuples of [timestamp score[0.0-100.0]]
  (s/def ::time-score
    (s/every
     (s/tuple ::xs/timestamp
              ::score-scaled-0-100)))

  ;; tuples of [string non-neg-int]
  (s/def ::category-count
    (s/every
     (s/tuple ::category
              ::count)))

  ;; tuples of [string non-nec-dec]
  (s/def ::category-rate
    (s/every
     (s/tuple ::category
              ::rate)))

  ;; Time bucketed data w/n labeled y values
  (s/def ::time-bucket-category-counts
    (s/every
     (s/tuple ::xs/timestamp ;; start
              ::xs/timestamp ;; end
              (s/map-of ::category
                        ::count))))
  )
