(ns com.yetanalytics.dave.datalog
  "Datalog facilities for DAVE"
  (:require [clojure.spec.alpha :as s :include-macros true]
            [clojure.spec.gen.alpha :as sgen :include-macros true]
            [xapi-schema.spec :as xs]
            [clojure.walk :as w]
            [datascript.core :as d]
            [com.yetanalytics.dave.datalog.schema :as schema]
            [com.yetanalytics.dave.datalog.rules :as rules]
            [clojure.string :as cstr]
            #?@(:cljs [[goog.string :as gstring :refer [format]]
                       [goog.string.format]])))

;; Language map conformer override
;; just entities with ltag keys
(s/def ::lmap
  (s/with-gen
    (s/and
     (s/conformer (fn [x] (keyword "lmap" (name x)))
                  (fn [x] (name x)))
     qualified-keyword?
     #(= "lmap" (namespace %)))
    (fn []
      (sgen/elements #{:lmap/en-US
                       :lmap/en-GB}))))

(s/def ::xs/language-map
  (s/with-gen
    (s/map-of ::lmap
              string?
              :conform-keys true
              :gen-max 3
              :min-count 1)
    (fn []
      (sgen/fmap
       (partial reduce-kv (fn [m k v]
                            (assoc m (name k) v)) {})
       (s/gen (s/map-of ::lmap
                        string?
                        :conform-keys true
                        :gen-max 3
                        :min-count 1))))))

;; Extensions conform override
;; just a map with ":extension/<iri>" keys

