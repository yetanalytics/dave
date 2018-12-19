(ns com.yetanalytics.dave-test
    (:require
     [clojure.test #?(:cljs :refer-macros
                      :clj :refer) [deftest is testing]]
     [clojure.spec.alpha :as s]
     clojure.test.check.generators
     [com.yetanalytics.dave :as dave]))

(deftest dummy-test
  (is #?(:cljs true
         :clj true)))
