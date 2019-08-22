(ns dave.sass
  (:require ["node-sass" :refer [render
                                 renderSync
                                 watcher]]
            ["gaze" :as gaze]
            ["fs" :refer [writeFile]]))

#_(enable-console-print!)

(defn render-dev! []
  (render #js {:file "resources/dave/ui/sass/style.scss"
               :outFile "resources/public/css/style.css"
               :sourceMap true
               :includePaths #js ["resources" "node_modules"]}
          (fn [?error result]
            (if ?error
              (.error
               js/console
               "Sass compilation error!"
               ?error)
              (do
                (writeFile "resources/public/css/style.css"
                           (.-css result)
                           (fn [?error]
                             (if ?error
                               (.error
                                js/console
                                "Error writing CSS file!"
                                ?error)
                               "CSS File written to disk.")))
                (writeFile "resources/public/css/style.css.map"
                           (.-map result)
                           (fn [?error]
                             (if ?error
                               (.error
                                js/console
                                "Error writing CSS source map!"
                                ?error)
                               "Source map File written to disk."))))))))

(defn render-prod! []
  (render #js {:file "resources/dave/ui/sass/style.scss"
               :outFile "resources/public/css/style.css"
               :sourceMap false
               :outputStyle "compressed"
               :includePaths #js ["resources" "node_modules"]}
          (fn [?error result]
            (if ?error
              (.error
               js/console
               "Sass compilation error!"
               ?error)
              (writeFile "resources/public/css/style.css"
                         (.-css result)
                         (fn [?error]
                           (if ?error
                             (.error
                              js/console
                              "Error writing CSS file!"
                              ?error)
                             "CSS File written to disk.")))))))

(goog-define ^boolean DEV_SASS false)

(defonce watch
  (when DEV_SASS
    (render-dev!)
    (let [watcher (gaze "resources/dave/ui/sass/**/*")]
      (doto watcher
        (.on "all"
             (fn [_ _]
               (render-dev!)))))))
