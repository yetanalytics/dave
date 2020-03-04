(ns com.yetanalytics.dave.datalog.schema
  "Datascript schema for xAPI")

;; Try to squeeze down the number of datoms by removing intermediate
;; entities + components
(def xapi
  {:activity/id                   {:db/unique :db.unique/identity},
   :activity.definition/choices
   {:db/isComponent true,
    :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref},
   :activity.definition/correctResponsesPattern
   {:db/cardinality :db.cardinality/many},
   :activity.definition/description
   {:db/isComponent true, :db/valueType :db.type/ref},
   :activity.definition/extensions
   {:db/isComponent true, :db/valueType :db.type/ref},
   :activity.definition/name
   {:db/isComponent true, :db/valueType :db.type/ref},
   :activity.definition/scale
   {:db/isComponent true,
    :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref},
   :activity.definition/source
   {:db/isComponent true,
    :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref},
   :activity.definition/steps
   {:db/isComponent true,
    :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref},
   :activity.definition/target
   {:db/isComponent true,
    :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref},
   :agent/mbox                    {:db/unique :db.unique/identity},
   :agent/mbox_sha1sum            {:db/unique :db.unique/identity},
   :agent/openid                  {:db/unique :db.unique/identity},
   :agent.account/mash            {:db/unique :db.unique/identity},
   :anon-group/member             {:db/unique :db.unique/identity
                                   },
   :attachment/description
   {:db/isComponent true, :db/valueType :db.type/ref},
   :attachment/display
   {:db/isComponent true, :db/valueType :db.type/ref},
   :component/unique-to           {:db/unique :db.unique/identity},
   :group/mbox                    {:db/unique :db.unique/identity},
   :group/mbox_sha1sum            {:db/unique :db.unique/identity},
   :group/member
   {:db/cardinality :db.cardinality/many, :db/valueType :db.type/ref},
   :group/openid                  {:db/unique :db.unique/identity},
   :group.account/mash            {:db/unique :db.unique/identity},
   :interaction-component/description
   {:db/isComponent true, :db/valueType :db.type/ref},

   :statement/actor               {:db/valueType :db.type/ref},
   :sub-statement/actor           {:db/valueType :db.type/ref},

   :statement/attachments
   {:db/isComponent true,
    :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref},
   :sub-statement/attachments
   {:db/isComponent true,
    :db/cardinality :db.cardinality/many,
    :db/valueType   :db.type/ref},

   :statement/authority           {:db/valueType :db.type/ref},
   :statement/id                  {:db/index true, :db/unique :db.unique/identity},
   :statement/object              {:db/valueType :db.type/ref},
   :sub-statement/object          {:db/valueType :db.type/ref},

   ; :statement/stored              {:db/index true}, ;; Don't index if we are duping
   :statement/stored-inst         {:db/index true},
   ; :statement/timestamp           {:db/index true},
   :statement/timestamp-inst      {:db/index true},
   ; :sub-statement/timestamp       {:db/index true},
   :sub-statement/timestamp-inst  {:db/index true},
   :statement/verb                {:db/valueType :db.type/ref},
   :sub-statement/verb            {:db/valueType :db.type/ref},
   :statement-ref/id              {:db/unique :db.unique/identity},
   :statement.context/extensions
   {:db/isComponent true, :db/valueType :db.type/ref},
   :sub-statement.context/extensions
   {:db/isComponent true, :db/valueType :db.type/ref},
   :statement.context/instructor  {:db/valueType :db.type/ref},
   :sub-statement.context/instructor  {:db/valueType :db.type/ref},
   :statement.context/statement   {:db/valueType :db.type/ref},
   :sub-statement.context/statement   {:db/valueType :db.type/ref},
   :statement.context/team        {:db/valueType :db.type/ref},
   :sub-statement.context/team        {:db/valueType :db.type/ref},

   :statement.context.contextActivities/category
   {:db/cardinality :db.cardinality/many, :db/valueType :db.type/ref},
   :sub-statement.context.contextActivities/category
   {:db/cardinality :db.cardinality/many, :db/valueType :db.type/ref},
   :statement.context.contextActivities/grouping
   {:db/cardinality :db.cardinality/many, :db/valueType :db.type/ref},
   :sub-statement.context.contextActivities/grouping
   {:db/cardinality :db.cardinality/many, :db/valueType :db.type/ref},
   :statement.context.contextActivities/other
   {:db/cardinality :db.cardinality/many, :db/valueType :db.type/ref},
   :sub-statement.context.contextActivities/other
   {:db/cardinality :db.cardinality/many, :db/valueType :db.type/ref},
   :statement.context.contextActivities/parent
   {:db/cardinality :db.cardinality/many, :db/valueType :db.type/ref},
   :sub-statement.context.contextActivities/parent
   {:db/cardinality :db.cardinality/many, :db/valueType :db.type/ref},
   :statement.result/extensions
   {:db/isComponent true, :db/valueType :db.type/ref},
   :sub-statement.result/extensions
   {:db/isComponent true, :db/valueType :db.type/ref},
   :statement.result/response     {:db/index true},
   :sub-statement.result/response     {:db/index true},
   :statement.result.score/scaled {:db/index true},
   :sub-statement.result.score/scaled {:db/index true},
   :verb/display                  {:db/isComponent true, :db/valueType :db.type/ref},
   :verb/id                       {:db/unique :db.unique/identity}})

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
