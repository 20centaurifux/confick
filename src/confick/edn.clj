(ns confick.edn
  "Reads extended EDN forms and resolves deferred configuration values."
  (:refer-clojure :exclude [read-string])
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [environ.core :as env]))

;;; Resolvable Protocol & Types

(defprotocol Resolvable
  "A value whose final representation is resolved after parsing EDN."
  (resolve-val [this]
    "Returns the resolved representation of this value."))

(deftype ^:private Environment [k]
  Resolvable
  (resolve-val [_] (env/env k :confick.core/none)))

(deftype ^:private Slurp [path]
  Resolvable
  (resolve-val [_] (try
                     (-> (slurp path)
                         str/trim)
                     (catch java.io.FileNotFoundException _
                       :confick.core/none))))

;;; EDN

(defn- read-env
  [k]
  (->Environment (keyword k)))

(defn- read-slurp
  [path]
  (->Slurp path))

(def ^:private readers
  {'env read-env
   'slurp read-slurp})

(defn read-string
  "Parses and returns a single extended EDN form from `s`.

  Supports standard EDN plus the tagged literals `#env` and `#slurp`. Tagged
  values remain deferred until the returned form is passed to `resolve-vals`."
  [s]
  (edn/read-string {:readers readers} s))

;;; Resolve

(defn resolve-vals
  "Walks `form` and replaces every Resolvable with its resolved value.

  `#env` values resolve from environment variables or Java system properties.
  `#slurp` values resolve to the trimmed contents of their files. A missing
  variable or file resolves to the internal missing-value sentinel used by
  `confick.core/lookup`."
  [form]
  (walk/postwalk #(cond-> %
                    (satisfies? Resolvable %) resolve-val)
                 form))