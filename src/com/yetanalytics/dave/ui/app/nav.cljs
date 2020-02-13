(ns com.yetanalytics.dave.ui.app.nav
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
      (sgen/fmap (fn [[?w ?a #_?v]]
                   (str (when ?w
                          (format "/workbooks/%s" ?w))
                        (when ?a
                          (format "/analyses/%s" ?a))
                        #_(when ?v
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
      :type        #{:workbooks}
      :workbook-id uuid?
      :analyses    (s/?
                    (s/cat :type          #{:analyses}
                           :analysis-id   uuid?
                           :visualization (s/? (s/cat :type #{:visualizations}
                                          :visualization-id
                                          uuid?))))
      #_#_:questions
      (s/?
       (s/cat :type #{:questions}
              :question-id uuid?
              :visualizations (s/? (s/cat :type #{:visualizations}
                                          :visualization-id
                                          uuid?)))))))))

;; This enumerates all possible contexts in the app
(s/def ::context
  #{:loading
    :root
    :workbook
    :analysis
    #_#_:question
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
                (uuid token-part)))
            (remove empty?
                    (cs/split token #"/")))))

(s/fdef path->token
  :args (s/cat :path ::path)
  ;; TODO: make ret more detailed
  :ret ::token)

(defn path->token
  "Given a path vector, return a token string"
  [path]
  (str "/"
       (cs/join "/"
                (map (fn [x]
                       (if (keyword? x)
                         (name x)
                         (str x)))
                     path))))

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

(re-frame/reg-fx
 ::nav-path!
 (fn [path]
   (.replaceToken @history (path->token path))
   #_(nav! token)))

;; Passhthrough for navigating by vector path
(re-frame/reg-event-fx
 :nav/nav-path!
 (fn [_ [_ path]]
   {::nav-path!
    path}))

;; Receive events from the history API and dispatch accordingly
(re-frame/reg-event-fx
 :nav/dispatch
 (fn [{:keys [db] :as ctx} [_ token]]
   (let [token (or (not-empty token)
                   "/")
         [_ workbook-id
          _ question-id
          _ visualization-id
          :as path] (token->path token)]
     (if (and
          ;; is the path valid?
          (s/valid? ::path path)
          ;; is something at the path?
          (get-in db path))
       {:db (assoc db :nav {:token token
                            :path path})
        ;; Queue nav dispatch stuff
        :dispatch-n (cond-> []
                      workbook-id
                      (conj [:com.yetanalytics.dave.ui.app.workbook.data/ensure
                             workbook-id]))}
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
 :nav/path-ids
 (fn [_ _] (re-frame/subscribe [:nav/path]))
 (fn [path _]
   (take-nth 2 (drop 1 path))))

(re-frame/reg-sub
 :nav/path-items
 (fn [_ _]
   [(re-frame/subscribe [:dave/db])
    (re-frame/subscribe [:nav/path])])
 (fn [[db
       path] _]
   (:result (reduce (fn [{:keys [db result] :as m} sub-path]
                      (let [item (get-in db sub-path)]
                        (-> m
                            (update :result
                                    conj
                                    ;; remove children
                                    (dissoc item
                                            :analyses
                                            #_#_:questions
                                            :visualizations))
                            (assoc :db item))))
                    {:result []
                     :db db}
                    (partition 2 path)))))

(def singularize
  {:workbooks :workbook
   :analyses  :analysis})

(re-frame/reg-sub
 :nav/context
 (fn [_ _] (re-frame/subscribe [:nav/path]))
 (fn [path _]
   (case path
     nil :loading
     [] :root
     ;; singularized path resource
     (get singularize
          (last
           (filter #{:workbooks
                     :analyses
                     #_#_:questions
                     :visualizations}
                   path))))))

(re-frame/reg-sub
 :nav/focus
 (fn [_ _]
   [(re-frame/subscribe [:dave/db])
    (re-frame/subscribe [:nav/path])])
 (fn [[db path] _]
   (get-in db path)))

(re-frame/reg-sub
 :nav/focus-id
 (fn [_ _]
   (re-frame/subscribe [:nav/focus]))
 (fn [focus _]
   (:id focus)))

(re-frame/reg-sub
 :nav/focus-children
 (fn [_ _]
   [(re-frame/subscribe [:nav/focus])
    (re-frame/subscribe [:nav/context])])
 (fn [[focus context] _]
   (if-let [child-key (case context
                        :root     :workbooks
                        :workbook :analyses #_:questions
                        :analysis :editor
                        ;;:question :visualizations
                        nil)]
     (mapv second (get focus child-key))
     [])))
