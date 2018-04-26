(ns clj-foundationdb.core-test
  (:import (com.apple.foundationdb Database)
           (com.apple.foundationdb FDB)
           (com.apple.foundationdb Range)
           (com.apple.foundationdb.tuple Tuple)
           (java.nio ByteBuffer))
  (:require [clojure.test :refer :all]
            [clj-foundationdb.core :refer :all]))

(deftest test-set
  (testing "Test simple set"
    (let [fd (. FDB selectAPIVersion 510)
          key "foo"
          value "1"]
      (with-open [db (.open fd)]
        (.run db (set-val key value))
        (is (= (.run db (get-val key)) value))))))

(deftest multiple-set
  (testing "Test multiple set"
    (let [fd (. FDB selectAPIVersion 510)
          keys ["bar" "bar1" "bar2" "car"]
          value "1"]
      (with-open [db (.open fd)]
        (.run db (set-keys keys value))
        (is (every? #(= (.run db (get-val %1)) value) keys))))))

(deftest test-get-range-begin-end
  (testing "Test get all keys with the range of begin and end with end being exclusive. [a, b)"
    (let [fd (. FDB selectAPIVersion 510)
          prefix "b"
          value "1"]
      (with-open [db (.open fd)]
        (let [keys (map first (.run db (get-range "b" "bar2")))]
          (is (= '("bar" "bar1") keys))
          (is (every? #(= (.run db (get-val %1)) value) keys)))))))

(deftest test-get-range-starts-with
  (testing "Test get all keys with prefix"
    (let [fd (. FDB selectAPIVersion 510)
          prefix "c"
          value "1"]
      (with-open [db (.open fd)]
        (let [keys (map first (.run db (get-range-startswith prefix)))]
          (is (= '("car") keys))
          (is (every? #(= (.run db (get-val %1)) value) keys)))))))

(deftest test-get-range-all
  (testing "Test get all keys"
    (let [fd (. FDB selectAPIVersion 510)
          prefix "b"
          value "1"]
      (with-open [db (.open fd)]
        (let [keys (map first (.run db (get-all)))]
          (is (= '("bar" "bar1" "bar2" "car") keys))
          (is (every? #(= (.run db (get-val %1)) value) keys)))))))

(deftest test-non-existent-get
  (testing "Test non-existent record for nil"
    (let [fd (. FDB selectAPIVersion 510)
          key "foo12"]
      (with-open [db (.open fd)]
        (is (nil? (.run db (get-val key))))))))

(deftest test-clear
  (testing "Test clear a key"
    (let [fd (. FDB selectAPIVersion 510)
          key "foo"
          value "1"]
      (with-open [db (.open fd)]
        (.run db (set-val key value))
        (is (= (.run db (get-val key)) "1"))
        (.run db (clear-key key))
        (is (nil? (.run db (get-val key))))))))

(deftest test-clear-all
  (testing "Test clear all"
    (let [fd (. FDB selectAPIVersion 510)]
      (with-open [db (.open fd)]
        (.run db (clear-all))
        (is (empty? (.run db (get-all))))))))
