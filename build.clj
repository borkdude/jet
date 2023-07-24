(ns build
  (:require [clojure.tools.build.api :as b]))

(def build-folder "target")
(def jar-content (str build-folder "/classes"))

(def native-basis (b/create-basis {:project "deps.edn"
                                   :aliases [:native]}))
(def basis (b/create-basis {:project "deps.edn"
                            :aliases []}))
(def app-name "jet")
(def uber-file-name (format "%s/%s-standalone.jar" build-folder app-name)) ; path for result uber file

(defn clean [_]
  (b/delete {:path "target"})
  (println (format "Build folder \"%s\" removed" build-folder)))

(defn uber [_]
  #_(clean nil)

  (b/copy-dir {:src-dirs   ["resources"]         ; copy resources
               :target-dir jar-content})

  (b/javac {:src-dirs ["src-java"]
            :class-dir jar-content
            :basis native-basis
            #_#_:javac-opts ["-Djet.native=true"]})

  (b/compile-clj {:basis     basis               ; compile clojure code
                  :src-dirs  ["src"]
                  :class-dir jar-content})

  (b/uber {:class-dir jar-content                ; create uber file
           :uber-file uber-file-name
           :basis     basis
           :main      'jet.main})                ; here we specify the entry point for uberjar

  (println (format "Uber file created: \"%s\"" uber-file-name)))
