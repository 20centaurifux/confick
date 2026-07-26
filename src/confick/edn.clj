(ns confick.edn
  (:refer-clojure :exclude [read-string])
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [environ.core :as env]))

;;; Resolvable Protocol & Types

(defprotocol Resolvable
  "A value that can be resolved after an EDN form has been read."
  (resolve-val [this]
    "Resolves this instance and returns its resulting value."))

(deftype Environment [k]
  Resolvable
  (resolve-val [_] (env/env k :confick/none))
  Object
  (toString [_]
    (str "#env " k)))

(deftype Slurp [path]
  Resolvable
  (resolve-val [_] (try
                     (-> (slurp path)
                         str/trim)
                     (catch java.io.FileNotFoundException _
                       :confick/none)))
  Object
  (toString [_]
    (str "#slurp " path)))

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
  "Reads a single EDN form from `s`.

  In addition to standard EDN, the tagged literals `#env` and `#slurp` are
  supported. Their values remain unresolved until passed to `resolve-vals`."
  [s]
  (edn/read-string {:readers readers} s))

;;; Resolve

(defn resolve-vals
  "Recursively replaces every Resolvable in `form` with its resolved value."
  [form]
  (walk/postwalk #(cond-> %
                    (satisfies? Resolvable %) resolve-val)
                 form))