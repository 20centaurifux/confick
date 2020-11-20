(defproject confick "0.1.0-SNAPSHOT"
  :description "Simple, stupid configuration management."
  :url "https://github.com/20centaurifux/goophi"
  :license {:name "AGPLv3"
            :url "https://www.gnu.org/licenses/agpl-3.0"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.memoize "1.0.236"]
                 [environ "1.2.0"]]
  :main ^:skip-aot confick.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :test {:env {:edn-config-path "test.edn"}}}
  :plugins [[lein-cljfmt "0.6.7"]
            [lein-environ "1.2.0"]])
