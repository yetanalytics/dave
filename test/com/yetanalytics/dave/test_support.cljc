(ns com.yetanalytics.dave.test-support
  (:require
   [clojure.test :refer [deftest testing is] :include-macros true]
   [clojure.spec.alpha :as s :include-macros true]
   [clojure.spec.test.alpha :as stest :include-macros true]
   [clojure.walk :as w]
   [clojure.string :as cs]
   clojure.test.check
   ))

#?(:clj (alias 'stc 'clojure.spec.test.check))

(def stc-ret #?(:clj :clojure.spec.test.check/ret
                :cljs :clojure.test.check/ret))

(def stc-opts #?(:clj :clojure.spec.test.check/opts
                 :cljs :clojure.test.check/opts))

(defn failures
  "Get any failing results of stest/check"
  [check-results]
  (mapv
   (fn [{:keys [spec clojure.spec.test.check/ret sym failure] :as x}]
     [sym
      (-> x
          (update :spec s/describe)
          (dissoc :sym)
          ;; Dissoc the top level trace, leave the shrunken one
          (update stc-ret dissoc :result-data))])
   (remove #(-> %
                stc-ret
                :result
                true?)
           check-results)))
