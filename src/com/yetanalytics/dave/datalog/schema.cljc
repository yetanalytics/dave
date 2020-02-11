(ns com.yetanalytics.dave.datalog.schema
  "Datascript schema for xAPI")

(def xapi
  {:definition/source
   {:db/isComponent true,
    :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref},
   :definition/extensions
   {:db/isComponent true,
    ; :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref},
   :definition/description
   {:db/isComponent true,
    ; :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref},
   :agent/mbox                    {:db/unique :db.unique/identity},
   :result/response               {:db/index true},
   :statement/result              {:db/isComponent true, :db/valueType :db.type/ref},

   :statement/id                  {:db/index true, :db/unique :db.unique/identity},
   :context/extensions
   {:db/isComponent true,
    ; :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref},
   :activity/id                   {:db/unique :db.unique/identity},
   :statement/context             {:db/isComponent true, :db/valueType :db.type/ref},
   :context/team                  {:db/valueType :db.type/ref},
   :definition/steps
   {:db/isComponent true,
    :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref},
   :statement-ref/id        {:db/unique :db.unique/identity},
   :context/contextActivities     {:db/isComponent true, :db/valueType :db.type/ref},

   :sub-statement/verb            {:db/valueType :db.type/ref},
   :contextActivities/other
   {:db/cardinality :db.cardinality/many, :db/valueType :db.type/ref},
   :group/mbox                    {:db/unique :db.unique/identity},
   :contextActivities/category
   {:db/cardinality :db.cardinality/many, :db/valueType :db.type/ref},
   :agent/openid                  {:db/unique :db.unique/identity},
   :agent/account
   {; :db/unique      :db.unique/identity,
    :db/isComponent true,
    :db/valueType   :db.type/ref},
   :statement/timestamp           {:db/index true},
   :context/instructor            {:db/valueType :db.type/ref},
   :group/member
   {:db/cardinality :db.cardinality/many, :db/valueType :db.type/ref},
   :anon-group/member
   {; :db/cardinality :db.cardinality/many,
    ; :db/valueType :db.type/ref,
    :db/unique :db.unique/identity}
   :definition/target
   {:db/isComponent true,
    :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref},
   :statement/exact-stored?       {:db/index true},
   :agent/account-mash            {:db/unique :db.unique/identity},
   :sub-statement/attachments
   {:db/isComponent true, :db/cardinality :db.cardinality/many, :db/valueType :db.type/ref},
   :statement/object              {:db/valueType :db.type/ref},
   :statement/actor               {:db/valueType :db.type/ref},
   :statement/stored              {:db/index true},
   :sub-statement/context
   {:db/isComponent true, :db/valueType :db.type/ref},
   :account/mash                  {:db/unique :db.unique/identity},
   :group/openid                  {:db/unique :db.unique/identity},
   :group/account
   {; :db/unique      :db.unique/identity,
    :db/isComponent true,
    :db/valueType   :db.type/ref},
   :attachment/display
   {:db/isComponent true,
    ; :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref},
   :result/score                  {:db/isComponent true, :db/valueType :db.type/ref},
   :interaction-component/description
   {:db/isComponent true,
    ; :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref},
   :context/statement             {:db/valueType :db.type/ref},
   :contextActivities/parent
   {:db/cardinality :db.cardinality/many, :db/valueType :db.type/ref},
   :agent/mbox_sha1sum            {:db/unique :db.unique/identity},
   :definition/correctResponsesPattern
   {:db/cardinality :db.cardinality/many},
   ;; :definition/interactionType    {:db/valueType :db.type/ref},
   :definition/choices
   {:db/isComponent true,
    :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref},
   :activity/definition
   {:db/isComponent true, :db/valueType :db.type/ref},
   :attachment/description
   {:db/isComponent true,
    ; :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref},
   :statement-reference/statement {:db/valueType :db.type/ref},
   :verb/id                       {:db/unique :db.unique/identity},
   :score/scaled                  {:db/index true},
   :sub-statement/result
   {:db/isComponent true, :db/valueType :db.type/ref},
   :verb/display
   {:db/isComponent true,
    ; :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref},
   :extension/iri                 {:db/index true},
   :group/account-mash            {:db/unique :db.unique/identity},
   :statement/verb                {:db/valueType :db.type/ref},
   :definition/scale
   {:db/isComponent true,
    :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref},
   :result/extensions
   {:db/isComponent true,
    ; :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref},
   :contextActivities/grouping
   {:db/cardinality :db.cardinality/many, :db/valueType :db.type/ref},
   :sub-statement/timestamp       {:db/index true},
   :statement/attachments
   {:db/isComponent true,
    :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref},
   :statement/authority           {:db/valueType :db.type/ref},
   :language-map/mash             {:db/index true, :db/unique :db.unique/identity},
   :sub-statement/object          {:db/valueType :db.type/ref},
   :group/mbox_sha1sum            {:db/unique :db.unique/identity},
   ;; :definition/activity-iri       {:db/unique :db.unique/identity},
   :sub-statement/actor           {:db/valueType :db.type/ref},
   :definition/name
   {:db/isComponent true,
    ; :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref}

   ;; Additional unique attrs for idempotency
   ;; TODO, these suck, remove
   ;; :extension/unique-to           {:db/unique :db.unique/identity}, ;; parent ident + ext id

   ;; :interaction-component/unique-to ;; activity + icomp key + icomp id
   ;; {:db/unique :db.unique/identity, :db/isComponent false},

   ;; :result/unique-to {:db/unique :db.unique/identity
   ;;                    :db/valueType :db.type/ref} ;; statement | sub

   ;; :score/unique-to {:db/unique :db.unique/identity
   ;;                   :db/valueType :db.type/ref} ;; statement | sub

   ;; :context/unique-to {:db/unique :db.unique/identity
   ;;                     :db/valueType :db.type/ref} ;; statement | sub
   ;; :contextActivities/unique-to {:db/unique :db.unique/identity
   ;;                               :db/valueType :db.type/ref} ;; statement | sub
   ;; :sub-statement/unique-to {:db/unique :db.unique/identity} ;; statement

   ;; let's try universal uniqueness on a string ident
   :component/unique-to {:db/unique :db.unique/identity}
   })


