(ns com.yetanalytics.dave.workbook.data.file
  (:require
   [clojure.spec.alpha :as s]
   [com.yetanalytics.dave.util.spec :as su]))

(s/def ::uri
  (s/and string?
         not-empty))

;; If this is a dataset included with DAVE, it is expected to be there and
;; doesn't need to be saved.
(s/def ::built-in?
  boolean?)

(def partial-spec
  (s/keys :req-un [::uri]
          :opt-un [::built-in?]))
