# confick

confick is a tiny library to load and cache configuration data from an [EDN](https://github.com/edn-format/edn) file.

## Installation

The library can be installed from Clojars:

[![Clojars Project](https://img.shields.io/clojars/v/de.dixieflatline/confick.svg?include_prereleases)](https://clojars.org/de.dixieflatline/confick)

## Example

	(require '[confick.core :refer [bind lookup]])

	;; receive configuration value
	(lookup [:tcp :address] :required true)

	;; bind configuration data to vars
	(bind [^:required addr [:tcp :address]
	       ^{:default 80 :conform nat-int?} port [:tcp :port]]
	  (println (format "%s:%d" addr port)))