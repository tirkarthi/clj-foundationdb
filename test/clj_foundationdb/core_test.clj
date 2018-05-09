(ns clj-foundationdb.core-test
  (:import (com.apple.foundationdb Database)
           (com.apple.foundationdb FDB)
           (com.apple.foundationdb Range)
           (com.apple.foundationdb.tuple Tuple)
           (java.nio ByteBuffer))
  (:require [clojure.test :refer :all]
            [clj-foundationdb.core :refer :all]))

;; Somehow I am not able to bind the db connection so that it's accessible in the test resulting
;; in a transaction for write and another transaction for test in the testing function. Then
;; finally one cleaning up the fixture.
;; Reusing the db to open new transaction also results in the fixture data not being committed
;; and hence not being read by the testing function transaction

(defn key-fixture
  [test]
  (let [fd    (. FDB selectAPIVersion 510)
        keys  ["bar" "bar1" "bar2" "car"]
        value "1"]
    (with-open [db (.open fd)]
      (tr! db
           (set-keys tr keys value)))
    (test)
    (with-open [db (.open fd)]
      (tr! db
           (clear-all tr)))))

(use-fixtures :each key-fixture)

(deftest test-transaction
  (testing "Test transaction rollback"
    (let [fd    (. FDB selectAPIVersion 510)
          key   "foo"
          value "1"]
      (with-open [db (.open fd)]
        (tr! db
             (is (nil? (get-val tr key)))
             (set-val tr key value)
             (is (= (get-val tr key) value)))))
    (let [fd    (. FDB selectAPIVersion 510)
          key   "foo"
          value "2"]
      (with-open [db (.open fd)]
        (is (thrown? Exception
                     (tr! db
                          (set-val tr key value)
                          (is (= (get-val tr key) value))
                          (/ 1 0))))))
    (let [fd    (. FDB selectAPIVersion 510)
          key   "foo"
          value "1"]
      (with-open [db (.open fd)]
        (tr! db
             (is (= (get-val tr key) value)))))))

(deftest test-set
  (testing "Test simple set"
    (let [fd    (. FDB selectAPIVersion 510)
          key   "foo"
          value "1"]
      (with-open [db (.open fd)]
        (tr! db
             (is (nil? (get-val tr key)))
             (set-val tr key value)
             (is (= (get-val tr key) value)))))))

