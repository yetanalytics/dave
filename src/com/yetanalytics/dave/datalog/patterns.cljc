(ns com.yetanalytics.dave.datalog.patterns
  "Patterns for xAPI datalog pull"
  (:require [clojure.walk :as w]))

(def language-map
  [:language-map/language-tag
   :language-map/text])

(def extension
  [:extension/iri
   :extension/json])

(def account
  [:account/name
   :account/homePage])

(def agente
  [:agent/objectType
   :agent/name
   :agent/mbox
   :agent/mbox_sha1sum
   :agent/openid
   {:agent/account account}])

(def group
  [:group/objectType
   :group/name
   :group/mbox
   :group/mbox_sha1sum
   {:group/account account}
   {:group/member agente}])

(def actor
  (into agente group))

(def verb
  [:verb/id
   {:verb/display language-map}])

(def interaction-component
  [:interaction-component/id
   {:interaction-component/description language-map}])

(def definition
  [{:definition/name language-map}
   {:definition/description language-map}
   :definition/correctResponsesPattern
   {:definition/interactionType [:db/ident]}
   :definition/type
   :definition/moreInfo

   {:definition/choices interaction-component}
   {:definition/scale interaction-component}
   {:definition/source interaction-component}
   {:definition/target interaction-component}
   {:definition/steps interaction-component}

   {:definition/extensions extension}])

(def activity
  [:activity/objectType
   :activity/id
   {:activity/definition definition}])

(def statement-reference
  [:statement-reference/objectType
   :statement-reference/id]
  )

(def score
  [:score/scaled
   :score/raw
   :score/min
   :score/max])

(def result
  [{:result/score score}
   :result/success
   :result/completion
   :result/response
   :result/duration
   {:result/extensions extension}])

(def context-activities
  [{:context-activities/parent activity}
   {:context-activities/grouping activity}
   {:context-activities/category activity}
   {:context-activities/other activity}])

(def context
  [:context/registration
   {:context/instructor actor}
   {:context/team group}
   {:context/contextActivities context-activities}
   :context/revision
   :context/platform
   :context/language
   {:context/statement statement-reference}
   {:context/extensions extension}])

(def attachment
  [:attachment/usageType
   {:attachment/display language-map}
   {:attachment/description language-map}
   :attachment/contentType
   :attachment/length
   :attachment/sha2
   :attachement/fileUrl])

(def ss-object
  (into actor (concat activity statement-reference)))

(def sub-statement
  [{:sub-statement/actor actor}
   {:sub-statement/verb verb}
   {:sub-statement/object ss-object}
   {:sub-statement/result result}
   {:sub-statement/context context}
   :sub-statement/timestamp
   {:sub-statement/attachments attachment}
   :sub-statement/objectType
   ])

(def s-object
  (into actor (concat activity sub-statement statement-reference)))

(def statement
  [:statement/id
   {:statement/actor actor}
   {:statement/verb verb}
   {:statement/object s-object}
   {:statement/result result}
   {:statement/context context}
   :statement/timestamp
   :statement/stored
   {:statement/authority actor}
   :statement/version
   {:statement/attachments attachment}
   ])

(defn as-json
  "Helper function to force all attributes to their string form"
  [pat]
  (w/postwalk
   (fn [node]
     (if (qualified-keyword? node)
       [node :as (name node)]
       node))
   pat))

(def statement-json
  (as-json statement))
