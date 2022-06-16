(defproject borkdude/jet
  #=(clojure.string/trim
     #=(slurp "resources/JET_VERSION"))
  :description "jet"
  :url "https://github.com/borkdude/jet"
  :scm {:name "git"
        :url "https://github.com/borkdude/jet"}
  :license {:name "Eclipse Public License 1.0"
            :url "http://opensource.org/licenses/eclipse-1.0.php"}
  :source-paths ["src"]
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.cognitect/transit-clj "1.0.329"]
                 [cheshire "5.11.0"]
                 [mvxcvi/puget "1.3.2"]
                 [commons-io/commons-io "2.11.0"]
                 [org.babashka/sci "0.3.5"]
                 [camel-snake-kebab "0.4.3"]
                 [org.fusesource.jansi/jansi "2.4.0"]
                 [org.babashka/cli "0.2.17"]]
  :profiles {:clojure-1.9.0 {:dependencies [[org.clojure/clojure "1.11.1"]]}
             :clojure-1.10.3 {:dependencies [[org.clojure/clojure "1.11.1"]]}
             :test {:dependencies [[clj-commons/conch "0.9.2"]]}
             :uberjar {:dependencies [[com.github.clj-easy/graal-build-time "0.1.4"]]
                       :global-vars {*assert* false}
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dclojure.spec.skip-macros=true"]
                       :aot :all}}
  :main jet.main
  :aliases {"jet" ["run" "-m" "jet.main"]}
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_user
                                    :password :env/clojars_pass
                                    :sign-releases false}]])
