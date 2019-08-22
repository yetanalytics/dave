(ns com.yetanalytics.dave.func.common
  (:require [xapi-schema.spec :as xs]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as sgen]
            [com.yetanalytics.dave.util.spec :as su]
            [com.yetanalytics.dave.func.util :as util]))

(s/fdef scale
  :args (s/with-gen (s/cat :raw number?
                           :min number?
                           :max number?)
          su/raw-min-max-gen)
  :ret (s/double-in :min 0.0
                    :max 100.0))


(defn scale
  "Given a number (raw) and its native domain (min and max), scale raw to the
  domain of 0.0..100.0 DAVE 2.6.6"
  [raw min max]
  (let [rng (- max min)]
    (if (< 0 rng)
      (double
       (* 100
          (/ (- raw min)
             rng)))
      100.0)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; common helpers for parsing statements
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/fdef get-helper
  :args (s/cat :src (s/map-of #{"foo"
                                "bar"
                                "baz"}
                              ::xs/any-json)
               :k #{"foo"
                    "bar"
                    "baz"
                    "notthere"})
  :ret ::xs/any-json)

(defn get-helper
  "get the value at `k` within `src`.
   - Returns nil if value is nil or empty"
  [src k]
  (let [query-result (get src k)]
    (cond (number? query-result) query-result
          (boolean? query-result) query-result
          :else
          (not-empty query-result))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ifi from agent or group
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::agent-ifi
  ;; :agent/[ifi-type] are defined in xapi-schema
  (s/or :mbox         :agent/mbox
        :account      :agent/account
        :mbox-sha1sum :agent/mbox_sha1sum
        :open-id      :agent/openid))

(s/def ::group-ifi
  ;; :group/[ifi-type] are defined in xapi-schema
  (s/nilable ;; anon groups have no ifi
   (s/or :mbox         :group/mbox
         :account      :group/account
         :mbox-sha1sum :group/mbox_sha1sum
         :open-id      :group/openid)))

(s/fdef get-ifi
  :args (s/cat :actor ::xs/actor)
  :ret  (s/or :agent  ::agent-ifi
              :group  ::group-ifi))

(defn get-ifi
  "check `m` for one of the possible IFI keys"
  [m]
  (or (get-helper m "mbox")
      (get-helper m "account")
      (get-helper m "openid")
      (get-helper m "mbox_sha1sum")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; language map text
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/fdef get-lmap-val
  :args (s/alt :unary (s/cat :lmap         ::xs/language-map)
               :two   (s/cat :lmap         ::xs/language-map
                             :language-tag ::xs/language-tag))
  :ret (s/nilable ;; provided language tag may be incorrect
        ::xs/language-map-text))

(defn get-lmap-val
  "single arity: return the first label across all language tags within a language map
    - ordering of labels is dictated by `vals` call
   double arity: return the label found at `language-tag` within `lmap`"
  ([lmap]
   (-> lmap vals first))
  ([lmap language-tag]
   (get-helper lmap language-tag)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; basic info from an agent
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::agent-name
  ;; :agent/name defined in xapi-schema
  :agent/name)

(s/def ::parse-agent-ret
  (s/keys :req-un [::agent-name ::agent-ifi]))

(s/fdef parse-agent
  :args (s/cat :agent ::xs/agent)
  :ret ::parse-agent-ret)

(defn parse-agent
  "return `agent-name` and `agent-ifi` given `m`"
  [m]
  (let [agent-name (or (get-helper m "name") "Unamed Agent")
        agent-ifi  (get-ifi m)]
    {:agent-name agent-name
     :agent-ifi  agent-ifi}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; basic info from a group
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::group-name :group/name)

(s/def ::group-members
  (s/coll-of ::parse-agent-ret :kind vector? :into [] :gen-max 3))

(s/def ::parse-group-ret
  (s/keys :req-un [::group-name]
          :opt-un [::group-members ::group-ifi]))

(s/fdef parse-group
  :args (s/cat :group ::xs/group)
  :ret  ::parse-group-ret)

(defn parse-group
  "return `group-name` and possibly `group-members` and/or `group-ifi`
    - `group-members` is a vector of maps containing `agent-name` and `agent-ifi`"
  [m]
  (let [group-name    (or (get-helper m "name") "Unamed Group")
        group-members (when-let [members (get-helper m "member")]
                        (mapv parse-agent members))
        group-ifi     (get-ifi m)]
    (cond-> {:group-name group-name}
      ;; did we have any members?
      (not-empty group-members) (assoc :group-members group-members)
      ;; did we have an ifi for the group?
      group-ifi                    (assoc :group-ifi group-ifi))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; basic info from an activity
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::activity-id
  ;; :activity/id defined in xapi-schema
  :activity/id)

(s/def ::activity-name
  ;; because of call to `get-lmap-val`
  ::xs/language-map-text)

(s/def ::parse-activity-ret
  (s/keys :req-un [::activity-id ::activity-name]))

(s/fdef parse-activity
  :args (s/alt :unary (s/cat :activity     ::xs/activity)
               :two   (s/cat :activity     ::xs/activity
                             :language-tag ::xs/language-tag))
  :ret ::parse-activity-ret)

(defn parse-activity
  "return `activity-id` and `activity-name` from `m`
   - single arity assumes `language-tag` is not known
   - double arity requires `language-tag` as an argument"
  ([m]
   (let [{activity-id                 "id"
          {activity-name-lmap "name"} "definition"} m
         activity-name (or (get-lmap-val activity-name-lmap) "Unamed Activity")]
     {:activity-id   activity-id
      :activity-name activity-name}))
  ([m language-tag]
   (let [{activity-id                 "id"
          {activity-name-lmap "name"} "definition"} m
         activity-name (or (get-lmap-val activity-name-lmap language-tag) "Unamed Activity")]
     {:activity-id   activity-id
      :activity-name activity-name})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; basic info from an actor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::parse-actor-ret
  (s/or :agent ::parse-agent-ret
        :group ::parse-group-ret))

(s/fdef parse-actor
  :args (s/cat :actor ::xs/actor)
  :ret ::parse-actor-ret)

(defn parse-actor
  "parse the actor as an agent or group based on the objectType
   - defaults to parsing as agent"
  [m]
  (case (get-helper m "objectType")
    "Group" (parse-group m)
    "Agent" (parse-agent m)
    ;; objectType is optional for Agents but not Groups
    (parse-agent m)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; basic info from a verb
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::verb-id
  ;; :verb/id defined in xapi-schema
  :verb/id)

(s/def ::verb-name
  ;; because of call to `get-lmap-val`
  ::xs/language-map-text)

(s/def ::parse-verb-ret
  (s/keys :req-un [::verb-id ::verb-name]))

(s/fdef parse-verb
  :args (s/alt :unary (s/cat :verb ::xs/verb)
               :two   (s/cat :verb ::xs/verb
                             :language-tag ::xs/language-tag))
  :ret ::parse-verb-ret)

(defn parse-verb
  "return `verb-id` and `verb-name` from `m`
   - single arity assumes `language-tag` is not known
   - double arity requires `language-tag` as an argument"
  ([m]
   {:verb-id   (get-helper m "id")
    :verb-name (or (-> m (get-helper "display") get-lmap-val) "Unamed Verb")})
  ([m language-tag]
   {:verb-id   (get-helper m "id")
    :verb-name (or (-> m (get-helper "display") (get-lmap-val language-tag)) "Unamed Verb")}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; basic info from an object
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::agent-as-object
  ;; temporary solution for generative testing
  ;; - can't use :statement/object for generative testing
  ;; -- not supporting substatements or statement refs
  ;; --- currently throwing when they are passed in, not sure how to write a spec for that
  ;; - can't use ::xs/agent because it doesn't properly enforce the objectType constraint (agent must have objectType)
  ;; -- simple approach (s/merge ::xs/agent (s/keys :req-un [:agent/objectType])) produces a bad generator
  ;; --- {"mbox" "mailto:some@email.test" :objectType "Agent"}
  ;; ---- not sure how to enforce all string keys from the outside
  ;; Solution = recreate ::xs/agent but will necessary objectType constraint applied
  (s/with-gen (s/and
               (s/conformer (partial xs/conform-ns-map "agent") xs/unform-ns-map)
               (s/keys :req [(or :agent/mbox
                                 :agent/mbox_sha1sum
                                 :agent/openid
                                 :agent/account)
                             :agent/objectType]
                       :opt [:agent/name])
               (xs/restrict-keys :agent/mbox
                                 :agent/mbox_sha1sum
                                 :agent/openid
                                 :agent/account
                                 :agent/name
                                 :agent/objectType)
               xs/max-one-ifi)
    #(sgen/fmap
      xs/unform-ns-map
      (s/gen (s/or :ifi-mbox
                   (s/keys :req [:agent/mbox :agent/objectType]
                           :opt [:agent/name])
                   :ifi-mbox_sha1sum
                   (s/keys :req [:agent/mbox_sha1sum :agent/objectType]
                           :opt [:agent/name])
                   :ifi-openid
                   (s/keys :req [:agent/openid :agent/objectType]
                           :opt [:agent/name])
                   :ifi-account
                   (s/keys :req [:agent/account :agent/objectType]
                           :opt [:agent/name]))))))

(s/def ::object
  (s/or :agent    ::agent-as-object
        :group    ::xs/group
        :activity ::xs/activity))

(s/def ::parse-object-ret
  (s/or :agent    ::parse-agent-ret
        :group    ::parse-group-ret
        :activity ::parse-activity-ret))

(s/fdef parse-object
  ;; limiting args to supported object types
  ;; - TODO: may be possible to spec intentional errors from (`throw` (`ex-info` ...)) but not sure how
  ;; -- some form of macro magic...I am but an apprentice
  :args (s/alt :unary (s/cat :supported-object-types ::object)
               :two   (s/cat :only-activity ::xs/activity
                             :language-tag  ::xs/language-tag))
  :ret ::parse-object-ret)

(defn parse-object
  "based on the object's type
   - pass `m` to `parse-group`
   - pass `m` to `parse-agent`
   - pass `m` to `parse-activity`
   - StatementRef and SubStatement are NOT SUPPORTED!

  defaults to `parse-activity`
   - when the object is an Agent or Group, objectType must be specified
   -- https://github.com/adlnet/xAPI-Spec/blob/master/xAPI-Data.md#requirements-6

  supports `language-tag` in 2-arity case but `m` must be an activity!"
  ([m]
   (case (get-helper m "objectType")
     "Group"    (parse-group m)
     "Agent"    (parse-agent m)
     "Activity" (parse-activity m)
     "StatementRef" (throw (ex-info "Statement References are not supported by `parse-object`"
                                    {:type ::operation-not-supported}))
     "SubStatement" (throw (ex-info "SubStatements are not supported by `parse-object`"
                                    {:type ::operation-not-supported}))
     (parse-activity m)))
  ([activity-m language-tag]
   ;; object = activity bc only case where `language-tag` is relevant
   (parse-activity activity-m language-tag)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; basic info from a statement specs
;; - warning...shield your eyes, it gets ugly
;; (╯°□°）╯︵ ┻━┻
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; return specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def :statement-simple.ret/id
  :statement/id)

(s/def :statement-simple.ret/actor
  ::parse-actor-ret)

(s/def :statement-simple.ret/verb
  ::parse-verb-ret)

(s/def :statement-simple.ret/object
  ::parse-object-ret)

(s/def :statement-simple.ret/timestamp
  number?)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; arg specs - single arity
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; TODO: there is probably a better way to handle this
;; - this is bc of
;; -- multi arity parse fns - target `language-tag` support...what have I done
;; -- intentional throwing on some objectTypes w/in parse-object
;; -- generate maps with string keys

(s/def :statement-simple.one.arg/id
  :statement/id)

(s/def :statement-simple.one.arg/actor
  :statement/actor)

(s/def :statement-simple.one.arg/verb
  :statement/verb)

(s/def :statement-simple.one.arg/object
  ::object)

(s/def :statement-simple.one.arg/timestamp
  :statement/timestamp)

(s/def :statement-simple.one.arg/statement
  (xs/conform-ns
   "statement-simple.one.arg"
   (s/keys :req [:statement-simple.one.arg/id
                 :statement-simple.one.arg/actor
                 :statement-simple.one.arg/verb
                 :statement-simple.one.arg/object
                 :statement-simple.one.arg/timestamp])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; arg specs - multi arity
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; TODO: there is probably a better way to handle this
;; - this is bc of
;; -- multi arity parse fns - target `language-tag` support...what have I done
;; -- intentional throwing on some objectTypes w/in parse-object
;; -- generate maps with string keys

(s/def :statement-simple.many.arg/id
  :statement/id)

(s/def :statement-simple.many.arg/actor
  :statement/actor)

(s/def :statement-simple.many.arg/verb
  :statement/verb)

(s/def :statement-simple.many.arg/object
  ::xs/activity)

(s/def :statement-simple.many.arg/timestamp
  :statement/timestamp)

(s/def :statement-simple.many.arg/statement
  (xs/conform-ns
   "statement-simple.many.arg"
   (s/keys :req [:statement-simple.many.arg/id
                 :statement-simple.many.arg/actor
                 :statement-simple.many.arg/verb
                 :statement-simple.many.arg/object
                 :statement-simple.many.arg/timestamp])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; we've made it to the fdef... щ（ﾟДﾟщ）
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/fdef parse-statement-simple
  :args (s/alt :only-statement        (s/cat :simple-statement      :statement-simple.one.arg/statement)
               :shared-language-tag   (s/cat :simple-statement      :statement-simple.many.arg/statement
                                             :shared-language-tag   ::xs/language-tag)
               :targeted-language-tag (s/cat :simple-statement      :statement-simple.many.arg/statement
                                             :language-tag-verb     ::xs/language-tag
                                             :language-tag-activity ::xs/language-tag))
  :ret (s/keys :req-un [:statement-simple.ret/id
                        :statement-simple.ret/actor
                        :statement-simple.ret/verb
                        :statement-simple.ret/object
                        :statement-simple.ret/timestamp]))

(defn parse-statement-simple
  "parses a statement to return
   - `id` = statement id
   - `actor` = return of `parse-actor`
   - `verb` = return of `parse-verb`
   - `object` = return of `parse-object`
   - `timestamp` = unix representation of the ISO timestamp

  1-arity case, language-tag is assumed to be unkown or irrelevant

  2-arity case, `language-tag` used for both `object` and `verb`
   - `object` must be an activity

  3-arity case `language-tag-verb` and `language-tag-activity` allow for differing language tags
   - its easy enough to do but unlikely to be needed."
  ([{:strs [id actor verb object timestamp] :as statement}]
   {:id           id
    :actor        (parse-actor actor)
    :verb         (parse-verb verb)
    :object       (parse-object object)
    :timestamp    (.getTime (util/timestamp->inst timestamp))})
  ([{:strs [id actor verb object timestamp] :as statement} language-tag]
   {:id           id
    :actor        (parse-actor actor)
    :verb         (parse-verb verb language-tag)
    :object       (parse-object object language-tag)
    :timestamp    (.getTime (util/timestamp->inst timestamp))})
  ([{:strs [id actor verb object timestamp] :as statement} language-tag-verb language-tag-activity]
   {:id           id
    :actor        (parse-actor actor)
    :verb         (parse-verb verb language-tag-verb)
    :object       (parse-object object language-tag-activity)
    :timestamp    (.getTime (util/timestamp->inst timestamp))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; my generators actually work
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  ;; can't use this because throw on statement refs and sub statements
  (first (sgen/sample (s/gen ::xs/statement)))

  ;; object can be agent, group or activity
  (let [agent-group-or-activity (first (sgen/sample (s/gen :statement-simple.one.arg/statement)))
        the-obj (get-helper agent-group-or-activity "object")]
    (and
     ;; valid statement
     (s/valid? ::xs/statement agent-group-or-activity)
     ;; valid agent, group or activity
     (s/valid? ::object the-obj)
     ;; not a sub-statement
     (false? (s/valid? ::xs/sub-statement the-obj))
     ;; not a statement ref
     (false? (s/valid? ::xs/statement-ref the-obj))))

  ;; object can only be activity
  (let [with-activity-obj (first (sgen/sample (s/gen :statement-simple.many.arg/statement)))
        the-obj (get-helper with-activity-obj "object")]
    (and
     ;; valid statement
     (s/valid? ::xs/statement with-activity-obj)
     ;; valid activity
     (s/valid? ::xs/activity the-obj)
     ;; not an agent
     (false? (s/valid? ::xs/agent the-obj))
     ;; not a group
     (false? (s/valid? ::xs/group the-obj))
     ;; not a substatement
     (false? (s/valid? ::xs/sub-statement the-obj))
     ;; not a statement ref
     (false? (s/valid? ::xs/statement-ref the-obj)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; helper for return information after statement has been parsed
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; actor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def :handle-actor.ret/agent-ordering
  (s/cat :name ::agent-name
         :ifi  ::agent-ifi))

(s/def :handle-actor.ret/agent
  (s/with-gen
    (s/and vector? :handle-actor.ret/agent-ordering)
    #(sgen/fmap vec (s/gen :handle-actor.ret/agent-ordering))))

(s/def :handle-actor.ret/group-ordering
  (s/cat :name ::group-name
         :ifi  ::group-ifi))

(s/def :handle-actor.ret/group
  (s/with-gen
    (s/and vector? :handle-actor.ret/group-ordering)
    #(sgen/fmap vec (s/gen :handle-actor.ret/group-ordering))))

(s/def :handle-actor.ret/members
  (s/coll-of :handle-actor.ret/agent
             :kind vector?
             :into []))

(s/fdef handle-actor
  :args (s/cat :parsed-actor
               (s/keys :req-un [(or (and ::agent-name ::agent-ifi) ::group-name)]
                       :opt-un [::group-members
                                ::group-ifi]))
  :ret (s/or :agent       :handle-actor.ret/agent
             :group       :handle-actor.ret/group
             :members     :handle-actor.ret/members
             :dont-accept nil?))

(defn handle-actor
  [{:keys [agent-name agent-ifi group-name group-members group-ifi]}]
  (if group-name
    ;; dealing with a group
    (if-let [members (not-empty
                      (mapv (fn [member]
                              (let [{a-name :agent-name
                                     a-ifi  :agent-ifi} member]
                                [a-name a-ifi]))
                            group-members))]
      ;; we had members, they are all we care about
      members
      ;; we didnt' have members, treat the group as an individual
      ;; - need their identity, only safe way to save their info in state
      (when group-ifi [group-name group-ifi]))
    ;; dealing with an agent, return what we know
    [agent-name agent-ifi]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; object
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def :handle-object.ret/agent
  :handle-actor.ret/agent)

(s/def :handle-object.ret/identified-group
  :handle-actor.ret/group)

(s/def :handle-object.ret/members
  (s/coll-of :handle-object.ret/agent
             :kind vector?
             :into []))

(s/def :handle-object.ret/group-with-members-ordering
  (s/cat :name    ::group-name
         :members :handle-object.ret/members))

(s/def :handle-object.ret/group-with-members
  (s/with-gen
    (s/and vector? :handle-object.ret/group-with-members-ordering)
    #(sgen/fmap vec (s/gen :handle-object.ret/group-with-members-ordering))))

(s/def :handle-object.ret/activity-ordering
  (s/cat :name ::activity-name
         :ifi  ::activity-id))

(s/def :handle-object.ret/activity
  (s/with-gen
    (s/and vector? :handle-object.ret/activity-ordering)
    #(sgen/fmap vec (s/gen :handle-object.ret/activity-ordering))))

(s/def :handle-object.ret/group-only-name
  (s/coll-of ::group-name
             :kind vector?
             :into []
             :count 1))

(s/fdef handle-object
  :args (s/cat :parsed-object
               (s/keys :req-un [(or (and ::agent-name ::agent-ifi)
                                    (and ::activity-id ::activity-name)
                                    ::group-name)]
                       :opt-un [::group-members
                                ::group-ifi]))
  :ret (s/or :activity     :handle-object.ret/activity
             :agent        :handle-object.ret/agent
             :group        :handle-object.ret/identified-group
             :with-members :handle-object.ret/group-with-members
             :only-name    :handle-object.ret/group-only-name))

(defn handle-object
  [{:keys [agent-name agent-ifi
           group-name group-members group-ifi
           activity-id activity-name]}]
  (cond (and activity-id activity-name)
        [activity-name activity-id]
        (and agent-name agent-ifi)
        [agent-name agent-ifi]
        (and group-name group-ifi)
        [group-name group-ifi]
        :else
        (if-let [members (not-empty
                          (mapv (fn [member]
                                  (let [{a-name :agent-name
                                         a-ifi  :agent-ifi} member]
                                    [a-name a-ifi]))
                                group-members))]
          [group-name members]
          [group-name])))
