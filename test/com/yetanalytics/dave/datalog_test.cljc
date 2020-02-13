(ns com.yetanalytics.dave.datalog-test
  (:require
   [clojure.test #?(:cljs :refer-macros
                    :clj :refer) [deftest is testing]]
   [clojure.spec.alpha :as s]
   [xapi-schema.spec :as xs]
   [clojure.spec.test.alpha :as stest]
   clojure.test.check.generators
   [clojure.test.check :as tc]
   [clojure.test.check.properties :as prop :include-macros true]
   [clojure.test.check.clojure-test #?(:cljs :refer-macros
                                       :clj :refer) [defspec]]
   [com.yetanalytics.dave.datalog :as datalog]
   [com.yetanalytics.dave.datalog.schema :as schema]
   [datascript.core :as d]
   [com.yetanalytics.dave.test-support :refer [failures stc-opts read-json]]
   [clojure.pprint :refer [pprint]]))


;; known-good-statements to use in testing
(def statements
  (read-json "resources/public/data/dave/ds.json"))

(deftest statements-sanity-test
  (is (= 224 (count statements)))
  (is (s/valid? ::xs/statements statements)))

(deftest ->tx-test
  (let [tx (datalog/->tx schema/xapi statements)]
    (testing "tx expectations"
      (is (s/valid? (s/every map?)
                    tx))
      (is (= 224 (count tx))))
    (testing "static statements"
      (let [db (d/db-with (d/init-db [] schema/xapi) tx)]
        (is (d/db? db))
        (is (= 2395 (count (d/datoms db :eavt))))
        (is (= 224
                 (d/q '[:find (count-distinct ?s) .
                        :where
                        [?s :statement/id]]
                      db))))))
  #?(:clj (testing "generated statements"
            (is (empty?
                 (failures
                  (stest/check
                   `datalog/->tx
                   {stc-opts
                    {:num-tests 10 :max-size 3}
                    })))))))


#?(:clj (deftest idempotency-test
          (testing "transactions to datalog db are idempotent"
            (let [tx (datalog/->tx schema/xapi statements)
                  db-1 (-> (d/init-db [] schema/xapi)
                           (d/db-with tx))
                  db-1-datoms (d/datoms db-1 :eavt)
                  db-1-attr-freqs (frequencies (map :a db-1-datoms))
                  db-2 (-> db-1
                           (d/db-with tx))
                  db-2-datoms (d/datoms db-2 :eavt)
                  db-2-attr-freqs (frequencies (map :a db-2-datoms))
                  ]
              (is (= db-1
                     db-2))
              (is (= db-1-attr-freqs
                     db-2-attr-freqs))))
          (testing "even more so with transact"
            (let [db-1 (-> (d/init-db [] schema/xapi)
                           (datalog/transact statements))
                  db-1-datoms (d/datoms db-1 :eavt)
                  db-1-attr-freqs (frequencies (map :a db-1-datoms))
                  db-2 (-> db-1
                           (datalog/transact statements)) ;; should be a no-op
                  db-2-datoms (d/datoms db-2 :eavt)
                  db-2-attr-freqs (frequencies (map :a db-2-datoms))
                  ]
              (is (= db-1
                     db-2))
              (is (= db-1-attr-freqs
                     db-2-attr-freqs))))))

(deftest account-upsert-test
  (testing "some strange behaviours of agent/group accounts"
    (testing "agent"
      (let [statement {"id" "0a029ae8-bd87-441e-b879-a4c5da2d6722",
                       ;; actor is identified by account
                       "actor" {"account" {"homePage" "https://example.org"
                                           "name" "milt"}},
                       "verb" {"id" "aaa://aaa.aaa.aaa/aaa"},
                       "object" {"objectType" "Agent", "mbox" "mailto:aaa@aaa.aaa"},
                       "timestamp" "1970-01-01T00:00:00.0Z",
                       "stored" "1970-01-01T00:00:00.0Z",
                       "version" "1.0.0"}
            tx (datalog/->tx schema/xapi [statement])]
        (is (d/db? (-> (d/init-db [] schema/xapi)
                       (d/db-with tx)
                       (d/db-with tx))))))))

#?(:clj (defspec idempotency-gen-test 10
          (prop/for-all
           [ss (s/gen ::xs/lrs-statements)]
           (let [tx (datalog/->tx schema/xapi ss)

                 db-1 (-> (d/init-db [] schema/xapi)
                          (d/db-with tx))
                 db-1-datoms (d/datoms db-1 :eavt)
                 db-1-attr-freqs (frequencies (map :a db-1-datoms))
                 db-2 (-> db-1
                          (d/db-with tx))
                 db-2-datoms (d/datoms db-2 :eavt)
                 db-2-attr-freqs (frequencies (map :a db-2-datoms))
                 ]
             (is (= db-1
                    db-2))
             (is (= db-1-attr-freqs
                    db-2-attr-freqs))))))

#?(:clj (deftest transact-test
          (is (empty?
               (failures
                (stest/check
                 `datalog/transact
                 {stc-opts
                  {:num-tests 10 :max-size 3}
                  }))))))

(deftest empty-db-test
  (is (empty?
       (failures
        (stest/check
         `datalog/empty-db
         {stc-opts
          {:num-tests 10 :max-size 3}
          })))))
