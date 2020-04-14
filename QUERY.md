# DAVE QUERIES
## The Data Analytics Visualization Environment for xAPI and the Total Learning Architecture - Query Features

**What is the DAVE Query Syntax**

The DAVE Beta implementation uses a new query syntax for xAPI filter, aggregate, and transformation queries. The basis of the language is DataScript, a variant of Datalog, and you can find some additional information below. Also provided by this implementation is a suite of xAPI attributes for traversing xAPI statements, and many math functions for transforms.

**DataScript Resources**

To get started using the new transform syntax implemented in DAVE Beta, you will need to know the basics of DataScript, as the DAVE query syntax is based on it. Here are a few resources to help get started:

- [Getting Started](https://github.com/tonsky/datascript/wiki/Getting-started)
- [Tutorials](https://github.com/kristianmandrup/datascript-tutorial)
- [DataScript 101](http://udayv.com/clojurescript/clojure/2016/04/28/datascript101/)

**xAPI Attributes**

xAPI attributes are programmed directly into the DAVE query syntax and can be used in queries to address specific elements of a statement. The following are currently available.

    :language-map/language-tag
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
    :attachment/fileUrl

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
    :statement/attachments

Example usage might be getting the Object ID from an activity in a Statement

    [:find ?s ?aid
     :where
     [?s :statement/object ?o]
     [?o :activity/id ?aid]]

**Math Functions**

The DAVE Query language also provides a large implementation of math functions, which are listed below:

    abs()
    acos()
    acosh()
    asin()
    asinh()
    atan()
    atan2()
    atanh()
    cbrt()
    ceil()
    clz32()
    cos()
    cosh()
    exp()
    expm1()
    floor()
    fround()
    hypot()
    imul()
    log()
    log10()
    log1p()
    log2()
    max()
    min()
    pow()
    random()
    round()
    sign()
    sin()
    sinh()
    sqrt()
    tan()
    tanh()
    trunc()

These can be used in queries to transform values using the math transform syntax we designed for DAVE, which follows the pattern:

    [math :log ?input ?output]

Where "math" is the keyword that tells DAVE to perform a math transform, ":log" tells it to use the logarithm function from above, and ?input is the value to take the log of, and ?output is the transformed value prepared for the output data. An example of a query using this syntax might be:

    [:find ?score-log
     :where
     [?s :statement.result.score/scaled ?score]
     [math :log ?score ?score-log]]
