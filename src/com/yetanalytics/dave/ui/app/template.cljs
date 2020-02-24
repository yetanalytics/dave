(ns com.yetanalytics.dave.ui.app.template)

(def queries
  {:query-1 "[:find ?x ?y ?c
  :where
  [?s :statement/timestamp ?t]
  [?s :statement.result.score/scaled ?y]
  [?s :statement/actor ?a]
  [?a :agent/mbox ?c]
  [->unix ?t ?x]]"})

(def visualizations
  {:viz-1 "{\n  \"autosize\": \"fit\",\n  \"legends\": [\n    {\n      \"fill\": \"color\"\n    }\n  ],\n  \"axes\": [\n    {\n      \"orient\": \"bottom\",\n      \"scale\": \"x\",\n      \"labelAngle\": 60,\n      \"labelAlign\": \"left\",\n      \"labelLimit\": 112,\n      \"labelOverlap\": true,\n      \"labelSeparation\": -35\n    },\n    {\n      \"orient\": \"left\",\n      \"scale\": \"y\"\n    }\n  ],\n  \"width\": 500,\n  \"scales\": [\n    {\n      \"name\": \"x\",\n      \"type\": \"time\",\n      \"range\": \"width\",\n      \"domain\": {\n        \"data\": \"result\",\n        \"field\": \"?x\"\n      }\n    },\n    {\n      \"name\": \"y\",\n      \"type\": \"linear\",\n      \"range\": \"height\",\n      \"nice\": true,\n      \"zero\": true,\n      \"domain\": {\n        \"data\": \"result\",\n        \"field\": \"?y\"\n      }\n    },\n    {\n      \"name\": \"color\",\n      \"type\": \"ordinal\",\n      \"range\": \"category\",\n      \"domain\": {\n        \"data\": \"result\",\n        \"field\": \"?c\"\n      }\n    }\n  ],\n  \"padding\": 5,\n  \"marks\": [\n    {\n      \"type\": \"group\",\n      \"from\": {\n        \"facet\": {\n          \"name\": \"series\",\n          \"data\": \"result\",\n          \"groupby\": \"?c\"\n        }\n      },\n      \"marks\": [\n        {\n          \"type\": \"symbol\",\n          \"from\": {\n            \"data\": \"series\"\n          },\n          \"encode\": {\n            \"enter\": {\n              \"size\": {\n                \"value\": 50\n              },\n              \"x\": {\n                \"scale\": \"x\",\n                \"field\": \"?x\"\n              },\n              \"y\": {\n                \"scale\": \"y\",\n                \"field\": \"?y\"\n              },\n              \"fill\": {\n                \"scale\": \"color\",\n                \"field\": \"?c\"\n              }\n            }\n          }\n        }\n      ]\n    }\n  ],\n  \"$schema\": \"https://vega.github.io/schema/vega/v4.json\",\n  \"signals\": [\n    {\n      \"name\": \"interpolate\",\n      \"value\": \"linear\"\n    }\n  ],\n  \"height\": 200\n}\n"})
