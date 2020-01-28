(ns com.yetanalytics.dave.transform
  "Declarative data (filtering and) transformations for DAVE"
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as sgen]
            [xapi-schema.spec :as xs]
            [clojure.set :as cset]))


;; TYPES

;; spec for any JSON

;; scalar
(s/def :json/string
  string?)

(s/def :json/null
  nil?)

(s/def :json/number
  (s/or :double
        (s/double-in :infinite? false :NaN? false
                     ;; :max 1000.0 :min -1000.0
                     )
        :int
        int?))

(s/def :json/boolean
  boolean?)

(s/def :json/scalar
  (s/or :string :json/string
        :number :json/number
        :boolean :json/boolean
        :null :json/null))

;; coll
(s/def :json.object/key
  :json/string)

(s/def :json/object
  (s/map-of :json.object/key
            :json/any))

(s/def :json/array
  (s/every :json/any
           :kind vector?
           :into []
           :gen-max 4))

(s/def :json/coll
  (s/or :object :json/object
        :array :json/array))

(s/def :json/any
  (s/or :scalar :json/scalar
        :coll :json/coll))

;; Complex Types:

;; Let's explicitly talk about a sequence of objects
(s/def ::object-seq
  (s/every :json/object))

;; and a single object
(s/def ::object
  :json/object)

(s/def ::input
  (s/with-gen (s/and ::object-seq
                     (s/every ::xs/statement))
    (fn []
      (s/gen (s/every ::xs/statement)))))

(derive ::input ::object-seq)

(s/def ::output
  ::object-seq)

(derive ::output ::object-seq)


;; So, a function spec representing all transform ops
(def all-ops-fspec
  (s/fspec :args (s/cat :input ::input)
           :ret ::output))

;; Some archetypal function specs

;; Filters return a subset
(def filter-fspec
  (s/fspec :args (s/cat :object-seq ::object-seq)
           :ret ::object-seq
           ;; Input is a superset of output
           :fn (fn [{{obj-seq :object-seq} :args
                     :keys [ret]}]
                 (cset/superset? (set obj-seq)
                                 (set ret)))))

;; seq-to-seq

(def map-transform-fspec
  (s/fspec :args (s/cat :object-seq ::object-seq)
           :ret ::object-seq
           ;; no further constraints
           ))

;; seq-to-map aggregates
(def aggregate-bucket-fspec
  (s/fspec :args (s/cat :object-seq ::object-seq)
           :ret ::object))

(def aggregate-decorate-fspec
  (s/fspec :args (s/cat :object ::object)
           :ret ::object))

(def aggregate-explode-fspec
  (s/fspec :args (s/cat :object ::object)
           :ret ::object-seq))



