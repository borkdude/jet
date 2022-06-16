(ns jet.patches
  "Don't load this namespace unless you're building a native image."
  (:require [com.rpl.specter.impl]
            [jet.specter :as js]))

;; (alter-var-root #'eval (constantly nil))
;; (alter-var-root #'require (constantly nil))
;; (alter-var-root #'resolve (constantly nil))
;; (alter-var-root #'requiring-resolve (constantly nil))

;; Note, when using direct linking we need to patch more vars than this
(alter-var-root #'com.rpl.specter.impl/closed-code (constantly js/closed-code))
(println "Patches applied")
