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
          keys ["bar" "bar1"]
          value "1"]
      (with-open [db (.open fd)]
        (.run db (set-keys keys value))
        (is (every? #(= (.run db (get-val %1)) value) keys))))))

(deftest test-get-range-prefix
  (testing "Test get all keys with the prefix"
    (let [fd (. FDB selectAPIVersion 510)
          prefix "b"
          value "1"]
      (with-open [db (.open fd)]
        (let [keys (map first (.run db (get-range "b")))]
          (is (= '("bar" "bar1") keys))
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
        (.run db (clear-val key))
        (is (nil? (.run db (get-val key))))))))
