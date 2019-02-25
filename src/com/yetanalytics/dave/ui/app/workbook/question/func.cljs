(ns com.yetanalytics.dave.ui.app.workbook.question.func
  (:require [re-frame.core :as re-frame]
            [com.yetanalytics.dave.func :as func]
            [com.yetanalytics.dave.workbook.data.state :as state]))

(re-frame/reg-event-fx
 :workbook.question.function/set-func!
 (fn [{:keys [db] :as ctx} [_
                            workbook-id
                            question-id
                            func-id
                            ?args]]
   (let [init-state (:function (func/get-func func-id))
         new-db (assoc-in db
                          [:workbooks
                           workbook-id
                           :questions
                           question-id
                           :function]
                          {:id func-id
                           :func init-state
                           :state {:statement-idx -1}
                           :args (or ?args
                                     {})})]
     {:db new-db
      :dispatch-n [[:db/save]
                   [:com.yetanalytics.dave.ui.app.workbook.data/ensure
                    workbook-id]]})))

(re-frame/reg-event-fx
 :workbook.question.function/reset!
 (fn [{:keys [db] :as ctx} [_
                            workbook-id
                            question-id
                            func-id]]
   (let [init-state (:function (func/get-func func-id))]
     {:db (update-in db
                     [:workbooks
                      workbook-id
                      :questions
                      question-id
                      :function]
                     merge
                     ;; Carry over the args/result to keep graphs stable
                     {:id func-id
                      :func init-state
                      :state {:statement-idx -1}})})))

(re-frame/reg-event-fx
 :workbook.question.function/reset-all!
 (fn [{:keys [db] :as ctx}
      [_
       workbook-id
       then-dispatch]]
   {:dispatch-n
    (conj (into []
                (for [[question-id {{function-id :id} :function}]
                      (get-in db
                              [:workbooks
                               workbook-id
                               :questions])]
                  [:workbook.question.function/reset!
                   workbook-id
                   question-id
                   function-id]))
          then-dispatch)}))


;; Offer function picker
(re-frame/reg-event-fx
 :workbook.question.function/offer-picker
 (fn [{:keys [db] :as ctx} [_
                            workbook-id
                            question-id]]
   {:dispatch [:picker/offer
               {:title "Choose a DAVE Function"
                :choices (into []
                               (for [[id {:keys [title
                                                 doc]}] func/registry]
                                 {:label title
                                  :img-src "img/lambda.svg"
                                  :dispatch [:workbook.question.function/set-func!
                                             workbook-id
                                             question-id
                                             id]}))}]}))

;; Set an arg
(re-frame/reg-event-fx
 :workbook.question.function/set-arg!
 (fn [{:keys [db] :as ctx} [_
                            workbook-id
                            question-id
                            arg-key
                            arg-val :as call]]
   (let [new-db (assoc-in db
                          [:workbooks
                           workbook-id
                           :questions
                           question-id
                           :function
                           :args
                           arg-key]
                          arg-val)]
     {:db new-db
      :dispatch [:workbook.question.function/result!
                 workbook-id
                 question-id]
      })))

(re-frame/reg-event-fx
 :workbook.question.function/result!
 (fn [{:keys [db] :as ctx}
      [_
       workbook-id
       question-id]]
   (let [{{data-state :state} :data
          :as workbook} (get-in db [:workbooks
                                    workbook-id])
         {{func-state :state
           func-record :func
           func-args :args
           :as function} :function
          :as question} (get-in workbook
                                [:questions
                                 question-id])]
     (when (and data-state
                func-state
                (= data-state func-state))
       {:db (assoc-in db
                      [:workbooks
                       workbook-id
                       :questions
                       question-id
                       :function
                       :result]
                      (func/result func-record
                                   func-args))
        :dispatch [:db/save]}))))

(re-frame/reg-event-fx
 :workbook.question.function/result-all!
 (fn [{:keys [db]}
      [_ workbook-id]]
   {:dispatch-later
    (into []
          (for [[question-id _] (get-in db [:workbooks
                                            workbook-id
                                            :questions])]
            {:ms 0
             :dispatch
             [:workbook.question.function/result!
              workbook-id
              question-id]}))}))

