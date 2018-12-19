;; This test runner is intended to be run from the command line
(ns com.yetanalytics.dave.test-runner
  (:require
   ;; require all the namespaces that you want to test
   [com.yetanalytics.dave-test]
   [com.yetanalytics.dave.func-test]
   [com.yetanalytics.dave.func.util-test]
   [com.yetanalytics.dave.func.common-test]
   [figwheel.main.testing :refer [run-tests-async]]))

(defn -main [& args]
  (run-tests-async 5000))
