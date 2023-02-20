(defproject zcfux/confick "0.1.4"
  :description "Simple, stupid configuration management."
  :url "https://github.com/20centaurifux/confick"
  :license {:name "AGPLv3"
            :url "https://www.gnu.org/licenses/agpl-3.0"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/core.memoize "1.0.257"]
                 [environ "1.2.0"]]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :test {:env {:edn-config-path "test.edn"}}}
  :plugins [[lein-cljfmt "0.9.2"]
            [lein-environ "1.2.0"]
            [lein-codox "0.10.8"]]
  :codox { :output-path "./doc" })
