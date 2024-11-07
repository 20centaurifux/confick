(ns confick.core
  (:require [clojure.core.memoize :as memo]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [environ.core :refer [env]])
  (:import [java.lang NumberFormatException]))

(defn- try-parse-int
  [x default]
  (try
    (-> x str str/trim Integer/parseInt)
    (catch NumberFormatException _ default)))

(defonce ^:private cache-millis (try-parse-int
                                 (env :confick-cache-millis)
                                 60000))

(defonce ^:private config-path (or (env :confick-path)
                                   "config.edn"))

(defn- from-fs
  []
  (-> config-path
      slurp
      edn/read-string))

(defonce ^:private from-cache (memo/ttl from-fs
                                        :ttl/threshold cache-millis))

(defn gulp
  "Reads the entire EDN formatted configuration file.

  The default relative path of the configuration file is \"config.edn\". It
  gets overwritten by the CONFICK_PATH environment variable or Java system
  property.

  Set CONFICK_CACHE_MILLIS to zero to disable caching."
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
       (format \"%s:%d\" addr port))
   
   Throws an ExceptionInfo if a required key is missing or a value doesn't
   conform a spec. The additional data of the exception contains path and value
   of the affected key."
  [bindings & body]
  `(let* ~(vec (mapcat #(list (first %)
                              (cons 'confick.core/lookup
                                    (cons (second %)
                                          (flatten (vec (meta (first %)))))))
                       (partition 2 bindings)))
         ~@body))