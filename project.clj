(defproject de.dixieflatline/confick "0.2.0"
  :description "Simple, stupid configuration management."
  :url "https://github.com/20centaurifux/confick"
  :license {:name "AGPLv3"
            :url "https://www.gnu.org/licenses/agpl-3.0"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/core.memoize "1.1.266"]
                 [environ "1.2.0"]]
  :target-path "target/%s"
  :profiles {:test {:env {:confick-path "test.edn"}}}
  :plugins [[dev.weavejester/lein-cljfmt "0.13.0"]
            [lein-environ "1.2.0"]
            [lein-codox "0.10.8"]]
  :cljfmt {:load-config-file? true}
  :codox {:output-path "./doc"})
