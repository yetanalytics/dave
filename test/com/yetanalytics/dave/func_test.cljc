(ns com.yetanalytics.dave.func-test
  (:require
   [clojure.test #?(:cljs :refer-macros
                    :clj :refer) [deftest is testing]]
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   clojure.test.check.generators
   [com.yetanalytics.dave.func :as func]
   [com.yetanalytics.dave.test-support :refer [failures stc-opts]]))

(deftest dummy-test
  (is #?(:cljs true
         :clj true)))

(deftest success-timeline-test
  (is (empty?
       (failures
        (stest/check
         `func/success-timeline
         {stc-opts
          ;; TODO: improve generative test perf in cljs
          {:num-tests 1 :max-size 1}})))))

(deftest difficult-questions-test
  (is (empty?
       (failures
        (stest/check
         `func/difficult-questions
         {stc-opts
          {:num-tests 1 :max-size 1}})))))

(deftest completion-rate-test
  (is (empty?
       (failures
        (stest/check
         `func/completion-rate
         {stc-opts
          {:num-tests 1 :max-size 1}})))))

(deftest followed-recommendations-test
  (is (empty?
       (failures
        (stest/check
         `func/followed-recommendations
         {stc-opts
          {:num-tests 10 :max-size 3}})))))
;; helper fns

(deftest helpers-test
  (is (empty?
       (failures
        (stest/check
         [`func/get-func
          `func/get-func-args-spec
          `func/get-func-ret-spec
          `func/get-func-ret-spec-k
          ])))))

(deftest explain-args-test
  (is (empty?
       (failures
        (stest/check
         [`func/explain-args*
          `func/explain-args]
         {stc-opts
          {:num-tests 1 :max-size 1}})))))

(deftest apply-func-test
  (is (empty?
       (failures
        (stest/check
         `func/apply-func
         {stc-opts
          {:num-tests 1 :max-size 1}})))))
