(ns com.yetanalytics.dave.func.state-test
  (:require
   [clojure.test #?(:cljs :refer-macros
                    :clj :refer) [deftest is testing]]
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   clojure.test.check.generators
   [com.yetanalytics.dave.func.state :as state]
   [com.yetanalytics.dave.test-support :refer [failures stc-opts]]))

(deftest step-state-test
  (is (empty?
       (failures
        (stest/check
         `state/step-state
         {stc-opts
          {:num-tests 10
           :max-size 3}})))))
