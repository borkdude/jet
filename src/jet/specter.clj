(ns jet.specter
  (:require
   [com.rpl.specter :as specter]
   [com.rpl.specter.impl :as i]
   [sci.core :as sci]))

;; copied from specter cli, might wanna move this to sci.configs

(def sns (sci/create-ns 'com.rpl.specter nil))
(def ins (sci/create-ns 'com.rpl.specter.impl nil))

(def sci-ctx (volatile! nil))
(def tmp-closure (sci/new-dynamic-var '*tmp-closure* ins))

(defn closed-code
  "Patch for closed-code which uses clojure.core/eval which isn't possible in native-image."
  [closure body]
  (let [lv (mapcat #(vector % `(i/*tmp-closure* '~%))
                   (keys closure))]
    (sci/binding [tmp-closure closure]
      (sci/eval-form @sci-ctx `(let [~@lv] ~body)))))

(when (System/getProperty "jet.native")
  (require 'jet.patches))

;; Private vars, used from path macro
(def ic-prepare-path #'specter/ic-prepare-path)
(def ic-possible-params #'specter/ic-possible-params)

(defmacro path
  "Patch for path macro which uses clojure.core/intern as a side effect,
  which is replaced by sci.core/intern here."
  [& path]
  (let [local-syms (-> &env keys set)
        used-locals (i/used-locals local-syms path)

        ;; note: very important to use riddley's macroexpand-all here, so that
        ;; &env is preserved in any potential nested calls to select (like via
        ;; a view function)
        expanded (i/clj-macroexpand-all (vec path))
        prepared-path (ic-prepare-path local-syms expanded)
        possible-params (vec (ic-possible-params expanded))

        cache-sym (vary-meta
                   (gensym "pathcache")
                   merge {:cljs.analyzer/no-resolve true :no-doc true :private true})

        info-sym (gensym "info")

        get-cache-code `(try (i/get-cell ~cache-sym)
                             (catch ClassCastException e#
                               ;; With AOT compilation it's possible for:
                               ;; Thread 1: unbound, so throw exception
                               ;; Thread 2: unbound, so throw exception
                               ;; Thread 1: do alter-var-root
                               ;; Thread 2: it's bound, so retrieve the current value
                               (if (bound? (var ~cache-sym))
                                 (i/get-cell ~cache-sym)
                                 (do
                                   (alter-var-root
                                    (var ~cache-sym)
                                    (fn [_#] (i/mutable-cell)))
                                   nil))))
        add-cache-code `(i/set-cell! ~cache-sym ~info-sym)
        precompiled-sym (gensym "precompiled")
        handle-params-code `(~precompiled-sym ~@used-locals)]
    ;; this is the actual patch
    (sci/intern @sci-ctx @sci/ns cache-sym (i/mutable-cell))
    ;; end patch
    `(let [info# ~get-cache-code

           info#
           (if (nil? info#)
             (let [~info-sym (i/magic-precompilation
                              ~prepared-path
                              ~(str *ns*)
                              (quote ~used-locals)
                              (quote ~possible-params))]
               ~add-cache-code
               ~info-sym)
             info#)

           ~precompiled-sym (i/cached-path-info-precompiled info#)
           dynamic?# (i/cached-path-info-dynamic? info#)]
       (if dynamic?#
         ~handle-params-code
         ~precompiled-sym))))

(def impl-ns (update-vals (ns-publics 'com.rpl.specter.impl) #(sci/copy-var* % ins)))
(def specter-ns (update-vals (ns-publics 'com.rpl.specter) #(sci/copy-var* % sns)))

(def config {:namespaces
             {'com.rpl.specter.impl
              (assoc impl-ns
                     '*tmp-closure* tmp-closure)
              'com.rpl.specter
              (assoc specter-ns
                     ;; the patched path macro
                     'path (sci/copy-var path sns))}
             :classes {'java.lang.ClassCastException ClassCastException
                       'clojure.lang.Util clojure.lang.Util}})
