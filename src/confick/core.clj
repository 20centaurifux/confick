(ns confick.core
  (:require [clojure.edn :as edn]
            [clojure.core.memoize :as memo]
            [clojure.string :as str]
            [environ.core :refer [env]])
  (:import [java.lang NumberFormatException]))

(defn- try-parse-int
  [val & {:keys [default]}]
  (try
    (-> val str str/trim Integer/parseInt)
    (catch NumberFormatException _ default)))

(defonce ^:private cache-millis (try-parse-int
                                 (env :edn-config-cache-millis)
                                 :default 60000))

(defonce ^:private edn-config-path (or (env :edn-config-path)
                                       "config.edn"))

(defn- from-fs
  []
  (-> (slurp edn-config-path)
      edn/read-string))

(defonce ^:private from-cache
  (memo/ttl from-fs
            :ttl/threshold cache-millis))

(defn gulp
  "Reads the entire EDN formatted configuration file.

  The default relative path of the configuration file is \"config.edn\". It
  gets overwritten by the EDN_CONFIG_PATH environment variable or Java system
  property.

  Set EDN_CONFIG_CACHE_MILLIS to zero to disable caching."
  []
  (if (pos? cache-millis)
    (from-cache)
    (from-fs)))

(defn- ->keys
  [ks]
  (flatten [ks]))

(defn lookup
  "Searches for a configuration value, where `ks` is a sequence of keys."
  [ks & {:keys [required default] :or {required false default nil}}]
  (if-some [v (get-in (gulp)
                      (->keys ks)
                      default)]
    v
    (when required
      (throw (Exception. (format "Key %s not found."
                                 (str/join " " (->keys ks))))))))

(defmacro bind
  "Evaluates `body` in a lexical scope in which the symbols in the
  binding-forms are bound to their corresponding configuration values.

  Configuration values are looked up at runtime.

  Example:
    (bind [addr [:tcp :address]
           port [:tcp :port]]
      (format \"%s:%d\" addr port))

  Use metadata to assign default values or make configuration keys mandatory.

  Example:
    (bind [^:required addr [:tcp :address]
           ^{default: 80} port [:tcp :port]]
      (format \"%s:%d\" addr port))"
  [bindings & body]
  `(let* ~(vec (mapcat #(list (first %)
                              (cons 'confick.core/lookup
                                    (cons (second %)
                                          (flatten (vec (meta (first %)))))))
                       (partition 2 bindings)))
         ~@body))

(defmacro bind*
  "Evaluates `body` in a lexical scope in which the symbols in the
  binding-forms are bound to their corresponding configuration values.

  Configuration values are looked up at compile-time.

  Example:
    (bind* [addr [:tcp :address]
            port [:tcp :port]]
      (format \"%s:%d\" addr port))

  Use metadata to assign default values or make configuration keys mandatory.

  Example:
    (bind* [^:required addr [:tcp :address]
            ^{default: 80} port [:tcp :port]]
      (format \"%s:%d\" addr port))"
  [bindings & body]
  `(let* ~(vec (mapcat #(list (first %)
                              (apply lookup
                                     (cons (second %)
                                           (flatten (vec (meta (first %)))))))
                       (partition 2 bindings)))
         ~@body))
