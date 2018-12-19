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
