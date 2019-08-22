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
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.cognitect/transit-clj "0.8.313"]
                 [cheshire "5.9.0"]
                 [fipp "0.6.18"]
                 [commons-io/commons-io "2.6"]
                 [borkdude/sci "0.0.7"]]
  :profiles {:clojure-1.9.0 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :clojure-1.10.1 {:dependencies [[org.clojure/clojure "1.10.1"]]}
             :test {:dependencies [[clj-commons/conch "0.9.2"]]}
             :uberjar {:global-vars {*assert* false}
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dclojure.spec.skip-macros=true"]
                       :main jet.main
                       :aot :all}}
  :aliases {"jet" ["run" "-m" "jet.main"]}
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_user
                                    :password :env/clojars_pass
                                    :sign-releases false}]])
