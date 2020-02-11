(ns com.yetanalytics.dave.datalog
  "Datalog facilities for DAVE"
  (:require [clojure.spec.alpha :as s :include-macros true]
            [clojure.spec.gen.alpha :as sgen :include-macros true]
            [xapi-schema.spec :as xs]
            [clojure.walk :as w]
            [datascript.core :as d]
            [com.yetanalytics.dave.datalog.schema :as schema]
            [clojure.zip :as z]
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


(s/def ::tx-datom
  (s/or :regular-datom
        (s/tuple #{:db/add}
                 ;; db-id
                 (s/or :string string?
                       :number number?
                       )
                 qualified-keyword?
                 (s/or :string string?
                       :number number?
                       :bool boolean?
                       :nil nil?))
        :extension-datom
        (s/tuple #{:db/add}
                 (s/or :string string?
                       :number number?
                       )
                 ::extension-key
                 any?)))

(s/def ::schema ;; only ever one schema
  #{schema/xapi})


(defn attr-order
  "Sort order fn for attrs by schema"
  [schema attr]
  (if-let [{many? :db/cardinality
            ref? :db/valueType
            ident? :db/unique} (get schema attr {})]
    (cond
      ident? -1
      (and many? (not ref?)) 1
      ref?    (if many?
                3 2)
      :else 0
      )))


(defn entity-zip
  "Produce a zipper for nested tx entities"
  [root]
  (z/zipper
   coll?
   seq ;; sorted-children
   (fn make-node
     [node kids]
     (if-let [empty-coll (empty node)]
       (into empty-coll
             kids)
       ;; if clojure.core/empty doesn't work, check for map entry
       (if (map-entry? node)
         (if (= 2 (count kids))
           (let [[k v] kids]
             #?(:clj (clojure.lang.MapEntry. k v)
                :cljs (reify cljs.core/IMapEntry
                        (-key [_]
                          k)
                        (-val [_]
                          v))))
           (throw (ex-info "Can only have two children in a MapEntry"
                           {:type ::map-entry-constraint
                            :node node
                            :children kids})))
         (throw (ex-info (format "Don't know how to make %s node" (type node))
                         {:type ::unknown-collection
                          :node node
                          :node-type (type node)
                          :children kids})))))
   root))

(def find-ident
  ;; "Return an entities natural ident or nil"
  (apply some-fn (for [[attr {unique :db/unique}] schema/xapi #_(dissoc schema/xapi ;; remove compounds
                                                          :agent/account
                                                          :group/account)
                       :when
                       (= unique :db.unique/identity)]
                   #(find % attr))))

(defn entity-ident
  "Find an ident for ANY entity"
  [loc]
  (let [node (z/node loc)]
    (assert (map? node) "Must be run on a map")
    (or (find-ident node)
        ;; handle agent/group account silliness
        (when-let [{:keys [account/name
                           account/homePage]}
                   (not-empty (select-keys node
                                           [:account/name
                                            :account/homePage]))]
          [:account/mash (format "%s|%s"
                                 homePage name)])
        (when-let [account (:agent/account node)]
          (let [[_ mash] (entity-ident (entity-zip account))]
            [:agent/account-mash mash]))
        (when-let [account (:group/account node)]
          (let [[_ mash] (entity-ident (entity-zip account))]
            [:group/account-mash mash]))

        ;; Anon groups?!?

        (when (:group/objectType node) ;; when there's no ifi, you'll get here
          [:anon-group/member
           ;; Anon groups are best-effort. Let's just take a hash of the members
           (hash (:group/member node))])


        (let [[parent-loc
               parent-me-loc
               parent-key] (some
                            (fn [l]
                              (let [nd (z/node l)]
                                (when (map-entry? nd)
                                  [(z/up l)
                                   l
                                   (key nd)])))
                            (rest (iterate z/up loc)))
              [plk plv :as parent-ident] (entity-ident parent-loc)]
          [:component/unique-to
           (cond
             ;; direct component of an identified thing or subcomponent
             (contains? #{:activity/definition
                          :statement/context
                          :statement/result
                          :statement/object
                          :context/contextActivities
                          :result/score
                          :sub-statement/context
                          :sub-statement/result}
                        parent-key)
             (format "%s.%s"
                     (if (= :component/unique-to plk)
                       ;; continue another component spec
                       plv
                       (format "%s/%s:%s"
                               (namespace plk)
                               (name plk)
                               plv))
                     (name parent-key))

             ;; lmaps
             (contains? #{:attachment/description
                          :attachment/display
                          :context/extensions
                          :definition/name
                          :definition/description
                          :definition/extensions
                          :result/extensions
                          :verb/display
                          :interaction-component/description} parent-key)
             (format "%s.%s"
                     (if (= :component/unique-to plk)
                       plv
                       (format "%s/%s:%s"
                               (namespace plk)
                               (name plk)
                               plv))
                     (name parent-key))
             ;; icomps
             (contains? #{:definition/source
                          :definition/steps
                          :definition/target
                          :definition/choices
                          :definition/scale}
                        parent-key)
             (format "%s.%s:%s"
                     plv
                     (name parent-key)
                     (:interaction-component/id node))

             ;; attachments
             (contains? #{:statement/attachments
                          :sub-statement/attachments} parent-key)
             (format "%s.%s:%s"
                     plv
                     (name parent-key)
                     (:attachment/sha2 node))
             :else (throw (ex-info "No component code for map entry"
                                   {:type ::unknown-component
                                    ;; :me [k v]
                                    :node node
                                    :parent-lookup [plk plv]
                                    :parent-key parent-key}))

             )]))))
