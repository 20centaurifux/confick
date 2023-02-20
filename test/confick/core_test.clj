(ns confick.core-test
  (:require [clojure.test :refer :all]
            [confick.core :refer :all]))

(deftest runtime
  (testing "All values defined."
    (bind [foo :foo
           answer [:answer :of :everything]]
          (is (= foo "bar"))
          (is (= answer 42))))
  (testing "Undefined value."
    (bind [v [:foo :bar]]
          (is (nil? v))))
  (testing "Set default value."
    (bind [^{:default 42} foo :foo
           ^{:default 23} bar :bar]
          (is (= foo "bar"))
          (is (= bar 23))))
  (testing "Missing required value."
    (is (thrown? Exception
                 (bind [^:required foobar [:foo :bar]])))))

(deftest compile-time
  (testing "All values defined."
    (bind* [foo :foo
            answer [:answer :of :everything]]
           (is (= foo "bar"))
           (is (= answer 42))))
  (testing "Undefined value."
    (bind* [v [:foo :bar]]
           (is (nil? v))))
  (testing "Set default value."
    (bind* [^{:default 42} foo :foo
            ^{:default 23} bar :bar]
           (is (= foo "bar"))
           (is (= bar 23))))
  (testing "Missing required value."
    (is (thrown? Exception
                 (eval (macroexpand
                        '(bind* [^:required foobar [:foo :bar]])))))))