(comment
  (clojure.pprint/pprint (for [[attr {is-comp? :db/isComponent
                                      :or {is-comp? false}}] xapi
                               :when is-comp?]
                           attr))


  )


(def xapi-attrs
  [:language-map/language-tag
   :language-map/text

   :extension/iri
   :extension/json

   :account/name
   :account/homePage

   :agent/objectType
   :agent/name
   :agent/mbox
   :agent/mbox_sha1sum
   :agent/openid
   :agent/account

   :group/objectType
   :group/name
   :group/mbox
   :group/mbox_sha1sum
   :group/account
   :group/member
   :group/account

   :verb/id
   :verb/display

   :interaction-component/id
   :interaction-component/description

   :definition/name
   :definition/description
   :definition/correctResponsesPattern
   :definition/interactionType
   :definition/type
   :definition/moreInfo
   :definition/choices
   :definition/scale
   :definition/source
   :definition/target
   :definition/steps
   :definition/extensions

   :activity/objectType
   :activity/id
   :activity/definition

   :statement-reference/objectType
   :statement-reference/id

   :score/scaled
   :score/raw
   :score/min
   :score/max

   :result/score
   :result/success
   :result/completion
   :result/response
   :result/duration
   :result/extensions

   :context-activities/parent
   :context-activities/grouping
   :context-activities/category
   :context-activities/other

   :context/registration
   :context/instructor
   :context/team
   :context/contextActivities
   :context/revision
   :context/platform
   :context/language
   :context/statement
   :context/extensions

   :attachment/usageType
   :attachment/display
   :attachment/description
   :attachment/contentType
   :attachment/length
   :attachment/sha2
   :attachement/fileUrl

   :sub-statement/actor
   :sub-statement/verb
   :sub-statement/object
   :sub-statement/result
   :sub-statement/context
   :sub-statement/timestamp
   :sub-statement/attachments
   :sub-statement/objectType

   :statement/id
   :statement/actor
   :statement/verb
   :statement/object
   :statement/result
   :statement/context
   :statement/timestamp
   :statement/stored
   :statement/authority
   :statement/version
   :statement/attachments])


(comment

  (sort-by (comp :db/unique xapi) (comp - compare) xapi-attrs)

  (filter (comp :db/unique xapi) xapi-attrs)


  )
