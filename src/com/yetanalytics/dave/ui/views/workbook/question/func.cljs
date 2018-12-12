(ns com.yetanalytics.dave.ui.views.workbook.question.func
  (:require [re-frame.core :refer [dispatch subscribe]]))

(defn info [?workbook-id ?question-id]
  [:div.function
   [:h3.title
    [:i.material-icons "functions"]
    "Function: " @(subscribe [:workbook.question.function.func/title
                              ?workbook-id ?question-id])]
   [:p.doc
    @(subscribe [:workbook.question.function.func/doc
                 ?workbook-id ?question-id])]])
