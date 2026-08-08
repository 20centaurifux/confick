(defproject de.dixieflatline/confick "0.3.0-SNAPSHOT"
  :description "Simple, stupid configuration management."
  :url "https://github.com/20centaurifux/confick"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/core.memoize "1.2.281"]
                 [environ "1.2.0"]]
  :target-path "target/%s"
  :profiles {:test {:env {:confick-path "test/resources/config.edn"}}}
  :plugins [[dev.weavejester/lein-cljfmt "0.16.5"]
            [lein-environ "1.2.0"]
            [lein-codox "0.10.8"]]
  :cljfmt {:load-config-file? true}
  :codox {:output-path "./doc"})