(deftest test-multiple-set
  (testing "Test multiple set"
    (let [fd            (. FDB selectAPIVersion 510)
          keys          ["foo" "for"]
          expected-keys (seq ["bar" "bar1" "bar2" "car" "foo" "for"])
          value         "1"]
      (with-open [db (.open fd)]
        (tr! db
             (set-keys tr keys value)
             (is (every? #(= (get-val tr %1) value) expected-keys))))))

  (testing "Test get all keys"
    (let [fd    (. FDB selectAPIVersion 510)
          keys  (seq ["bar" "bar1" "bar2" "car" "foo" "for"])
          value "1"]
      (with-open [db (.open fd)]
        (tr! db
             (let [returned-keys (mapcat first (get-all tr))]
               (is (= keys returned-keys))
               (is (every? #(= (get-val tr %1) value) keys))))))))

(deftest test-get-range-begin-end
  (testing "Test get all keys with the range of begin and end with end being exclusive. [a, b)"
    (let [fd     (. FDB selectAPIVersion 510)
          prefix "b"
          value  "1"]
      (with-open [db (.open fd)]
        (tr! db
             (let [keys (mapcat first (get-range tr "b" "bar2"))]
               (is (= '("bar" "bar1") keys))
               (is (every? #(= (get-val tr %1) value) keys))))))))

(deftest test-get-range-starts-with
  (testing "Test get all keys with prefix"
    (let [fd     (. FDB selectAPIVersion 510)
          prefix "car"
          value  "1"]
      (with-open [db (.open fd)]
        (tr! db
             (let [keys (mapcat first (get-range-startswith tr prefix))]
               (is (= '("car") keys))
               (is (every? #(= (get-val tr %1) value) keys))))))))

(deftest test-get-last-less-than
  (testing "Test last less than the given key"
    (let [fd            (. FDB selectAPIVersion 510)
          key           "bar1"
          value         "1"
          expected-keys '("bar")]
      (with-open [db (.open fd)]
        (tr! db
             (let [keys (mapcat first (last-less-than tr key))]
               (is (= expected-keys keys))
               (is (every? #(= (get-val tr %1) value) keys))))))))

(deftest test-get-last-less-or-equal
  (testing "Test last less than or equal to the given key"
    (let [fd            (. FDB selectAPIVersion 510)
          key           "bar1"
          value         "1"
          expected-keys '("bar1")]
      (with-open [db (.open fd)]
        (tr! db
             (let [keys (mapcat first (last-less-or-equal tr key))]
               (is (= expected-keys keys))
               (is (every? #(= (get-val tr %1) value) keys))))))))

(deftest test-get-first-greater-than
  (testing "Test first greater than"
    (let [fd            (. FDB selectAPIVersion 510)
          key           "bar"
          value         "1"
          expected-keys '("bar1")]
      (with-open [db (.open fd)]
        (tr! db
             (let [keys (mapcat first (first-greater-than tr key))]
               (is (= expected-keys keys))
               (is (every? #(= (get-val tr %1) value) keys)))))))

  (testing "Test first greater than with limit"
    (let [fd            (. FDB selectAPIVersion 510)
          key           "bar"
          value         "1"
          limit         2
          expected-keys '("bar1" "bar2")]
      (with-open [db (.open fd)]
        (tr! db
             (let [keys (mapcat first (first-greater-than tr key limit))]
               (is (= expected-keys keys))
               (is (every? #(= (get-val tr %1) value) keys)))))))

  (testing "Test first greater than with last key"
    (let [fd    (. FDB selectAPIVersion 510)
          key   "foo"
          value "1"]
      (with-open [db (.open fd)]
        (tr! db
             (let [keys (first-greater-than tr key)]
               (is (empty? keys))))))))

(deftest test-get-first-greater-or-equal
  (testing "Test first greater or equal"
    (let [fd            (. FDB selectAPIVersion 510)
          key           "bar"
          value         "1"
          expected-keys '("bar")]
      (with-open [db (.open fd)]
        (tr! db
             (let [keys (mapcat first (first-greater-or-equal tr key))]
               (is (= expected-keys keys))
               (is (every? #(= (get-val tr %1) value) keys)))))))

  (testing "Test first greater or equal with limit"
    (let [fd            (. FDB selectAPIVersion 510)
          key           "bar"
          value         "1"
          limit         2
          expected-keys '("bar" "bar1")]
      (with-open [db (.open fd)]
        (tr! db
             (let [keys (mapcat first (first-greater-or-equal tr key limit))]
               (is (= expected-keys keys))
               (is (every? #(= (get-val tr %1) value) keys))))))))

(deftest test-non-existent-get
  (testing "Test non-existent record for nil"
    (let [fd  (. FDB selectAPIVersion 510)
          key "foo12"]
      (with-open [db (.open fd)]
        (tr! db
             (is (nil? (get-val tr key))))))))

(deftest test-clear
  (testing "Test clear a key"
    (let [fd    (. FDB selectAPIVersion 510)
          key   "foo123"
          value "1"]
      (with-open [db (.open fd)]
        (tr! db
             (set-val tr key value)
             (is (= (get-val tr key) "1"))
             (clear-key tr key)
             (is (nil? (get-val tr key))))))))

(deftest test-clear-range
  (testing "Test clearing range"
    (let [fd      (. FDB selectAPIVersion 510)
          in-keys [["foo" "a"] ["foo" "b"]]
          value   "1"]
      (with-open [db (.open fd)]
        (tr! db
             (set-keys tr in-keys value)
             (is (every? #(= (get-val tr %1) value) in-keys))
             (clear-range tr "foo")
             (is (every? #(nil? (get-val tr %1)) in-keys))))))

  (testing "Test clearing range with begin and end with end being exclusive"
    (let [fd      (. FDB selectAPIVersion 510)
          in-keys [["bar" "a"] ["foo" "a"] ["foo" "b"] ["gum" "a"]]
          value   "1"]
      (with-open [db (.open fd)]
        (tr! db
             (set-keys tr in-keys value)
             (is (every? #(= (get-val tr %1) value) in-keys))
             (clear-range tr "bar" "gum")
             (is (every? #(nil? (get-val tr %1)) (butlast in-keys))))))))

(deftest test-clear-all
  (testing "Test clear all"
    (let [fd    (. FDB selectAPIVersion 510)
          key   "z"
          value "1"]
      (with-open [db (.open fd)]
        (tr! db
             (set-val tr key value)
             (clear-all tr)
             (is (empty? (get-all tr))))))))

(deftest test-subspace
  (testing "Test simple subspace"
    (let [fd    (. FDB selectAPIVersion 510)
          key   "foo"
          value "bar"]
      (with-open [db (.open fd)]
        (tr! db
             (clear-all tr)
             (with-subspace "class"
               (set-val tr key value)
               (is (= (get-val tr key) value)))
             (is (nil? (get-val tr key)))
             (is (= (get-all tr) [[["class" "foo"] "bar"]]))))))

  (testing "Test multiple level subspace"
    (let [fd    (. FDB selectAPIVersion 510)
          key   "foo"
          value "bar"]
      (with-open [db (.open fd)]
        (tr! db
             (clear-all tr)
             (with-subspace ["class" "intro"]
               (set-val tr key value)
               (is (= (get-val tr key) value)))
             (is (nil? (get-val tr key)))
             (is (= (get-range tr ["class"]) [[["class" "intro" "foo"] "bar"]]))
             (is (= (get-all tr) [[["class" "intro" "foo"] "bar"]])))))))
