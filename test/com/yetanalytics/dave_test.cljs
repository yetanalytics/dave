(ns com.yetanalytics.dave-test
    (:require
     [clojure.test :refer-macros [deftest is testing]]
     [clojure.spec.alpha :as s]
     clojure.test.check.generators
     [com.yetanalytics.dave :as dave]))

(deftest dummy-test
  (is true))
