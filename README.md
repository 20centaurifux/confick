# confick

confick is a tiny library to load and cache configuration settings from an [EDN](https://github.com/edn-format/edn) file.

## Installation

The library can be installed from Clojars:

[![Clojars Project](https://img.shields.io/clojars/v/de.dixieflatline/confick.svg?include_prereleases)](https://clojars.org/de.dixieflatline/confick)

## Example

	(require '[confick.core :as cnf])

	(cnf/bind [^:required addr [:tcp :address]
	           ^{:default 80 :conform pos?} port [:tcp :port]]
	  (println (format "%s:%d" addr port)))
