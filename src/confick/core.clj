(ns confick.core
  (:require [clojure.edn :as edn]
            [clojure.core.memoize :as memo]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [environ.core :refer [env]])
  (:import [java.lang NumberFormatException]))

(defn- try-parse-int
  [val default]
  (try
    (-> val str str/trim Integer/parseInt)
    (catch NumberFormatException _ default)))

(defonce ^:private cache-millis (try-parse-int
                                 (env :edn-config-cache-millis)
                                 60000))

(defonce ^:private edn-config-path (or (env :edn-config-path)
                                       "config.edn"))

(defn- from-fs
  []
  (-> edn-config-path
      slurp
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

(defn lookup
  "Searches for a configuration value, where ks is a sequence of keys."
  [ks & {:keys [required default conform] :or {conform any?}}]
  (let [path (flatten [ks])]
    (letfn [(assert-required [v]
              (if (#{::none} v)
                (if required
                  (throw (ex-info "Key not found."
                                  {:path path :value v}))
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

(defmacro bind
  "Evaluates body in a lexical scope in which the symbols in the
   binding-forms are bound to their corresponding configuration values.

  Example:
    (bind [addr [:tcp :address]
           port [:tcp :port]]
      (format \"%s:%d\" addr port))

  Use metadata to assign default values, make configuration keys mandatory or
  validate them with the Spec library.

  Example:
    (bind [^:required addr [:tcp :address]
           ^{:default 80 :conform pos?} port [:tcp :port]
      (format \"%s:%d\" addr port))"
  [bindings & body]
  `(let* ~(vec (mapcat #(list (first %)
                              (cons 'confick.core/lookup
                                    (cons (second %)
                                          (flatten (vec (meta (first %)))))))
                       (partition 2 bindings)))
         ~@body))