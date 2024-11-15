(ns confick.edn-test
  (:require [clojure.test :refer [deftest testing is]]
            [confick.edn :as edn]))

(deftest test-opt
  (testing "key found"
    (let [v (edn/read-string "#cnf/opt :foo")]
      (is (= "bar" v))))

  (testing "key not found"
    (let [v (edn/read-string "#cnf/opt [:foo :bar]")]
      (is (nil? v)))))

(deftest test-req
  (testing "key found"
    (let [v (edn/read-string "#cnf/req :foo")]
      (is (= "bar" v))))

  (testing "key not found"
    (is (thrown? clojure.lang.ExceptionInfo
                 (edn/read-string "#cnf/req [:foo :bar]")))))

(deftest test-or
  (testing "key found"
    (let [v (edn/read-string "#cnf/or [:foo]")]
      (is (= "bar" v))))

  (testing "key not found"
    (let [v (edn/read-string "#cnf/or [[:foo :bar] 23]")]
      (is (= 23 v)))))