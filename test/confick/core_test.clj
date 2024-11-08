(ns confick.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [confick.core :refer [gulp lookup bind]]))

(deftest test-gulp
  (testing "load configuration"
    (let [m (gulp)]
      (is (= {:foo "bar" :answer {:of {:everything 42}}}
             m)))))

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

  (testing "set value if it conforms spec"
    (let [x (lookup :foo :conform string?)]
      (is (= "bar" x))))

  (testing "throw exception when value doesn't conform spec"
    (is (thrown? clojure.lang.ExceptionInfo
                 (lookup :foo :conform int?))))

  (testing "spec is validated after setting default value"
    (let [x (lookup :bar :default 23 :conform pos?)]
      (is (= 23 x))
      (is (thrown? clojure.lang.ExceptionInfo
                   (lookup :bar :default 23 :conform neg?))))))

(deftest test-bind
  (testing "all keys found"
    (bind [a :foo
           b [:answer :of :everything]]
      (is (= "bar" a))
      (is (= 42 b))))

  (testing "key not found"
    (bind [x [:foo :bar]]
      (is (nil? x))))

  (testing "set default value when key not found"
    (bind [^{:default 42} a :foo
           ^{:default 23} b :bar]
      (is (= "bar" a))
      (is (= 23 b))))

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