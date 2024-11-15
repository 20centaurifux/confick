# confick

confick is a tiny library to load and cache configuration data from an [edn](https://github.com/edn-format/edn) file.

## Installation

The library can be installed from Clojars:

[![Clojars Project](https://img.shields.io/clojars/v/de.dixieflatline/confick.svg?include_prereleases)](https://clojars.org/de.dixieflatline/confick)

## Example

	(require '[confick.core :refer [bind lookup]])

	;; receive configuration value
	(lookup [:tcp :address] :required true)

	;; bind configuration values in a let block
	(bind [^:required addr [:tcp :address]
	       ^{:default 80 :conform nat-int?} port [:tcp :port]]
	  (println (format "%s:%d" addr port)))

	;; access configuration values in EDN
	(require '[confick.edn :as edn])

	(edn/read-string "{:address #cnf/req [:tcp port] :port #cnf/or [[:tcp :port] 80]}")

## Configuration

The default relative path of the configuration file is \"config.edn\". It gets
overwritten by the CONFICK_PATH environment variable or Java system property.

Set CONFICK_CACHE_MILLIS to zero to disable caching.