(ns com.yetanalytics.dave.datalog.rules
  "Core rules for DAVE Datalog queries")

(def core
  '[[[->unix ?timestamp ?unix]
     [$fn :timestamp->unix ?f]
     [(?f ?timestamp) ?unix]]
    [[->x-y-datum ?x ?y ?datum]
     [(array-map :x ?x :y ?y) ?datum]]])
