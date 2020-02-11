(ns com.yetanalytics.dave.test-support
  (:require
   [clojure.test :refer [deftest testing is] :include-macros true]
   [clojure.spec.alpha :as s :include-macros true]
   [clojure.spec.test.alpha :as stest :include-macros true]
   [clojure.walk :as w]
   [clojure.string :as cs]
   clojure.test.check
   clojure.test.check.properties
   #?@(:cljs [[cljs.nodejs :as node]
              ]
       :clj [[clojure.data.json :as json]
             [clojure.java.io :as io]])
   ))

#?(:clj (alias 'stc 'clojure.spec.test.check))

(def stc-ret :clojure.spec.test.check/ret

  ;; #?(:clj :clojure.spec.test.check/ret
  ;;    :cljs :clojure.test.check/ret)
  )

(def stc-opts :clojure.spec.test.check/opts

  ;; #?(:clj :clojure.spec.test.check/opts
  ;;    :cljs :clojure.test.check/opts)
  )

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


(defn read-json
  [path]
  #?(:clj (with-open [r (io/reader path)]
            (json/read r))
     :cljs (let [fs (node/require "fs")]
             (js->clj (.parse js/JSON
                              (.readFileSync fs path "utf8"))
                      :keywordize-keys false))))
