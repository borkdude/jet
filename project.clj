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
                 [com.cognitect/transit-clj "1.0.333"]
                 [cheshire "5.11.0"]
                 [clj-commons/clj-yaml "1.0.26"]
                 [mvxcvi/puget "1.3.2"]
                 [commons-io/commons-io "2.11.0"]
                 [org.babashka/sci "0.8.41"]
                 [org.babashka/cli "0.8.58"]
                 [camel-snake-kebab "0.4.3"]
                 [com.rpl/specter "1.1.4"]
                 [rewrite-clj/rewrite-clj "1.1.47"]]
  :profiles {:test {:dependencies [[clj-commons/conch "0.9.2"]]}
             :uberjar {:dependencies [[com.github.clj-easy/graal-build-time "0.1.4"]]
                       :global-vars {*assert* false}
                       :jvm-opts [#_"-Dclojure.compiler.direct-linking=true"
                                  #_"-Dclojure.spec.skip-macros=true"]
                       :aot [jet.main]
                       :main jet.main}
             :native-image {:jvm-opts ["-Djet.native=true"]
                            :java-source-paths ["src-java"]}}
  :aliases {"jet" ["run" "-m" "jet.main"]}
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_user
                                    :password :env/clojars_pass
                                    :sign-releases false}]])
