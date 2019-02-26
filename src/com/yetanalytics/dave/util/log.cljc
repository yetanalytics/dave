(ns com.yetanalytics.dave.util.log
  "Attempt at consistent logging between CLJ/CLJS.
  Adapted from https://gist.github.com/caskolkm/39d823f5bac7051d3062"
  #?@(:clj
      [(:require [net.cgrand.macrovich :as macros]
                 [clojure.tools.logging :as ctl])]
     :cljs
      [(:require goog.log
                 goog.string
                 goog.string.format)
       (:import goog.debug.Console
                goog.debug.LogManager
                goog.debug.Logger)
       (:require-macros [net.cgrand.macrovich :as macros]
                        [com.yetanalytics.dave.util.log
                         :refer [log
                                 logp
                                 logf
                                 spy
                                 spyf
                                 trace
                                 debug
                                 info
                                 warn
                                 error
                                 fatal
                                 tracef
                                 debugf
                                 infof
                                 warnf
                                 errorf
                                 fatalf]])]))

#?(:cljs
   (defn pfmt [& msgs]
     (apply str (interpose " " (map pr-str msgs)))))

#?(:cljs (def levels
           "Translation between tools.logging and goog.log"
           {:fatal goog.debug.Logger.Level.SEVERE
            :error goog.debug.Logger.Level.SEVERE
            :warn goog.debug.Logger.Level.WARNING
            :info goog.debug.Logger.Level.INFO
            :debug goog.debug.Logger.Level.FINE
            :trace goog.debug.Logger.Level.FINEST}))

#?(:cljs (defn level->goog
           [level]
           (get levels
                level
                ;; default
                (:info levels))))

(macros/deftime

  (defmacro ns-logger
    "Generate code to retrieve or create a logger for the current cljs ns"
    [& [default]]
    `(goog.log.getLogger
      ~(or (some-> &env
                   :ns
                   :name
                   name)
           ~(or default "unknown"))))

  (defmacro log
    "Log a single message."
    [level msg]
    `(macros/case
       :clj (ctl/log ~level
                     ~msg)
       :cljs
       (goog.log.log
        (ns-logger)
        (level->goog ~level)
        ~msg)))

  (defmacro logp
    "Log an arbitrary list of messages/values"
    [level & msgs]
    `(macros/case
         :clj (ctl/logp ~level
                        ~@msgs)
         :cljs
         (goog.log.log
          (ns-logger)
          (level->goog ~level)
          (pfmt ~@msgs))))

  (defmacro logf
    "Log with a format string and a list of values."
    [level fmt-str & more]
    `(macros/case
         :clj (ctl/logf ~level
                        ~fmt-str
                        ~@more)
         :cljs
         (goog.log.log
          (ns-logger)
          (level->goog ~level)
          (goog.string.format ~fmt-str ~@more))))

  ;; Limited spy
  (defmacro spy
    "Evaluates expr and may write the form and its result to the log. Returns the
    result of expr. Defaults to :debug log level."
    ([expr]
     `(spy :debug ~expr))
    ([level expr]
     `(let [a# ~expr]
        (log ~level
             (str (pr-str '~expr)
                  " => "
                  (pr-str a#)))
        a#)))

  (defmacro spyf
    "Evaluates expr and may write (format fmt expr result) to the log. Returns the
    result of expr. Defaults to :debug log level."
    ([fmt expr]
     `(spyf :debug ~fmt ~expr))
    ([level fmt expr]
     `(let [a# ~expr]
        (logf ~level ~fmt '~expr a#)
        a#)))


  ;; Convenience Macros
  ;; see https://github.com/clojure/tools.logging/blob/master/src/main/clojure/clojure/tools/logging.clj
  (defmacro trace
    "Trace level logging using print-style args."
    [& args]
    `(logp :trace ~@args))

  (defmacro debug
    "Debug level logging using print-style args."
    [& args]
    `(logp :debug ~@args))

  (defmacro info
    "Info level logging using print-style args."
    [& args]
    `(logp :info ~@args))

  (defmacro warn
    "Warn level logging using print-style args."
    [& args]
    `(logp :warn ~@args))

  (defmacro error
    "Error level logging using print-style args."
    [& args]
    `(logp :error ~@args))

  (defmacro fatal
    "Fatal level logging using print-style args."
    [& args]
    `(logp :fatal ~@args))

  (defmacro tracef
    "Trace level logging using format."
    [& args]
    `(logf :trace ~@args))

  (defmacro debugf
    "Debug level logging using format."
    [& args]
    `(logf :debug ~@args))

  (defmacro infof
    "Info level logging using format."
    [& args]
    `(logf :info ~@args))

  (defmacro warnf
    "Warn level logging using format."
    [& args]
    `(logf :warn ~@args))

  (defmacro errorf
    "Error level logging using format."
    [& args]
    `(logf :error ~@args))

  (defmacro fatalf
    "Fatal level logging using format."
    [& args]
    `(logf :fatal ~@args))
  )

#?(:cljs
   (defn set-level!
     ([level]
      (.setLevel ^goog.debug.Logger (.getRoot goog.debug.LogManager)
                 (get levels level (:info levels))))
     ([logger-name level]
      (.setLevel ^goog.debug.Logger (.getLogger goog.debug.LogManager
                                                logger-name)
                 (get levels level (:info levels))))))


(comment
  ;; Log just takes level/string
  (log :info "I feel great")
  (log :warn "I don't feel so good")

  ;; Logp can take & args
  (logp :info "I" "feel" "great")
  (logp :warn "I" "don't" "feel" "so" "good")

  ;; Logf formats
  (logf :warn "The number %d" 1)

  ;; Levels are manipulatable in cljs...
  (log :debug "debug!") ;; => nothing, by default
  ;; Globally set
  (set-level! :debug)
  (log :debug "debug!") ;; => something
  ;; Set for a specific NS, other named thing
  (set-level! "com.yetanalytics.dave.util.log" :info)

  (spy :info (+ 1 1))
  (spyf :info "%s => %s" (+ 1 1))
  )
