;; This test runner is intended to be run from the command line
(ns com.yetanalytics.dave.test-runner
  (:require
   clojure.test.check
   clojure.test.check.properties
   ;; require all the namespaces that you want to test
   [com.yetanalytics.dave-test]
   [com.yetanalytics.dave.func.util-test]
   [com.yetanalytics.dave.func.common-test]
   [com.yetanalytics.dave.datalog-test]
   [figwheel.main.testing :refer-macros [run-tests-async]]))

(defn -main [& args]
  (run-tests-async 120000))
