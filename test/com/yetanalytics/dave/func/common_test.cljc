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
         `common/get-helper)))))

(deftest get-ifi-test
  (is (empty?
       (failures
        (stest/check
         `common/get-ifi)))))

(deftest get-lmap-val-test
  (is (empty?
       (failures
        (stest/check
         `common/get-lmap-val)))))

(deftest parse-agent-test
  (is (empty?
       (failures
        (stest/check
         `common/parse-agent)))))

(deftest parse-group-test
  (is (empty?
       (failures
        (stest/check
         `common/parse-group)))))

(deftest parse-activity-test
  (is (empty?
       (failures
        (stest/check
         `common/parse-activity)))))

(deftest parse-actor-test
  (is (empty?
       (failures
        (stest/check
         `common/parse-actor)))))

(deftest parse-verb-test
  (is (empty?
       (failures
        (stest/check
         `common/parse-verb)))))

(deftest parse-object-test
  (is (empty?
       (failures
        (stest/check
         `common/parse-object)))))

(deftest parse-statement-simple-test
  (is (empty?
       (failures
        (stest/check
         `common/parse-statement-simple)))))