(s/def ::extension-key
  (s/with-gen
    (s/and
     (s/conformer (fn [x] (keyword "extension" (name x)))
                  (fn [x] (name x)))
     qualified-keyword?
     #(= "extension" (namespace %)))
    (fn []
      (sgen/fmap
       (fn [iri]
         (keyword "extension" iri))
       (s/gen ::xs/iri)))))

(s/def ::xs/extensions
  (s/with-gen (s/map-of ::extension-key
                        (s/nonconforming ::xs/any-json)
                        :conform-keys true
                        :gen-max 3
                        :min-count 1)
    (fn []
      (sgen/fmap
       (partial reduce-kv (fn [m k v]
                            (assoc m (name k) v)) {})
       (s/gen (s/map-of ::extension-key
                        (s/nonconforming ::xs/any-json)
                        :conform-keys true
                        :gen-max 3
                        :min-count 1))))))

;; Statement Authority override - the conformer is crazy. Just make it any actor
(s/def :statement/authority
  ::xs/actor)

(s/def ::schema ;; only ever one schema
  #{schema/xapi})

(defn collapse
  [m k]
  (if-let [to-collapse (get m k)]
    (let [kns (namespace k)]
      (reduce-kv
       (fn [m' k' v]
         (assoc m'
                (keyword (format "%s.%s"
                                 kns
                                 (namespace k'))
                         (name k'))
                v))
       (dissoc m k)
       to-collapse))
    m))

(defn mash
  "Make an attribute mashing two keys together"
  [m mash-k k1 k2]
  (let [v1 (get m k1)
        v2 (get m k2)]
    (if (and v1 v2
             (not (get m mash-k)))
      (assoc m mash-k (format "%s|%s" v1 v2))
      m)))

(defn uniqify-component
  "Given a map and keys for the component and parent lookup, set
  :component/unique-to to a tuple of the parent ident and key"
  [m component-k lookup-k]
  (let [component (get m component-k)]
    (if (and component
             (not (:component/unique-to component)))
      (assoc-in m [component-k :component/unique-to] (pr-str
                                                      [(find m lookup-k)
                                                       component-k]))
      m)))

(defn uniqify-component-card-many
  "Same as uniquify component, but for card-many components like icomps.
  Takes an additional key for an id value on component entities

  Takes an additional function arg that applies extra processing to each member"
  [m component-k lookup-k each-k each-fn]
  (let [components (get m component-k)]
    (if (not-empty components)
      (let [lookup (find m lookup-k)]
        (assoc m component-k
               (into []
                     (for [component components]
                       (if (:component/unique-to component)
                         component
                         (each-fn (assoc component :component/unique-to
                                         (pr-str
                                          [lookup
                                           component-k
                                           (get component each-k)]))))))))m)))


(def collapse-any
  (comp
   #(collapse % :statement/result)
   #(collapse % :sub-statement/result)
   #(collapse % :result/score)
   #(collapse % :statement/context)
   #(collapse % :sub-statement/context)
   #(collapse % :context/contextActivities)
   #(collapse % :activity/definition)
   #(collapse % :agent/account)
   #(collapse % :group/account)))

(def mash-any
  (comp
   #(mash % :agent.account/mash :agent.account/homePage :agent.account/name)
   #(mash % :group.account/mash :group.account/homePage :group.account/name)))

(def uniqify-icomp
  #(uniqify-component % :interaction-component/description :component/unique-to))

(def uniqify-attachment
  (comp #(uniqify-component % :attachment/display :component/unique-to)
        #(uniqify-component % :attachment/description :component/unique-to)))

(def uniqify-any
  (comp
   #(uniqify-component % :activity.definition/name :activity/id)
   #(uniqify-component % :activity.definition/description :activity/id)
   #(uniqify-component % :activity.definition/extensions :activity/id)
   #(uniqify-component % :verb/display :verb/id)
   #(uniqify-component % :statement.result/extensions :statement/id)
   #(uniqify-component % :sub-statement.result/extensions :component/unique-to)
   #(uniqify-component % :statement.context/extensions :statement/id)
   #(uniqify-component % :sub-statement.context/extensions :component/unique-to)
   ;; icomps
   #(uniqify-component-card-many % :activity.definition/choices :activity/id
                                 :interaction-component/id
                                 uniqify-icomp)
   #(uniqify-component-card-many % :activity.definition/scale :activity/id
                                 :interaction-component/id
                                 uniqify-icomp)
   #(uniqify-component-card-many % :activity.definition/source :activity/id
                                 :interaction-component/id
                                 uniqify-icomp)
   #(uniqify-component-card-many % :activity.definition/steps :activity/id
                                 :interaction-component/id
                                 uniqify-icomp)
   #(uniqify-component-card-many % :activity.definition/target :activity/id
                                 :interaction-component/id
                                 uniqify-icomp)
   #(uniqify-component-card-many % :statement/attachments :statement/id
                                 :attachment/sha2
                                 uniqify-attachment)
   #(uniqify-component-card-many % :sub-statement/attachments :statement/id
                                 :attachment/sha2
                                 uniqify-attachment)
   ))

(defn anon-group-id
  [x]
  (if (and (:group/member x)
           (not ((some-fn
                 :group/account-mash
                 :group/mbox
                 :group/mbox_sha1sum
                 :group/openid) x)))
    (assoc x :anon-group/member (pr-str (:group/member x)))
    x))

(s/fdef ->tx
  :args (s/cat :schema ::schema
               :data (s/with-gen ::xs/statements
                       (fn []
                         (sgen/fmap
                          (fn [ss]
                            (mapv #(dissoc % "authority") ss))
                          (s/gen ::xs/lrs-statements)))))
  :ret (s/every map?))

(defn ->tx
  "Squish it down, leave it as entities"
  [schema data]
  (let [conformed (s/conform (s/coll-of ::xs/statement
                                        :kind vector?
                                        :into []) data)]
    (if (= conformed ::s/invalid)
      (throw (ex-info "Invalid Statement Data"
                      {:type ::invalid-statements
                       :spec-error (s/explain-data ::xs/statements data)
                       :data data}))
      (w/postwalk
       (fn coerce [x]
         (if (map? x)
           (-> x
               collapse-any
               mash-any
               anon-group-id
               uniqify-any
               ;; remove nil vals
               (->> (reduce-kv (fn [m k v]
                                 (if (nil? v)
                                   m
                                   (assoc m k v)))
                               {})))
           x))
       ;; we map over to set unique ids for substatements
       (mapv (fn [s]
               (cond-> s
                   ;; for substatements, we just go and set the component/unique-by
                   (and (:statement/object s)
                        (get-in s [:statement/object :sub-statement/objectType]))
                   (uniqify-component :statement/object :statement/id)))
             conformed)))))

(s/def ::db
  (s/with-gen d/db?
    (fn []
      (sgen/return (d/empty-db schema/xapi)))))

(s/fdef transact
  :args (s/cat :db ::db
               :statements (s/with-gen ::xs/statements
                             (fn []
                               (sgen/fmap
                                (fn [ss]
                                  (mapv #(dissoc % "authority") ss))
                                (s/gen ::xs/lrs-statements)))))
  :ret ::db)

(defn transact
  "Given a DB and some statements, transact them.
  Omits statements that are already known to the DB!"
  [db statements]
  (d/db-with db
             (->tx schema/xapi
                   (filterv
                    (fn [{:strs [id]}]
                      (nil? (d/entid db [:statement/id id])))
                    statements))))

(s/fdef empty-db
  :args (s/cat)
  :ret ::db)

(defn empty-db
  []
  (d/empty-db schema/xapi))

(defn q
  "Wrapper for datascript.core/q, always expects DB as a second arg, and injects
   core rules as a third"
  [query db & extra-inputs]
  (apply d/q query db rules/core extra-inputs))
