(ns com.yetanalytics.dave.ui.events.nav
  (:require [re-frame.core :as re-frame]
            [goog.events :as events]
            [clojure.string :as cs]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as sgen]
            [goog.string :as gstring :refer [format]]
            [goog.string.format])
  (:import [goog.history Html5History EventType]))


;; a la https://lispcast.com/mastering-client-side-routing-with-secretary-and-goog-history/

(defn make-history []
  (doto (Html5History.)
    ;; for SPA use
    #_(.setPathPrefix (str js/window.location.protocol
                           "//"
                           js/window.location.host))
    #_(.setUseFragment false)))

(defonce history
  #_(make-history)
  (delay
   (doto (make-history)
     (events/listen EventType.NAVIGATE
                    (fn [x]
                      (re-frame/dispatch [:nav/dispatch (.-token x)]))))))

(defn get-token []
  (.getToken @history))

(defn nav! [token]
  (.setToken @history token))

(s/def ::token
  (s/with-gen string?
    (fn []
      (sgen/fmap (fn [[?w ?q ?v]]
                   (str (when ?w
                          (format "/workbooks/%s" ?w))
                        (when ?q
                          (format "/questions/%s" ?q))
                        (when ?v
                          (format "/visualizations/%s" ?v))))
                 (sgen/vector (sgen/string-alphanumeric) 0 3)))))

;; Master spec for all paths in App
;; TODO: plug in actual ID specs
(s/def ::path
  (s/and
   vector?
   (s/cat
    :workbooks
    (s/?
     (s/cat
      :type #{:workbooks}
      :workbook-id string?
      :questions
      (s/?
       (s/cat :type #{:questions}
              :question-id string?
              :visualizations (s/? (s/cat :type #{:visualizations}
                                          :visualization-id
                                          string?)))))))))

;; This enumerates all possible contexts in the app
(s/def ::context
  #{:loading
    :root
    :workbook
    :question
    :visualization})

(def nav-spec
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
;; Handlers

(re-frame/reg-cofx
 ::token
 (fn [cofx]
   (assoc cofx :token (get-token))))

(re-frame/reg-event-fx
 :nav/init
 (fn [_ _]
   {::listen true}))

(re-frame/reg-fx
 ::listen
 (fn [listen?]
   (.setEnabled @history listen?)))

(re-frame/reg-fx
 ::nav!
 (fn [token]
   (.replaceToken @history "/")
   #_(nav! token)))

;; Receive events from the history API and dispatch accordingly
(re-frame/reg-event-fx
 :nav/dispatch
 (fn [{:keys [db] :as ctx} [_ token]]
   (let [token (or (not-empty token)
                   "/")
         path (token->path token)]
     (if (and
          ;; is the path valid?
          (s/valid? ::path path)
          ;; is something at the path?
          (get-in db path))
       {:db (assoc db :nav {:token token
                            :path path})}
       {:notify/snackbar
        {:message "Page not found!"
         :timeout 1000}
        ::nav! ""}))))

;; Subs
(re-frame/reg-sub
 :nav/state
 (fn [db _]
   (:nav db)))

(re-frame/reg-sub
 :nav/path
 (fn [_ _] (re-frame/subscribe [:nav/state]))
 (fn [state _]
   (:path state)))

(re-frame/reg-sub
 :nav/context
 (fn [_ _] (re-frame/subscribe [:nav/path]))
 (fn [path _]
   (case path
     nil :loading
     [] :root
     ;; singularized path resource
     (apply str
            (butlast
             (last
              (filter #{:workbooks
                        :questions
                        :visualizations}
                      path)))))))
