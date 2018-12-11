(ns dave.tag
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.dave.util.spec :as su]))

;; Tags are just slug strings
(s/def ::tag
  slug-spec)

;; They are collected in sets so it is easier to combine them
;; no global registry or anything
(s/def ::tags
  (s/every ::tag
           :kind set?
           :into #{}))
