#!/usr/bin/env bb

(ns bump-version
  (:require [babashka.process :as p]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def version-file (io/file "resources" "JET_VERSION"))
(def released-version-file (io/file "resources" "JET_RELEASED_VERSION"))

(case (first *command-line-args*)
  "release" (let [version-string (str/trim (slurp version-file))
                  [major minor patch] (str/split version-string #"\.")
                  patch (str/replace patch "-SNAPSHOT" "")
                  new-version (str/join "." [major minor patch])]
              (spit version-file new-version)
              (-> (p/process ["git" "commit" "-a" "-m" (str "v" new-version)] {:inherit true})
                  (p/check))
              (-> (p/process ["git" "diff" "HEAD^" "HEAD"] {:inherit true})
                  (p/check))
              nil)
  "post-release" (do
                   (io/copy version-file released-version-file)
                   (let [version-string (str/trim (slurp version-file))
                         [major minor patch] (str/split version-string #"\.")
                         patch (Integer. patch)
                         patch (str (inc patch) "-SNAPSHOT")
                         new-version (str/join "." [major minor patch])]
                     (spit version-file new-version)
                     (-> (p/process ["git" "commit" "-a" "-m" "Version bump"])
                         (p/check))
                     (-> (p/process ["git" "diff" "HEAD^" "HEAD"])
                         (p/check))
                     nil))
  (println "Expected: release | post-release."))
