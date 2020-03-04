(ns com.yetanalytics.dave.datalog.rules
  "Core rules for DAVE Datalog queries")

(def core
  '[[[->unix ?timestamp ?unix]
     [$fn :timestamp->unix ?f]
     [(?f ?timestamp) ?unix]]
    [[math ?format ?args ?result]
     [$fn :math-transform ?f]
     [(?f ?format ?args) ?result]]
    [[->x-y-datum ?x ?y ?datum]
     [(array-map :x ?x :y ?y) ?datum]]
    [[time-format ?fmt ?t ?t-str]
     [$fn :time/format ?f]
     [(?f ?fmt ?t) ?t-str]]])