(comment
  ;; spec for any JSON

  ;; scalar
  (s/def :json/string
    string?)

  (s/def :json/null
    nil?)

  (s/def :json/number
    (s/or :double
          (s/double-in :infinite? false :NaN? false
                       :max 1000.0 :min -1000.0)
          :int
          int?))

  (s/def :json/boolean
    boolean?)

  (derive :json/string :json/scalar)
  (derive :json/null :json/scalar)
  (derive :json/number :json/scalar)
  (derive :json/boolean :json/scalar)

  (s/def :json/scalar
    (s/or :string :json/string
          :number :json/number
          :boolean :json/boolean
          :null :json/null))

  ;; coll
  (s/def :json.object/key
    :json/string)

  (s/def :json/object
    (s/map-of :json.object/key
              :json/any))

  (s/def :json/array
    (s/every :json/any
             :kind vector?
             :into []
             :gen-max 4))

  (derive :json.object/key :json/string)
  (derive :json/object :json/coll)
  (derive :json/array :json/coll)

  (s/def :json/coll
    (s/or :object :json/object
          :array :json/array))


  (derive :json/scalar :json/any)
  (derive :json/coll :json/any)

  (s/def :json/any
    (s/or :scalar :json/scalar
          :coll :json/coll))

  ;; input to filters + transform
  (s/def ::input
    (s/every ::xs/statement))

  ;; that's also a JSON array
  (derive ::input :json/array)

  ;; output of all filters + transforms, to pass to vega
  (s/def :output/primitive-array
    (s/or :strings (s/every :json/string)
          :numbers (s/every :json/number)))

  (derive :output/primitive-array ::output)

  (s/def :output/object-array
    (s/every :json/object))

  (derive :output/object-array ::output)

  (s/def ::output
    (s/or :primitive-array :output/primitive-array
          :object-array :output/object-array))

  ;; And output should be too
  (derive ::output :json/array)

  ;; operation specs, placeholder-y for now
  (s/def :op/name
    string?)

  ;; params the op needs
  (s/def :op/spec
    :json/object)

  ;; Ops can accept and return any of the valid types, but they must be used
  ;; in the correct order. Thus, let's think about a couple of types of ops, and
  ;; then look at their input/output

  ;; Statement filters, which must come first, accept and return statements
  ;; Which we have bound here as ::input btw
  #_(s/def :op.statement-filter/input
      #{::input})

  #_(s/def :op.statement-filter/output
      #{::input})

  (s/def :op/input
    ;; qualified keyword representing a spec
    (descendants :json/any))

  (s/def :op/output
    (descendants :json/any))

  (defmulti classify-op
    (juxt :input :output))

  (s/def ::op
    (s/keys :req-un [:op/name
                     :op/spec
                     :op/input
                     :op/output]))

  (defmethod classify-op [::input ::input]
    [_] ::statement-filter)

  (defmethod classify-op [:json/any :json/any]
    [_] ::intermediate-transform)

  (defmethod classify-op [:json/any ::output]
    [_] ::final-transform)


  (s/def ::op-filter
    (s/and ::op
           #(= ::statement-filter
               (classify-op %))))

  (s/def ::op-intermediate
    (s/and ::op
           #(= ::intermediate-transform
               (classify-op %))))

  (s/def ::op-final
    (s/and ::op
           #(= ::final-transform
               (classify-op %))))

  ;; Express constraints for a chain of ops

  (s/def ::ops
    (s/cat :filters (s/* ::op-filter)
           :intermediate (s/* ::op-intermediate)
           :final (s/* ::op-final)))

  (comment
    (sgen/generate (s/gen ::op))

    )

  ;; In abstract, the signature of an operation
  ;; it takes json and produces json!
  (def op-fn-fspec
    (s/fspec :args (s/cat :input :json/coll)
             :ret :json/coll))

  ;; to make an op-fn, we compile an op spec
  (def op-compile-fspec
    (s/fspec :args (s/cat :op ::op)
             :ret op-fn-fspec))

  (s/def compile-op
    op-compile-fspec)

  ;; Multiple ops together have constraints!
  #_(s/def ::ops
      )

  ;; compiling the composition of ops should yield a function that obeys our
  ;; input and output constraints
  (def all-ops-fn-fspec
    (s/fspec :args (s/cat :input ::input)
             :ret ::output))

  (def all-ops-compile-fspec
    (s/fspec :args (s/cat :ops (s/every ::op))
             :ret all-ops-fn-fspec))

  (s/def compile
    all-ops-compile-fspec)

  ;; Closing over the compilation, we can look at the total process
  (def apply-ops-fspec
    (s/fspec :args (s/cat :ops (s/every ::op)
                          :input ::input)
             :ret ::output))


  )
(comment
  ;; Filtering -- based on JSON path

  (s/def :statement-filter/spec
    (s/and :op/spec
           (s/or :path string?
                 :and
                 (s/map-of #{"and"}
                           (s/every :statement-filter/spec
                                    :kind vector?
                                    :into []
                                    :min-count 1)
                           :count 1)
                 :or
                 (s/map-of #{"or"}
                           (s/every :statement-filter/spec
                                    :kind vector?
                                    :into []
                                    :min-count 1)
                           :count 1)
                 :not
                 (s/map-of #{"not"}
                           :statement-filter/spec
                           :count 1))))

  ;; spec for a statement filter fn
  (s/def statement-filter-fspec
    (s/fspec :args (s/cat :filter-spec :statement-filter/spec
                          :statements (s/every ::xs/statement))
             :ret (s/every ::xs/statement)
             :fn (fn [{{:keys [statements]}
                       :args
                       ret :ret}]
                   (cset/superset? (set statements)
                                   (set ret)))))

  )


;; Filter (statements,spec):statements
;; Select (json,spec):json








(comment
  (defn dave-xf
  [& {:keys [init-outer ;; post init modifications
             step-inner ;; runs on state arg of step
             step-filter ;; pre-transform filter predicate
             step-map ;; element transform
             step-keep ;; post-transform filter predicate
             step-outer ;; runs oun the result of step
             completion-inner ;; run on the completed value, before other transforms
             completion-outer ;; run on the completed value, after other transforms
             ]
      :or {init-outer identity
           step-inner identity
           step-filter (constantly true)
           step-map identity
           step-keep (constantly true)
           step-outer identity
           completion-inner identity
           completion-outer identity}}]
  (fn [rf]
   (fn
     ;; init
     ([] (init-outer (rf)))
     ;; Completion
     ([result] (completion-outer
                (rf
                 (completion-inner result))))
     ;; Step
     ([result input]
      (let [element (and (step-filter input)
                         (let [m (step-map input)]
                           (and (step-keep m)
                                m)))]
        (-> result
            step-inner
            (cond->
              element
              (rf element))
            step-outer))))))

  (require '[clojure.java.io :as io])
  (into [] (dave-xf)
            (range 1000))

  (transduce (dave-xf :completion-outer set)  conj [] (range 100))

  (def count-xf
    "A transducer that just counts?"
    (fn [rf]
      (fn
        ([] (rf))
        ([result]
         (rf result))
        ([result input]
         (rf result input)))))


  (def ed (eduction (map inc) (range 10)))

  (reductions conj [] ed)

  (into []
        (comp
         (map str)
         count-xf
         )
        (repeat 100 :foo))
  (transduce (comp

              count-xf
              (map str))  conj [] (repeat 100 :foo))



  (def xs (into [] (repeatedly 100 #(rand-int 1000))))

  (require '[clojure.core.reducers :as r])

  (r/fold (partial merge-with +)
          (fn
            ([]
             {})
            ([result]
             result)
            ([result input]
             (update result input (fnil inc 0))))
          (repeatedly 100 #(rand-int 1000)))

  )

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))
