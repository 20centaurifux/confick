# confick

confick is a tiny library to load and cache configuration settings from an [EDN](https://github.com/edn-format/edn) file.

## Installation

The library can be installed from Clojars using Leiningen:

[![Clojars Project](http://clojars.org/zcfux/confick/latest-version.svg)](https://clojars.org/zcfux/confick)

## Example

[![Clojars Project](http://clojars.org/zcfux/clojure-tlv/latest-version.svg)](https://clojars.org/zcfux/clojure-tlv)

	(require '[confick.core :as cnf])

	(bind [^:required addr [:tcp :address]
	       ^{default: 80} port [:tcp :port]]
	  (format \"%s:%d\" addr port))
