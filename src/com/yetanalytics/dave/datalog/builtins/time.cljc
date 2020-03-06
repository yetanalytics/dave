(ns com.yetanalytics.dave.datalog.builtins.time
  "Built-ins for time"
  (:require [#?(:clj clj-time.core
                :cljs cljs-time.core) :as t]
            [#?(:clj clj-time.format
                :cljs cljs-time.format) :as tf]
            [#?(:clj clj-time.coerce
                :cljs cljs-time.coerce) :as tc]))

;; TODO: replace with more modern time APIs... cljc.java-time + Tick?

(defn tformat
  [fmt-str date]
  (try (tf/unparse
        (tf/formatter fmt-str)
        (tc/to-date-time date))
       (catch #?(:clj Exception
                 :cljs js/Error) e
         (throw (ex-info "Timestamp Format Error!"
                         {:type ::date-format-error}
                         e)))))

(comment

  (tformat "e"
          #inst "2018-12-19T13:58:29.000-00:00")


  (tf/unparse
   (tf/formatter (tf/formatter "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"))
   (tc/to-date-time #inst "2018-12-19T13:58:29.000-00:00"))
  )
