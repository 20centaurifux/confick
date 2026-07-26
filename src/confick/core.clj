(ns confick.core
  "Loads, caches, and queries an EDN configuration."
  (:require [clojure.core.memoize :as memo]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [confick.edn :as edn]
            [environ.core :as env])
  (:import [java.lang NumberFormatException]))

(defn- try-parse-int
  [x default]
  (try
    (-> x str str/trim Integer/parseInt)
    (catch NumberFormatException _ default)))

(defonce ^:private cache-millis (try-parse-int
                                 (env/env :confick-cache-millis)
                                 60000))

(defonce ^:private config-path (or (env/env :confick-path)
                                   "config.edn"))

(defn- from-fs
  []
  (try
    (-> config-path
        slurp
        edn/read-string
        edn/resolve-vals)
    (catch java.io.FileNotFoundException _ {})))

(defonce ^:private from-cache (memo/ttl from-fs :ttl/threshold cache-millis))

(defn clear-cache!
  "Clears the in-memory configuration cache and returns nil.

  The next call to `gulp`, `lookup`, or `bind` reloads the configuration file."
  []
  (memo/memo-clear! from-cache)
  nil)

(defn gulp
  "Returns the complete, resolved EDN configuration.

  Reads `config.edn` by default. `CONFICK_PATH` or the `confick.path` Java
  system property can override the path. Returns an empty map when the file
  does not exist.

  Results are cached for 60 seconds by default. Set
  `CONFICK_CACHE_MILLIS` or the `confick.cache.millis` Java system property to
  change the duration; use zero to disable caching."
  []
  (if (pos? cache-millis)
    (from-cache)
    (from-fs)))

(defn lookup
  "Returns the configuration value at `ks`.

  `ks` may be a single key or a sequence of nested keys. Supported options:

  - `:required` — throw when the value is missing
  - `:default` — value returned when the configuration value is missing
  - `:conform` — spec or predicate used to validate the resulting value

  Missing optional values return nil unless `:default` is supplied. Validation
  happens after applying a default.

  Throws ExceptionInfo with `:path` when a required value is missing. Throws
  ExceptionInfo with `:path`, `:value`, and `:spec` when validation fails."
  [ks & {:keys [required default conform] :or {conform any?}}]
  (let [path (flatten [ks])]
    (letfn [(assert-required [v]
              (if (#{::none} v)
                (if required
                  (throw (ex-info "Key not found."
                                  {:path path}))
                  default)
                v))
            (assert-spec [v]
              (if (s/valid? conform v)
                v
                (throw (ex-info "Value doesn't conform spec."
                                {:path path :value v :spec conform}))))]
      (-> (gulp)
          (get-in path ::none)
          assert-required
          assert-spec))))

(defn- option-args
  [metadata]
  (->> (select-keys metadata [:required :default :conform])
       (reduce-kv
        (fn [args key value]
          (conj args key value))
        [])))

(defmacro bind
  "Binds configuration values and evaluates `body` in the resulting scope.

  `bindings` is a vector of alternating binding forms and configuration paths.
  Binding forms support the same destructuring as `let`.

      (bind [{:keys [address port]} :tcp]
        (format \"%s:%d\" address port))

  Add `:required`, `:default`, or `:conform` metadata to a binding form to pass
  the corresponding option to `lookup`.

      (bind [^:required address [:tcp :address]
             ^{:default 80 :conform pos-int?} port [:tcp :port]]
        (format \"%s:%d\" address port))

  Throws IllegalArgumentException during macro expansion when `bindings` is
  not a vector or contains an odd number of forms. Missing or invalid values
  produce the same ExceptionInfo as `lookup`."
  [bindings & body]
  (when-not (vector? bindings)
    (throw (IllegalArgumentException.
            "bind requires a vector for its bindings")))

  (when (odd? (count bindings))
    (throw (IllegalArgumentException.
            "bind requires an even number of forms in binding vector")))

  `(let ~(vec
          (mapcat
           (fn [[binding path]]
             (let [options (select-keys (meta binding)
                                        [:required :default :conform])]
               [binding
                `(confick.core/lookup
                  ~path
                  ~@(option-args options))]))
           (partition 2 bindings)))
     ~@body))