(ns clj-foundationdb.core
  (:import (com.apple.foundationdb Database)
           (com.apple.foundationdb FDB)
           (com.apple.foundationdb Range)
           (com.apple.foundationdb.tuple Tuple)
           (java.nio ByteBuffer))
  (:require [clojure.spec.alpha :as spec]
            [clj-foundationdb.utils :refer :all]))

(defn get-val
  "Set a value for the key"
  [key]
  (reify
    java.util.function.Function
    (apply [this tr]
      (let [key   (.getBytes key)
            value @(.get tr key)]
        (if value
          (bytes-to-str value))))))

(spec/fdef get-val
           :args (spec/cat :key string?)
           :ret (spec/nilable string?))

(defn get-tuple-val
  "Get the value for the collection of keys as tuple"
  [key]
  (reify
    java.util.function.Function
    (apply [this tr]
      (let [key   (.pack (Tuple/from (to-array key)))]
        (if-let [value @(.get tr key)]
          (.getString (Tuple/fromBytes value) 0))))))

(spec/fdef get-tuple-val
           :args (spec/cat :key (spec/coll-of string?))
           :ret (spec/nilable string?))

(defn set-val
  "Set a value for the key"
  [key value]
  (reify
    java.util.function.Function
    (apply [this tr]
      (let [key   (.getBytes key)
            value (.getBytes value)]
        (.set tr key value)))))

(spec/fdef set-val
           :args (spec/cat :key string? :value string?)
           :ret (spec/nilable string?))

(defn set-tuple-val
  "Set a value for the collection of keys with tuple"
  [key value]
  (reify
    java.util.function.Function
    (apply [this tr]
      (let [key   (.pack (Tuple/from (to-array key)))
            value (.pack (Tuple/from (to-array [value])))]
        (.set tr key value)))))

(spec/fdef set-val
           :args (spec/cat :key (spec/coll-of string?) :value string?))

(defn set-keys
  "Set given keys with the value"
  [keys value]
  (reify
    java.util.function.Function
    (apply [this tr]
      (let [keys  (map #(.getBytes %1) keys)
            value (.getBytes value)]
        (doall (map #(.set tr %1 value) keys))))))

(spec/fdef set-keys
           :args (spec/cat :key (spec/coll-of string?) :value string?))


(defn clear-val
  "Clear a key from the database"
  [key]
  (reify
    java.util.function.Function
    (apply [this tr]
      (let [key (.getBytes key)]
        (.clear tr key)))))

(spec/fdef clear-val
           :args (spec/cat :key string?))


(defn clear-tuple-val
  "Clear a key from the database"
  [key]
  (reify
    java.util.function.Function
    (apply [this tr]
      (let [key (.pack (Tuple/from (to-array [key])))]
        (.clear tr key)))))

(spec/fdef clear-tuple-val
           :args (spec/cat :key (spec/coll-of string?)))

(defn get-range
  "Get a range of key values as a vector"
  [prefix]
  (reify
    java.util.function.Function
    (apply [this tr]
      (let [begin (.getBytes prefix)
            r     (Range/startsWith begin)]
        (->> (.getRange tr r)
             (mapv #(vector
                     (bytes-to-str (.getKey %1))
                     (bytes-to-str (.getValue %1)))))))))

(spec/fdef get-range
           :args (spec/cat :prefix string?))


(defn get-all
  "Get a range of key values as a vector"
  [key]
  (reify
    java.util.function.Function
    (apply [this tr]
      (let [key (Tuple/from (to-array [key]))]
        (->> (.getRange tr (.range key))
             (mapv #(vector
                     (.getString (Tuple/fromBytes (.getKey %1)) 1)
                     (.getString (Tuple/fromBytes (.getValue %1)) 0))))))))

(defn clear-range
  "Clear a range of keys from the database"
  [key]
  (reify
    java.util.function.Function
    (apply [this tr] (.clear tr (.range key)))))


(spec/fdef clear-range
           :args (spec/cat :key string?))


;; https://stackoverflow.com/a/21421524/2610955

(defn clear-all
  "Clear all the keys and values"
  []
  (reify
    java.util.function.Function
    (apply [this tr]
      (let [begin (.getBytes "")
            end   (.getBytes "xFF")]
        (.clear tr (Range. begin end))))))
