(ns confick.edn
  (:refer-clojure :exclude [read-string])
  (:require [clojure.edn :as edn]
            [confick.core :refer [lookup]]))

(defn- req
  [ks]
  (lookup ks :required true))

(defn- or*
  [[ks default]]
  (lookup ks :default default))

(def ^:private default-readers
  {'cnf/opt lookup
   'cnf/req req
   'cnf/or or*})

(defn read-string
  "Read configuration from a string of edn. Configuration keys may be denotied
   by tagging keys with #cnf/opt, #cnf/req and #cnf/or.

   Example:
   (read-string
    \"{:address #cnf/req [:tcp :address] :port #cnf/or [[:tcp :port] 80]}\")"
  ([s]
   (read-string {:eof nil} s))
  ([opts s]
   (let [readers (merge default-readers (:readers opts {}))]
     (edn/read-string (assoc opts :readers readers) s))))