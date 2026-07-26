(ns confick.core-test
  (:require [clojure.core.memoize :as memo]
            [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is testing]]
            [confick.core :refer [bind clear-cache! gulp lookup]]))

(deftest test-gulp
  (testing "load configuration"
    (let [m (gulp)]
      (is (= {:foo "bar"
              :answer {:of {:everything 42}}
              :values [1 2]
              :environment "test/resources/config.edn"
              :file "test"
              :missing-environment :confick.core/none
              :missing-file :confick.core/none}
             m))))

  (testing "file not found"
    (with-redefs [confick.core/cache-millis 0
                  confick.core/config-path "xyz"]
      (let [m (gulp)]
        (is (= {} m))))))

(deftest test-clear-cache
  (testing "reload configuration after clearing the cache"
    (let [loads (atom 0)
          cached-load (memo/ttl #(swap! loads inc) :ttl/threshold 60000)]
      (with-redefs [confick.core/cache-millis 60000
                    confick.core/from-cache cached-load]
        (is (= 1 (gulp)))
        (is (= 1 (gulp)))
        (clear-cache!)
        (is (= 2 (gulp)))))))

(deftest test-lookup
  (testing "key found"
    (let [x (lookup :foo)]
      (is (= x "bar"))))

  (testing "key not found"
    (let [x (lookup [:foo :bar])]
      (is (nil? x))))

  (testing "set default value when key not found"
    (let [x (lookup [:foo :bar] :default "bar")]
      (is (= "bar" x))))

  (testing "throw exception when required key not found"
    (is (thrown? clojure.lang.ExceptionInfo
                 (lookup [:foo :bar] :required true))))

  (testing "reject collection keys"
    (is (thrown-with-msg?
         IllegalArgumentException
         #"Configuration keys must not be collections"
         (lookup [:routes [:get "/health"]])))
    (is (thrown-with-msg?
         IllegalArgumentException
         #"Configuration keys must not be collections"
         (lookup {:composite :key}))))

  (testing "treat missing resolvable values as missing"
    (testing "use default values"
      (is (= "fallback" (lookup :missing-environment :default "fallback")))
      (is (= "fallback" (lookup :missing-file :default "fallback"))))

    (testing "throw exception for required values"
      (is (thrown? clojure.lang.ExceptionInfo
                   (lookup :missing-environment :required true)))
      (is (thrown? clojure.lang.ExceptionInfo
                   (lookup :missing-file :required true)))))

  (testing "set value if it conforms spec"
    (let [x (lookup :foo :conform string?)]
      (is (= "bar" x))))

  (testing "throw exception when value doesn't conform spec"
    (let [error (try
                  (lookup :foo :conform int?)
                  (catch clojure.lang.ExceptionInfo e
                    e))]
      (is (= {:path [:foo]
              :value "bar"
              :explain (s/explain-data int? "bar")}
             (ex-data error)))))

  (testing "spec is validated after setting default value"
    (let [x (lookup :bar :default 23 :conform pos?)]
      (is (= 23 x))
      (is (thrown? clojure.lang.ExceptionInfo
                   (lookup :bar :default 23 :conform neg?))))))

(deftest test-bind
  (testing "reject a non-vector binding form"
    (let [error (try
                  (macroexpand '(confick.core/bind (a :foo) a))
                  (catch clojure.lang.Compiler$CompilerException e
                    e))]
      (is (instance? IllegalArgumentException (ex-cause error)))
      (is (re-find #"bind requires a vector"
                   (ex-message (ex-cause error))))))

  (testing "reject an odd number of binding forms"
    (let [error (try
                  (macroexpand '(confick.core/bind [a :foo b] a))
                  (catch clojure.lang.Compiler$CompilerException e
                    e))]
      (is (instance? IllegalArgumentException (ex-cause error)))
      (is (re-find #"bind requires an even number of forms"
                   (ex-message (ex-cause error))))))

  (testing "all keys found"
    (bind [a :foo
           b [:answer :of :everything]]
      (is (= "bar" a))
      (is (= 42 b))))

  (testing "destructure configuration maps"
    (bind [{:keys [of]} :answer]
      (is (= {:everything 42} of))))

  (testing "destructure configuration sequences"
    (bind [[a b] :values]
      (is (= 1 a))
      (is (= 2 b))))

  (testing "key not found"
    (bind [x [:foo :bar]]
      (is (nil? x))))

  (testing "set default value when key not found"
    (bind [^{:default 42} a :foo
           ^{:default 23} b :bar]
      (is (= "bar" a))
      (is (= 23 b))))

  (testing "preserve a sequential default value"
    (bind [^{:default [1 2]} xs :bar]
      (is (= [1 2] xs))))

  (testing "throw exception when required key not found"
    (is (thrown? clojure.lang.ExceptionInfo
                 (bind [^:required _ [:foo :bar]]))))

  (testing "set value if it conforms spec"
    (bind [^{:conform string?} x :foo]
      (is (= "bar" x))))

  (testing "throw exception when value doesn't conform spec"
    (is (thrown? clojure.lang.ExceptionInfo
                 (bind [^{:conform int?} _ :foo]))))

  (testing "spec is validated after setting default value"
    (bind [^{:default 23 :conform pos?} x :bar]
      (is (= 23 x))
      (is (thrown? clojure.lang.ExceptionInfo
                   (bind [^{:default 23 :conform neg?} _ :bar]))))))