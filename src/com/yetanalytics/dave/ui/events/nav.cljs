(ns com.yetanalytics.dave.ui.events.nav
  (:require [re-frame.core :as re-frame]
            [goog.events :as events]
            [clojure.string :as cs]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as sgen]
            [goog.string :as gstring :refer [format]]
            [goog.string.format])
  (:import [goog.history Html5History EventType]))

(defn get-token []
  (str js/window.location.pathname js/window.location.search))


(declare history)

(defn nav! [token]
  (.setToken history token))

(s/def ::token
  (s/with-gen string?
    (fn []
      (sgen/fmap (fn [[?w ?q ?v]]
                   (str (when ?w
                          (format "/workbook/%s" ?w))
                        (when ?q
                          (format "/question/%s" ?q))
                        (when ?v
                          (format "/vis/%s" ?v))))
                 (sgen/vector (sgen/string-alphanumeric) 0 3)))))

;; Master spec for all paths in App
;; TODO: plug in actual ID specs
(s/def ::path
  (s/and vector?
         (s/cat :workbook
                (s/? (s/cat :type #{:workbook}
                            :workbook-id string?
                            :question (s/? (s/cat :type #{:question}
                                                  :question-id string?
                                                  :vis (s/? (s/cat :type #{:vis}
                                                                   :vis-id
                                                                   string?)))))))))

(s/def ::state
  (s/keys :opt-un [::token
                   ::path]))


(s/fdef token->path
    :args (s/cat :token ::token)
    ;; TODO: make ret more detailed
    :ret ::path)

(defn token->path
  "Given a token, return a path vector"
  [token]
  (into [] (map-indexed
            (fn [idx token-part]
              (if (even? idx)
                (keyword token-part)
                token-part))
            (remove empty?
                    (cs/split token #"/")))))

;; Receive events from the history API and dispatch accordingly
(re-frame/reg-event-fx
 ::dispatch
 (fn [{:keys [db] :as ctx} [_ token]]
   (.log js/console "dispatch" token)
   {:db (assoc db ::state {:token token
                           :path (token->path token)})}))

;; a la https://lispcast.com/mastering-client-side-routing-with-secretary-and-goog-history/

(defn make-history []
  (doto (Html5History.)
    ;; for SPA use
    #_(.setPathPrefix (str js/window.location.protocol
                           "//"
                           js/window.location.host))
    #_(.setUseFragment false)))

(defonce history
  (doto (make-history)
    (events/listen EventType.NAVIGATE (fn [x] (re-frame/dispatch [::dispatch (.-token x)])))
    (.setEnabled true)))
