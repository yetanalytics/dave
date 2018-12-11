(ns dave.util
  "Global utilities for DAVE"
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as cs]
            [com.yetanalytics.dave.util.spec :as su]))

(s/fdef slugify
  :args (s/cat :s (s/and string? not-empty))
  :ret su/slug-spec)

(defn slugify
  "Given a non-empty string, ensure it only has lower-case letters and dashes."
  [s]
  (-> s
      cs/lower-case
      (cs/replace #"[^a-z0-9\-]" "-")))
