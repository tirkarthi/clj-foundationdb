(ns clj-foundationdb.core
  (:import (com.apple.foundationdb Database)
           (com.apple.foundationdb FDB)
           (com.apple.foundationdb Range)
           (com.apple.foundationdb KeySelector)
           (com.apple.foundationdb.tuple Tuple)
           (java.nio ByteBuffer))
  (:require [clojure.spec.alpha :as spec]
            [clj-foundationdb.utils :refer :all]))


(spec/def ::db #(instance? com.apple.foundationdb.Database %1))

(defn get-val
  "Set a value for the key

  (let [fd  (. FDB selectAPIVersion 510)
        key \"foo\"]
  (with-open [db (.open fd)]
     (get-val db key)))
 "
  [db key]
  (.run db
        (reify
          java.util.function.Function
          (apply [this tr]
            (let [key   (.getBytes key)
                  value @(.get tr key)]
              (if value
                (bytes-to-str value)))))))

(spec/fdef get-val
           :args (spec/cat :db ::db :key string?)
           :ret (spec/nilable string?))

(defn get-tuple-val
  "Get the value for the collection of keys as tuple"
  [db key]
  (.run db
        (reify
          java.util.function.Function
          (apply [this tr]
            (let [key   (.pack (Tuple/from (to-array key)))]
              (if-let [value @(.get tr key)]
                (.getString (Tuple/fromBytes value) 0)))))))

(spec/fdef get-tuple-val
           :args (spec/cat :db ::db :key (spec/coll-of string?))
           :ret (spec/nilable string?))

(defn set-val
  "Set a value for the key

  (let [fd    (. FDB selectAPIVersion 510)
        key   \"foo\"
        value \"bar\"]
  (with-open [db (.open fd)]
     (set-val db key value)))
  "
  [db key value]
  (.run db
        (reify
          java.util.function.Function
          (apply [this tr]
            (let [key   (.getBytes key)
                  value (.getBytes value)]
              (.set tr key value))))))

(spec/fdef set-val
           :args (spec/cat :db ::db :key string? :value string?)
           :ret (spec/nilable string?))

(defn set-tuple-val
  "Set a value for the collection of keys with tuple"
  [db key value]
  (.run db
        (reify
          java.util.function.Function
          (apply [this tr]
            (let [key   (.pack (Tuple/from (to-array key)))
                  value (.pack (Tuple/from (to-array [value])))]
              (.set tr key value))))))

(defn set-tuple-vals
  "Set a value for the collection of keys with tuple"
  [db keys value]
  (.run db
        (reify
          java.util.function.Function
          (apply [this tr]
            (let [keys  (map #(.pack (Tuple/from (to-array %1))) keys)
                  value (.pack (Tuple/from (to-array [value])))]
              (doall (map #(.set tr %1 value) keys)))))))

(spec/fdef set-val
           :args (spec/cat :db ::db :key (spec/coll-of string?) :value string?))

(defn set-keys
  "Set given keys with the value

  (let [fd    (. FDB selectAPIVersion 510)
        keys  [\"foo\" \"baz\"]
        value \"bar\"]
  (with-open [db (.open fd)]
     (set-keys db keys value)))
  "
  [db keys value]
  (.run db
        (reify
          java.util.function.Function
          (apply [this tr]
            (let [keys  (map #(.getBytes %1) keys)
                  value (.getBytes value)]
              (doall (map #(.set tr %1 value) keys)))))))

(spec/fdef set-keys
           :args (spec/cat :db ::db :key (spec/coll-of string?) :value string?))

(defn clear-key
  "Clear a key from the database

  (let [fd  (. FDB selectAPIVersion 510)
        key \"foo\"]
  (with-open [db (.open fd)]
     (clear-key db key)))
  "
  [db key]
  (.run db
        (reify
          java.util.function.Function
          (apply [this tr]
            (let [key (.getBytes key)]
              (.clear tr key))))))

(spec/fdef clear-key
           :args (spec/cat :db ::db :key string?))

(defn clear-tuple-key
  "Clear a key from the database"
  [db key]
  (.run db
        (reify
          java.util.function.Function
          (apply [this tr]
            (let [key (.pack (Tuple/from (to-array [key])))]
              (.clear tr key))))))

(spec/fdef clear-tuple-key
           :args (spec/cat :db ::db :key (spec/coll-of string?)))

(defn get-range-startswith
  "Get a range of key values as a vector that starts with prefix

  (let [fd     (. FDB selectAPIVersion 510)
        prefix \"f\"]
  (with-open [db (.open fd)]
     (get-range-startswith db key prefix)))
  "
  [db prefix]
  (.run db
        (reify
          java.util.function.Function
          (apply [this tr]
            (let [prefix (.getBytes prefix)
                  r     (Range/startsWith prefix)]
              (->> (.getRange tr r)
                   (mapv #(vector
                           (bytes-to-str (.getKey %1))
                           (bytes-to-str (.getValue %1))))))))))

(spec/fdef get-range-startswith
           :args (spec/cat :db ::db :prefix string?))

(defn watch
  [db key]
  (.run db
        (reify
          java.util.function.Function
          (apply [this tr]
            (let [key (.getBytes key)]
              (.watch tr key))))))

(spec/fdef watch
           :args (spec/cat :db ::db :key string?))

(defn get-tuple-range-startswith
  "Get a range of key values as a vector that starts with prefix"
  [db prefix]
  (.run db
        (reify
          java.util.function.Function
          (apply [this tr]
            (let [prefix (.pack (Tuple/from (to-array prefix)))
                  r      (Range/startsWith prefix)]
              (->> (.getRange tr r)
                   (mapv #(vector
                           (bytes-to-str (.getKey %1))
                           (bytes-to-str (.getValue %1))))))))))

(spec/fdef get-tuple-range-startswith
           :args (spec/cat :db ::db :prefix string?))

(defn get-range
  "Get a range of key values as a vector

  (let [fd    (. FDB selectAPIVersion 510)
        begin \"foo\"
        end   \"foo\"]
  (with-open [db (.open fd)]
     (get-range db begin end)))
  "
  [db begin end]
  (.run db
        (reify
          java.util.function.Function
          (apply [this tr]
            (let [begin (.getBytes begin)
                  end   (.getBytes end)
                  r     (Range. begin end)]
              (->> (.getRange tr r)
                   (mapv #(vector
                           (bytes-to-str (.getKey %1))
                           (bytes-to-str (.getValue %1))))))))))

(spec/fdef get-range
           :args (spec/cat :db ::db :begin string? :end string?))

;; https://stackoverflow.com/a/21421524/2610955
;; TODO: Unfortunately this doesn't seem to work and only retrives until keys prefixing x

(defn get-all
  "Get all key values as a vector"
  [db]
  (get-range db "" "xFF"))

(spec/fdef clear-all
           :args (spec/cat :db ::db))

(defn clear-range
  "Clear a range of keys from the database

  (let [fd    (. FDB selectAPIVersion 510)
        begin \"foo\"
        end   \"foo\"]
  (with-open [db (.open fd)]
     (clear-range db begin end)))
  "
  [db begin end]
  (.run db
        (reify
          java.util.function.Function
          (apply [this tr]
            (let [begin (.getBytes begin)
                  end   (.getBytes end)]
              (.clear tr (Range. begin end)))))))

(spec/fdef clear-range
           :args (spec/cat :db ::db :begin string? :end string?))

;; https://stackoverflow.com/a/21421524/2610955
;; TODO: Unfortunately this doesn't seem to work and only deletes till keys prefixing x

(defn clear-all
  "Clear all the keys and values

  (let [fd (. FDB selectAPIVersion 510)]
  (with-open [db (.open fd)]
     (clear-all db)))
  "
  [db]
  (let [begin ""
        end   "xFF"]
    (clear-range db begin end)))

(spec/fdef clear-all
           :args (spec/cat :db ::db))

(defn last-less-than
  "Returns key and value pairs with keys less than the given key for the given limit

  (let [fd  (. FDB selectAPIVersion 510)
        key \"foo\"]
  (with-open [db (.open fd)]
     (last-less-than db key)))
  "
  ([db key]
   (last-less-than db key 1))
  ([db key limit]
   (.run db
         (reify
           java.util.function.Function
           (apply [this tr]
             (let [key (KeySelector/lastLessThan (.getBytes key))
                   end (.add key limit)
                   range-query (.getRange tr key end)]
               (->> range-query
                    (mapv #(vector
                            (bytes-to-str (.getKey %1))
                            (bytes-to-str (.getValue %1)))))))))))

(spec/fdef last-less-than
           :args (spec/cat :db ::db :key string? :limit (spec/? pos-int?))
           :ret (spec/tuple string? number?))

(defn last-less-or-equal
  "Returns key and value pairs with keys less than or equal the given key for the given limit

  (let [fd  (. FDB selectAPIVersion 510)
        key \"foo\"]
  (with-open [db (.open fd)]
     (last-less-or-equal db key)))
  "
  ([db key]
   (last-less-or-equal db key 1))
  ([db key limit]
   (.run db
         (reify
           java.util.function.Function
           (apply [this tr]
             (let [key (KeySelector/lastLessOrEqual (.getBytes key))
                   end (.add key limit)
                   range-query (.getRange tr key end)]
               (->> range-query
                    (mapv #(vector
                            (bytes-to-str (.getKey %1))
                            (bytes-to-str (.getValue %1)))))))))))

(spec/fdef last-less-or-equal
           :args (spec/cat :db ::db :key string? :limit (spec/? pos-int?))
           :ret (spec/tuple string? number?))

(defn first-greater-than
  "Returns key and value pairs with keys greater than the given key for the given limit

  (let [fd  (. FDB selectAPIVersion 510)
        key \"foo\"]
  (with-open [db (.open fd)]
     (first-greater-than db key)))
  "
  ([db key]
   (first-greater-than db key 1))
  ([db key limit]
   (.run db
         (reify
           java.util.function.Function
           (apply [this tr]
             (let [key (KeySelector/firstGreaterThan (.getBytes key))
                   end (.add key limit)
                   range-query (.getRange tr key end)]
               (->> range-query
                    (mapv #(vector
                            (bytes-to-str (.getKey %1))
                            (bytes-to-str (.getValue %1)))))))))))

(spec/fdef first-greater-than
           :args (spec/cat :db ::db :key string? :limit (spec/? pos-int?))
           :ret (spec/tuple string? number?))

(defn first-greater-or-equal
  "Returns key and value pairs with keys greater than or equal to the given key for the given limit

  (let [fd  (. FDB selectAPIVersion 510)
        key \"foo\"]
  (with-open [db (.open fd)]
     (first-greater-or-equal db key)))
  "
  ([db key]
   (first-greater-or-equal db key 1))
  ([db key limit]
   (.run db
         (reify
           java.util.function.Function
           (apply [this tr]
             (let [key (KeySelector/firstGreaterOrEqual (.getBytes key))
                   end (.add key limit)
                   range-query (.getRange tr key end)]
               (->> range-query
                    (mapv #(vector
                            (bytes-to-str (.getKey %1))
                            (bytes-to-str (.getValue %1)))))))))))

(spec/fdef first-greater-or-equal
           :args (spec/cat :db ::db :key string? :limit (spec/? pos-int?))
           :ret (spec/tuple string? number?))
