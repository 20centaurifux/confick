(ns confick.edn-test
  (:require [clojure.test :refer [deftest is testing]]
            [confick.edn :as edn]
            [environ.core :as env]))

(deftest test-read-string
  (testing "read standard EDN"
    (is (= {:foo [1 2 3]
            :bar #{:a :b}}
           (edn/read-string "{:foo [1 2 3] :bar #{:a :b}}"))))

  (testing "read standard tagged literals"
    (let [[instant uuid]
          (edn/read-string
           "[#inst \"2020-01-01T00:00:00.000-00:00\"
             #uuid \"00000000-0000-0000-0000-000000000000\"]")]
      (is (instance? java.util.Date instant))
      (is (instance? java.util.UUID uuid))))

  (testing "leave confick tagged literals unresolved"
    (let [{:keys [environment file]}
          (edn/read-string
           "{:environment #env :confick-test
             :file #slurp \"test/resources/test.txt\"}")]
      (is (satisfies? edn/Resolvable environment))
      (is (satisfies? edn/Resolvable file))))

  (testing "reject unknown tagged literals"
    (is (thrown-with-msg?
         RuntimeException
         #"No reader function for tag unknown"
         (edn/read-string "#unknown :value")))))

(deftest test-resolve-environment
  (testing "resolve an environment value"
    (with-redefs [env/env {:confick-test "configured"}]
      (is (= "configured"
             (-> "#env :confick-test"
                 edn/read-string
                 edn/resolve-vals)))))

  (testing "resolve a missing environment value"
    (with-redefs [env/env {}]
      (is (= :confick.core/none
             (-> "#env :confick-test"
                 edn/read-string
                 edn/resolve-vals))))))

(deftest test-resolve-slurp
  (testing "resolve trimmed file contents"
    (is (= "test"
           (-> "#slurp \"test/resources/test.txt\""
               edn/read-string
               edn/resolve-vals))))

  (testing "resolve a missing file"
    (is (= :confick.core/none
           (-> "#slurp \"test/resources/missing.txt\""
               edn/read-string
               edn/resolve-vals)))))

(deftest test-resolve-vals
  (testing "resolve values throughout a form"
    (let [resolvable (reify edn/Resolvable
                       (resolve-val [_] :resolved))
          form {:vector [resolvable]
                :list (list resolvable)
                :set #{resolvable}
                resolvable {:nested resolvable}}]
      (is (= {:vector [:resolved]
              :list (list :resolved)
              :set #{:resolved}
              :resolved {:nested :resolved}}
             (edn/resolve-vals form)))))

  (testing "leave ordinary values unchanged"
    (is (= {:foo [1 "bar" nil]}
           (edn/resolve-vals {:foo [1 "bar" nil]})))))