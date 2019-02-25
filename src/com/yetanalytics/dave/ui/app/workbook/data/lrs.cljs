(ns com.yetanalytics.dave.ui.app.workbook.data.lrs
  "Re-frame LRS client"
  (:require [re-frame.core :as re-frame]
            [com.yetanalytics.dave.workbook.data.state :as state]
            [com.yetanalytics.dave.workbook.data.lrs.client :as client]
            [com.yetanalytics.dave.util.log :as log]))

;; ingress
(re-frame/reg-event-fx
 ::query
 (fn [{:keys [db] :as ctx}
      [_
       workbook-id
       {xapi-query :query
        :keys [endpoint
               auth
               more]
        lrs-state :state
        :as lrs-spec}
       {:keys [retry]
        :or {retry {:error-codes
                    ;; default to retrying on transient/gateway errors
                    #{408 420 429
                      502 503 504}
                    :wait 200
                    :wait-min 200
                    :wait-max 5000
                    :wait-rate 1.5
                    :tries 0
                    :tries-max 10}}
        :as options}]]
   (let [fresh-lrs? (= lrs-state
                       {:statement-idx -1})
         state (if fresh-lrs?
                 lrs-state
                 (apply min-key
                        :statement-idx
                        (remove nil?
                                (cons lrs-state
                                      (for [[_ {{:keys [state]} :function}]
                                            (get-in db [:workbooks
                                                        workbook-id
                                                        :questions])]
                                        state)))))
         ?since (get-in state [:stored-domain 1])]
     (log/debugf "Query lrs: %s state: %s" lrs-state state)
     {:http/request
      {:request {:url (str endpoint
                           "/xapi/statements")
                 :headers {"X-Experience-Api-Version" "1.0.3"}
                 :basic-auth (select-keys
                              auth
                              [:username :password])
                 :with-credentials? false
                 :method :get
                 :query-params (merge
                                (when ?since
                                  {"since" ?since})
                                {"ascending" true})}
       :handler
       [::result
        workbook-id
        state
        lrs-spec
        options]
       :error-handler
       [::error
        workbook-id
        state
        lrs-spec
        options]}})))

;; Error handling
(re-frame/reg-event-fx
 ::error
 (fn [{:keys [db] :as ctx}
      [_
       _
       _
       _
       _
       e]]
   (.error js/console (pr-str (ex-data e)))))

;; Result handling
(re-frame/reg-event-fx
 ::result
 (fn [{:keys [db] :as ctx}
      [_
       workbook-id
       {:keys [statement-idx] :as state}
       {xapi-query :query
        :keys [endpoint
               auth
               more]
        :as lrs-spec}
       options
       {:keys [status body]
        :as response}]]
   (if (= status 200)
     (do
       (log/debugf "LRS query resp statements: %d" (count (get body "statements")))
       (if (not-empty (get body "statements"))
         (let [new-state (state/update-state state (get body "statements"))
               s-idx-range [(inc statement-idx)
                            (:statement-idx new-state)]]
           (log/debugf "Response old: %s new: %s idx-range: %s"
                     state new-state s-idx-range)
           {:dispatch [:com.yetanalytics.dave.ui.app.workbook.data/load
                       workbook-id
                       s-idx-range
                       {:status 200 :body (get body "statements")}
                       (if-let [more (get body "more")]
                         [::continue
                          workbook-id
                          new-state
                          lrs-spec
                          (update options :retry client/reset-retry)
                          more]
                         [::shutdown
                          workbook-id
                          new-state])]})
         {:dispatch [::shutdown
                     workbook-id
                     state
                     lrs-spec
                     options]}))
     {:dispatch [::error
                 workbook-id
                 state
                 lrs-spec
                 options
                 (ex-info "LRS Request Error"
                          {:type ::lrs-request-error
                           :status status
                           :response response})]})))

;; continue w/more link
(re-frame/reg-event-fx
 ::continue
 (fn [{:keys [db] :as ctx}
      [_
       workbook-id
       {:keys [statement-idx] :as state}
       {xapi-query :query
        :keys [endpoint
               auth
               more]
        lrs-state :state
        :as lrs-spec}
       options
       more-link]]
   (log/debugf "Continue state: %s more: %s"
             state more-link)
   {:http/request
    {:request {:url (str endpoint
                         more-link)
               :headers {"X-Experience-Api-Version" "1.0.3"}
               :basic-auth (select-keys
                            auth
                            [:username :password])
               :with-credentials? false
               :method :get}
     :handler
     [::result
      workbook-id
      state
      lrs-spec
      options]
     :error-handler
     [::error
      workbook-id
      state
      lrs-spec
      options]}}
   ))

;; end, trigger renders?
(re-frame/reg-event-fx
 ::shutdown
 (fn [{:keys [db] :as ctx}
      [_
       workbook-id
       state
       _
       _]]
   (log/debugf "Shutdown: %s" state)
   {:dispatch [:com.yetanalytics.dave.ui.app.workbook.data/set-state
               workbook-id
               state]}))
