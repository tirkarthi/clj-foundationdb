(ns clj-foundationdb.core-test
  (:import (com.apple.foundationdb Database)
           (com.apple.foundationdb FDB)
           (com.apple.foundationdb Range)
           (com.apple.foundationdb.tuple Tuple)
           (java.nio ByteBuffer))
  (:require [clojure.test :refer :all]
            [clj-foundationdb.core :refer :all]))

(defn key-fixture
  [f]
  (let [fd (. FDB selectAPIVersion 510)
        keys ["bar" "bar1" "bar2" "car"]
        value "1"]
    (with-open [db (.open fd)]
      (set-keys db keys value)
      (f)
      (clear-all db))))

(use-fixtures :each key-fixture)

(deftest test-set
  (testing "Test simple set"
    (let [fd (. FDB selectAPIVersion 510)
          key "foo"
          value "1"]
      (with-open [db (.open fd)]
        (is (nil? (get-val db key)))
        (set-val db key value)
        (is (= (get-val db key) value))))))

(deftest test-multiple-set
  (testing "Test multiple set"
    (let [fd (. FDB selectAPIVersion 510)
          keys ["foo" "for"]
          expected-keys (seq ["bar" "bar1" "bar2" "car" "foo" "for"])
          value "1"]
      (with-open [db (.open fd)]
        (set-keys db keys value)
        (is (every? #(= (get-val db %1) value) expected-keys)))))

  (testing "Test get all keys"
    (let [fd (. FDB selectAPIVersion 510)
          keys (seq ["bar" "bar1" "bar2" "car" "foo" "for"])
          value "1"]
      (with-open [db (.open fd)]
        (let [returned-keys (mapcat first (get-all db))]
          (is (= keys returned-keys))
          (is (every? #(= (get-val db %1) value) keys)))))))

(deftest test-get-range-begin-end
  (testing "Test get all keys with the range of begin and end with end being exclusive. [a, b)"
    (let [fd (. FDB selectAPIVersion 510)
          prefix "b"
          value "1"]
      (with-open [db (.open fd)]
        (let [keys (mapcat first (get-range db "b" "bar2"))]
          (is (= '("bar" "bar1") keys))
          (is (every? #(= (get-val db %1) value) keys)))))))

(deftest test-get-range-starts-with
  (testing "Test get all keys with prefix"
    (let [fd (. FDB selectAPIVersion 510)
          prefix "car"
          value "1"]
      (with-open [db (.open fd)]
        (let [keys (mapcat first (get-range-startswith db prefix))]
          (is (= '("car") keys))
          (is (every? #(= (get-val db %1) value) keys)))))))

(deftest test-get-last-less-than
  (testing "Test last less than the given key"
    (let [fd (. FDB selectAPIVersion 510)
          key "bar1"
          value "1"
          expected-keys '("bar")]
      (with-open [db (.open fd)]
        (let [keys (mapcat first (last-less-than db key))]
          (is (= expected-keys keys))
          (is (every? #(= (get-val db %1) value) keys)))))))

(deftest test-get-last-less-or-equal
  (testing "Test last less than or equal to the given key"
    (let [fd (. FDB selectAPIVersion 510)
          key "bar1"
          value "1"
          expected-keys '("bar1")]
      (with-open [db (.open fd)]
        (let [keys (mapcat first (last-less-or-equal db key))]
          (is (= expected-keys keys))
          (is (every? #(= (get-val db %1) value) keys)))))))

(deftest test-get-first-greater-than
  (testing "Test first greater than"
    (let [fd (. FDB selectAPIVersion 510)
          key "bar"
          value "1"
          expected-keys '("bar1")]
      (with-open [db (.open fd)]
        (let [keys (mapcat first (first-greater-than db key))]
          (is (= expected-keys keys))
          (is (every? #(= (get-val db %1) value) keys))))))

  (testing "Test first greater than with limit"
    (let [fd (. FDB selectAPIVersion 510)
          key "bar"
          value "1"
          limit 2
          expected-keys '("bar1" "bar2")]
      (with-open [db (.open fd)]
        (let [keys (mapcat first (first-greater-than db key limit))]
          (is (= expected-keys keys))
          (is (every? #(= (get-val db %1) value) keys))))))

  (testing "Test first greater than with last key"
    (let [fd (. FDB selectAPIVersion 510)
          key "foo"
          value "1"]
      (with-open [db (.open fd)]
        (let [keys (first-greater-than db key)]
          (is (empty? keys)))))))

(deftest test-get-first-greater-or-equal
  (testing "Test first greater or equal"
    (let [fd (. FDB selectAPIVersion 510)
          key "bar"
          value "1"
          expected-keys '("bar")]
      (with-open [db (.open fd)]
        (let [keys (mapcat first (first-greater-or-equal db key))]
          (is (= expected-keys keys))
          (is (every? #(= (get-val db %1) value) keys))))))

  (testing "Test first greater or equal with limit"
    (let [fd (. FDB selectAPIVersion 510)
          key "bar"
          value "1"
          limit 2
          expected-keys '("bar" "bar1")]
      (with-open [db (.open fd)]
        (let [keys (mapcat first (first-greater-or-equal db key limit))]
          (is (= expected-keys keys))
          (is (every? #(= (get-val db %1) value) keys)))))))

(deftest test-non-existent-get
  (testing "Test non-existent record for nil"
    (let [fd (. FDB selectAPIVersion 510)
          key "foo12"]
      (with-open [db (.open fd)]
        (is (nil? (get-val db key)))))))

(deftest test-clear
  (testing "Test clear a key"
    (let [fd (. FDB selectAPIVersion 510)
          key "foo123"
          value "1"]
      (with-open [db (.open fd)]
        (set-val db key value)
        (is (= (get-val db key) "1"))
        (clear-key db key)
        (is (nil? (get-val db key)))))))

(deftest test-clear-all
  (testing "Test clear all"
    (let [fd (. FDB selectAPIVersion 510)]
      (with-open [db (.open fd)]
        (clear-all db)
        (is (empty? (get-all db)))))))
