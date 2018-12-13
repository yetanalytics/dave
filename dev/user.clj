(ns user)

(comment

  (require '[clj-http.client :as client]
           '[cheshire.core :as json]
           '[clojure.java.io :as io])


  (do (def kokea-endpoint "https://demo-lrs.yetanalytics.io/xapi/statements")
      (def kokea-api-key "26589566c610a046c322f87cfcc1383032d09d0774d20b68")
      (def kokea-api-key-secret "d50f0b8711acc82793ed040d005f41ed571e2a1e69cf975339aa784d50ea8d1c")
      (def viewed-verb-id "https://xapinet.org/kokea-concepts/verbs/viewed")
      (def page-obj-id "https://xapinet.org/kokea-concepts/activities/Page-1-Listening-to-the-Customer")
      (def query-params {:verb viewed-verb-id
                         :activity page-obj-id})

      (defn query-lrs
        [{:keys [endpoint api-key api-key-secret query-params]}]
        (clj-http.client/get
         endpoint
         {:basic-auth [api-key api-key-secret]
          :headers {"X-Experience-API-Version" "1.0.3"
                    "Content-Type" "application/json;"}
          :query-params query-params})))


  (def rate-of-completions-statements
    (-> {:endpoint kokea-endpoint
         :api-key kokea-api-key
         :api-key-secret kokea-api-key-secret
         :query-params {:verb "https://xapinet.org/kokea-concepts/verbs/completed"}}
        query-lrs
        :body
        (cheshire.core/parse-string true)
        :statements))

  (with-open [w (io/writer (io/file "resources/public/data/kokea/rate_of_completions_16.json"))]
    (json/generate-stream rate-of-completions-statements w))

  (clojure.pprint/pprint rate-of-completions-statements)


 (defn rate-of-completions
   [stmts]
   (let [c-per-obj (loop [accum {}
                          src stmts]
                     (if (empty? src)
                       accum
                       (let [cur-stmt (first src)
                             {{{{obj-name :en-US} :name} :definition
                                obj-id :id} :object} cur-stmt]
                         (recur
                          (update-in accum [obj-id]
                                     (fn [old]
                                       (if (nil? old)
                                         {:name obj-name
                                          :count 1}
                                         (let [{c :count} old
                                               updated-c (inc c)]
                                           {:name obj-name
                                            :count updated-c}))))
                          (rest src)))))]
     (loop [accum []
            src c-per-obj]
       (if (empty? src)
         accum
         (let [[obj-id obj-info] (first src)
               {obj-n :name
                obj-c :count} obj-info]
           (recur (conj accum [obj-id obj-n obj-c]) (rest src)))))))

 (count (rate-of-completions rate-of-completions-statements)))