;; Todo: Fit partial batches
(re-frame/reg-event-fx
 :workbook.question.function/step-all!
 (fn [{:keys [db] :as ctx}
      [_
       workbook-id
       [fidx lidx]
       statements
       then-dispatch
       phase ;; :find, :step :done
       steps
       ]]
   (let [phase (or phase :find)]
     (case phase
       :find
       (let [{:keys [questions] :as workbook}
             (get-in db [:workbooks workbook-id])
             steps (for [[question-id {{state :state
                               :as function} :function
                              :as question}] questions
                         :when (and function
                                    (state/accept? state
                                                   [fidx lidx]))
                         :let [[[fidx' lidx'] ss] (state/trim state
                                                              [fidx lidx]
                                                              statements)]]
                     {:question-id question-id
                      :index-range [fidx' lidx']
                      :statements ss})]
         {:dispatch-later [{:ms 0
                            :dispatch
                            [:workbook.question.function/step-all!
                             workbook-id
                             [fidx lidx]
                             statements
                             then-dispatch
                             :step
                             steps]}]})
       :step
       (if-let [{:keys [question-id]
                 ss :statements} (first steps)]
         (let [{:keys [data]
                :as workbook} (get-in db [:workbooks
                                          workbook-id])
               {func-id :id
                func-record :func
                func-state :state
                :as function} (get-in db [:workbooks
                                          workbook-id
                                          :questions
                                          question-id
                                          :function])
               relevant-ss (not-empty
                            (filter
                             (partial func/relevant?
                                      func-record)
                             ss))
               next-state (state/update-state
                           func-state statements)]
           #_(println next-state (:state data))
           {:db (assoc-in db
                          [:workbooks
                           workbook-id
                           :questions
                           question-id
                           :function]
                          (-> function
                              (assoc :state next-state)
                              (cond->
                                  relevant-ss
                                (assoc :func
                                       (reduce func/-step
                                               func-record
                                               relevant-ss)))))
            :dispatch-later
            [{:ms 0
              :dispatch [:workbook.question.function/step-all!
                         workbook-id
                         [fidx lidx]
                         statements
                         then-dispatch
                         :step
                         (rest steps)]}]})
         {:dispatch-later [{:ms 0
                            :dispatch
                            [:workbook.question.function/step-all!
                             workbook-id
                             [fidx lidx]
                             statements
                             then-dispatch
                             :done]}]})
       :done {:dispatch then-dispatch}))))

;; Subs
(re-frame/reg-sub
 :workbook.question/function
 (fn [[_ & args] _]
   (re-frame/subscribe (into [:workbook/question] args)))
 (fn [question _]
   (:function question)))

(defn function-sub-base
  [[_ & args] _]
  (re-frame/subscribe (into [:workbook.question/function] args)))

(re-frame/reg-sub
 :workbook.question.function/id
 function-sub-base
 (fn [function _]
   (:id function)))

(re-frame/reg-sub
 :workbook.question.function/args
 function-sub-base
 (fn [function _]
   (:args function)))

(re-frame/reg-sub
 :workbook.question.function/arg
 (fn [[_ workbook-id question-id arg-k] _]
   [(re-frame/subscribe [:workbook.question.function/args workbook-id question-id])
    (re-frame/subscribe [:workbook.question.function.func/args-default workbook-id question-id])])
 (fn [[args
       args-default] [_ _ _ arg-k]]
   (get (merge args-default
               args) arg-k)))

(re-frame/reg-sub
 :workbook.question.function/state
 function-sub-base
 (fn [function _]
   (:state function)))

(re-frame/reg-sub
 :workbook.question.function/result
 function-sub-base
 (fn [function _]
   (:result function)))

(re-frame/reg-sub
 :workbook.question.function/func
 (fn [[_ & args] _]
   (re-frame/subscribe (into [:workbook.question.function/id] args)))
 (fn [func-id _]
   (func/get-func func-id)))

(defn func-sub-base
  [[_ & args] _]
  (re-frame/subscribe (into [:workbook.question.function/func] args)))

(re-frame/reg-sub
 :workbook.question.function.func/title
 func-sub-base
 (fn [func _]
   (:title func)))

(re-frame/reg-sub
 :workbook.question.function.func/doc
 func-sub-base
 (fn [func _]
   (:doc func)))

(re-frame/reg-sub
 :workbook.question.function.func/args-enum
 func-sub-base
 (fn [func _]
   (:args-enum func)))

(re-frame/reg-sub
 :workbook.question.function.func/args-default
 func-sub-base
 (fn [func _]
   (:args-default func)))

(re-frame/reg-sub
 :workbook.question.function.result/count
 (fn [[_ ?workbook-id ?question-id] _]
   (re-frame/subscribe [:workbook.question.function/result ?workbook-id ?question-id]))
 (fn [{:keys [values]} _]
   (count values)))
