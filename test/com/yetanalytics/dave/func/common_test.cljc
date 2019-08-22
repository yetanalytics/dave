(ns com.yetanalytics.dave.func.common-test
  (:require
   [clojure.test #?(:cljs :refer-macros
                    :clj :refer) [deftest is testing]]
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   clojure.test.check.generators
   [com.yetanalytics.dave.func.common :as common]
   [com.yetanalytics.dave.test-support :refer [failures stc-opts]]))

(deftest scale-test
  (is (empty?
       (failures
        (stest/check `common/scale)))))

(deftest get-helper-test
  (is (empty?
       (failures
        (stest/check
         `common/get-helper
         {:num-tests 10})))))

(deftest get-ifi-test
  (is (empty?
       (failures
        (stest/check
         `common/get-ifi
         {:num-tests 10})))))

(deftest get-lmap-val-test
  (is (empty?
       (failures
        (stest/check
         `common/get-lmap-val
         {:num-tests 10})))))

(deftest parse-agent-test
  (is (empty?
       (failures
        (stest/check
         `common/parse-agent
         {:num-tests 10})))))

(deftest parse-group-test
  (is (empty?
       (failures
        (stest/check
         `common/parse-group
         {:num-tests 10})))))

(deftest parse-activity-test
  (is (empty?
       (failures
        (stest/check
         `common/parse-activity
         {:num-tests 10})))))

(deftest parse-actor-test
  (is (empty?
       (failures
        (stest/check
         `common/parse-actor
         {:num-tests 10})))))

(deftest parse-verb-test
  (is (empty?
       (failures
        (stest/check
         `common/parse-verb
         {:num-tests 10})))))

(deftest parse-object-test
  (is (empty?
       (failures
        (stest/check
         `common/parse-object
         {:num-tests 10})))))

(deftest parse-statement-simple-test
  (is (empty?
       (failures
        (stest/check
         `common/parse-statement-simple
         {:num-tests 10})))))

(deftest handle-actor-test
  (is (empty?
       (failures
        (stest/check
         `common/handle-actor
         {:num-tests 10})))))

(deftest handle-object-test
  (is (empty?
       (failures
        (stest/check
         `common/handle-object
         {:num-tests 10})))))


