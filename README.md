# confick

confick is a tiny library to load and cache configuration data from an [edn](https://github.com/edn-format/edn) file.

## Installation

The library can be installed from Clojars:

[![Clojars Project](https://img.shields.io/clojars/v/de.dixieflatline/confick.svg?include_prereleases)](https://clojars.org/de.dixieflatline/confick)

## Example

Given the following `config.edn`:

```clojure
{:tcp {:address "localhost"
       :port 8080}}
```

The complete configuration can be loaded with `gulp`, individual values with
`lookup`, or several values with `bind`:

```clojure
(require '[confick.core :as confick])

;; load the complete configuration
(confick/gulp)
;; => {:tcp {:address "localhost", :port 8080}}

;; receive a mandatory configuration value
(confick/lookup [:tcp :address] :required true)
;; => "localhost"

;; destructure configuration values in a let-like binding
(confick/bind [{:keys [address port]} :tcp]
  (format "%s:%d" address port))
;; => "localhost:8080"

;; use metadata for required values, defaults, and validation
(confick/bind [^:required addr [:tcp :address]
               ^{:default 80 :conform nat-int?} port [:tcp :port]]
  (println (format "%s:%d" addr port)))

;; force the next access to reload the configuration file
(confick/clear-cache!)
```

Configuration keys used with `lookup` and `bind` must be simple values, such as
keywords or strings. Collections such as vectors, lists, maps, or sets are not
supported as keys. Nested maps remain accessible by passing a sequence of
simple keys as the configuration path.

## Resolvable EDN values

Configuration files support tagged literals for values that are resolved when
they are loaded through `confick.core`:

```clojure
{:database {:host "localhost"
            :password #env :database-password}
 :certificate #slurp "/run/secrets/certificate.pem"}
```

`#env` reads an environment variable or Java system property. The keyword
follows environ's naming conventions; for example, `:database-password`
corresponds to `DATABASE_PASSWORD`.

`#slurp` reads the file at the given path and trims surrounding whitespace from
its contents. Relative paths are resolved from the application's working
directory.

If an environment variable or file does not exist, `lookup` and `bind` treat
the value as missing: optional values return nil, defaults are applied, and
required values cause an exception.

`gulp`, `lookup`, and `bind` resolve these values automatically. The lower-level
`confick.edn` API keeps reading and resolving as separate operations:

```clojure
(require '[confick.edn :as edn])

(-> "{:password #env :database-password}"
    edn/read-string
    edn/resolve-vals)
```

In addition to standard EDN tagged literals such as `#inst` and `#uuid`,
confick supports `#env` and `#slurp`. Application-defined tagged literal
readers cannot be registered with confick's EDN reader.

## Configuration

The default relative path of the configuration file is `"config.edn"`. It can
be overridden with the `CONFICK_PATH` environment variable or the
`confick.path` Java system property:

```shell
CONFICK_PATH=/etc/my-app/config.edn java -jar my-app.jar
java -Dconfick.path=/etc/my-app/config.edn -jar my-app.jar
```

If the configuration file does not exist, confick uses an empty configuration
map.

Configuration is cached for 60 seconds by default. Set
`CONFICK_CACHE_MILLIS` to a different duration in milliseconds, or to zero to
disable caching. The equivalent Java system property is
`confick.cache.millis`:

```shell
CONFICK_CACHE_MILLIS=30000 java -jar my-app.jar
java -Dconfick.cache.millis=0 -jar my-app.jar
```