(s/fdef ->tx
  :args (s/cat :schema ::schema
               :data (s/with-gen ::xs/statements
                       (fn []
                         (sgen/fmap
                          (fn [ss]
                            (mapv #(dissoc % "authority") ss))
                          (s/gen ::xs/lrs-statements)))))
  :ret (s/every ::tx-datom))


(defn parent-loc
  "Given a loc, return the loc of the nearest parent, or nil"
  [loc]
  (some
   (fn [l]
     (let [nd (z/node l)]
       (when (map? nd)
         l)))
   (take-while some? (rest (iterate z/up loc)))))


(defn ->tx
  [schema data]
  (let [conformed (s/conform (s/coll-of ::xs/statement
                                        :kind vector?
                                        :into []) data)]
    (if (= conformed ::s/invalid)
      (throw (ex-info "Invalid Statement Data"
                      {:type ::invalid-statements
                       :spec-error (s/explain-data ::xs/statements data)
                       :data data}))
      (let [entity-ident-memo (memoize entity-ident)]
        (loop [loc (entity-zip conformed)
               tx []
               tempid -1]
          (if (z/end? loc)
            (into [] (sort-by (comp
                               (partial attr-order schema/xapi)
                               #(get % 2))
                              ;; remove any nil values
                              (remove
                               (fn [[_ _ _ v :as datom]]
                                 (nil? v))
                               tx)))
            (let [node (z/node loc)]
              (cond
                ;; on maps, make sure they have an ident
                (map? node)
                (let [[lk lv] (entity-ident-memo loc)]
                  (recur (z/next (z/edit loc assoc lk lv))
                         tx
                         tempid))

                (map-entry? node)
                (let [[k v] node
                      ent-ident (entity-ident-memo (parent-loc loc))
                      {many? :db/cardinality
                       ref? :db/valueType
                       component? :db/isComponent
                       ident? :db/unique
                       :as attr-spec} (get schema k {})
                      ext-me? (= "extension" (namespace k))]
                  (recur (if ext-me?
                           ;; remove extension locs so they are not walked
                           (z/next (z/remove loc))
                           (z/next loc))
                         (cond
                           ext-me?
                           (conj tx
                                 [:db/add
                                  (pr-str ent-ident)
                                  k
                                  v])
                           ref?
                           (if many?
                             (let [child-refs (map entity-ident-memo (-> loc
                                                                         z/down
                                                                         z/right
                                                                         z/down
                                                                         (->> (iterate z/right)
                                                                              (take-while some?))))]
                               (into tx
                                     (for [ref child-refs]
                                       [:db/add
                                        (pr-str ent-ident)
                                        k
                                        (pr-str ref)])))

                             (let [child-ref (entity-ident-memo (-> loc z/down z/right))]
                               (conj tx
                                     [:db/add
                                      (pr-str ent-ident)
                                      k
                                      (pr-str child-ref)])))
                           many?
                           (into tx
                                 (for [v' v]
                                   [:db/add (pr-str ent-ident) k v']))
                           :else
                           (conj tx [:db/add
                                     (pr-str ent-ident)
                                     k
                                     v]))
                         tempid))

                :else (recur (z/next loc)
                             tx
                             tempid)))))))))

(defn transact-xapi
  "Given a DB and some statements, transact them.
  Omits statements that are already known to the DB!"
  [db statements]
  (d/db-with db
             (->tx schema/xapi
                   (filterv
                    (fn [{:strs [id]}]
                      (nil? (d/entid db [:statement/id id])))
                    statements))))
