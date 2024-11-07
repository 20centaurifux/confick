(ns confick.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [confick.core :refer [bind]]))

(deftest test-bind
  (testing "All keys found."
    (bind [foo :foo
           answer [:answer :of :everything]]
      (is (= foo "bar"))
      (is (= answer 42))))

  (testing "Key not found."
    (bind [v [:foo :bar]]
      (is (nil? v))))

  (testing "Set default value when key not found."
    (bind [^{:default 42} foo :foo
           ^{:default 23} bar :bar]
      (is (= foo "bar"))
      (is (= bar 23))))

  (testing "Throw exception when required key not found."
    (is (thrown? clojure.lang.ExceptionInfo
                 (bind [^:required foobar [:foo :bar]]))))

  (testing "Set value if it conforms spec."
    (bind [^{:conform string?} foo :foo]
      (is (= foo "bar"))))

  (testing "Throw exception when value doesn't conform spec."
    (is (thrown? clojure.lang.ExceptionInfo
                 (bind [^{:conform int?} foo :foo]))))

  (testing "Spec is validated after setting default value."
    (bind [^{:default 23 :conform pos?} bar :bar]
      (is (= bar 23)))))