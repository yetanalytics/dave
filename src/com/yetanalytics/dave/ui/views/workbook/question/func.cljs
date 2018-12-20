(ns com.yetanalytics.dave.ui.views.workbook.question.func
  (:require [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.dave.ui.views.form.select
             :as select]))


(defn args [workbook-id question-id]
  (into [:div.args]
        (for [[k enum] @(subscribe [:workbook.question.function.func/args-enum
                                    workbook-id question-id])]
          [select/select
           :label (name k)
           :options (into []
                          (for [v enum]
                            {:label (name v)
                             :value (name v)}))
           :selected
           @(subscribe [:workbook.question.function/arg
                        workbook-id question-id k])
           :handler #(-> %
                         keyword
                         (->> (conj [:workbook.question.function/set-arg!
                                     workbook-id question-id
                                     k]))
                         dispatch)])))


(defn info [workbook-id question-id]
  [:div.function
   [:h3.title
    [:img.title-img {:src "/img/lambda.svg"}]
    "Function: " @(subscribe [:workbook.question.function.func/title
                              workbook-id question-id])
    [:button.minorbutton
     {:on-click #(dispatch [:workbook.question.function/offer-picker
                            workbook-id question-id])}
     "Change Function"]]
   [:p.doc
    @(subscribe [:workbook.question.function.func/doc
                 workbook-id question-id])]

   #_(str @(subscribe [:workbook.question.function/result
                     ?workbook-id
                       ?question-id]))
   #_(str @(subscribe [:workbook.question.function/func
                     ?workbook-id
                     ?question-id]))
   [args workbook-id question-id]
   [:div (str @(subscribe [:workbook.question.function.result/count
                           workbook-id
                           question-id])
              " results with current dataset")]])
