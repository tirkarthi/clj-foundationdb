(ns clj-foundationdb.core
  (:require [clojure.spec.alpha :as spec]
            [clj-foundationdb.utils :refer :all])
  (:import (com.apple.foundationdb Database
                                   FDB
                                   Range
                                   KeySelector
                                   Transaction)
           (com.apple.foundationdb.tuple Tuple)
           (java.util List)))

(def db? #(instance? com.apple.foundationdb.Database %1))

;; Refer https://github.com/apple/foundationdb/blob/e0c8175f3ccad92c582a3e70e9bcae58fff53633/bindings/java/src/main/com/apple/foundationdb/tuple/TupleUtil.java#L171

(def serializable? #(or (number? %1)
                        (string? %1)
                        (decimal? %1)
                        (instance? List %1)))

(defn get-val
  "Get the value for the collection of keys as tuple

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
            (let [key   (key->tuple key)]
              (if-let [value @(.get tr key)]
                (.get (Tuple/fromBytes value) 0)))))))

(spec/fdef get-val
           :args (spec/cat :db db? :key serializable?)
           :ret (spec/nilable serializable?))

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
            (let [key   (key->tuple [key])
                  value (key->tuple [value])]
              (.set tr key value))))))

(spec/fdef set-val
           :args (spec/cat :db db? :key serializable? :value serializable?)
           :ret (spec/nilable string?))

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
            (let [keys  (map #(key->tuple %1) keys)
                  value (key->tuple [value])]
              (doseq [key keys] (.set tr key value)))))))

(spec/fdef set-keys
           :args (spec/cat :db db? :key (spec/coll-of serializable?) :value serializable?))

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
            (let [key (key->tuple [key])]
              (.clear tr key))))))

(spec/fdef clear-key
           :args (spec/cat :db db? :key serializable?))

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
            (let [prefix      (key->tuple [prefix])
                  range-query (Range/startsWith prefix)]
              (->> (.getRange tr range-query)
                   range->kv))))))

(spec/fdef get-range-startswith
           :args (spec/cat :db db? :prefix string?))

(defn watch
  [db key]
  (.run db
        (reify
          java.util.function.Function
          (apply [this tr]
            (let [key (.getBytes key)]
              (.watch tr key))))))

(spec/fdef watch
           :args (spec/cat :db db? :key serializable?))

(defn get-range
  "Get a range of key values as a vector

  (let [fd    (. FDB selectAPIVersion 510)
        begin \"foo\"
        end   \"foo\"]
  (with-open [db (.open fd)]
     (get-range db begin end)))
  "
  ([db begin]
   (.run db
         (reify
           java.util.function.Function
           (apply [this tr]
             (let [begin       (Tuple/from (to-array (if (sequential? begin) begin [begin])))
                   range-query (.getRange tr (.range begin))]
               (range->kv range-query))))))
  ([db begin end]
   (.run db
         (reify
           java.util.function.Function
           (apply [this tr]
             (let [begin       (key->tuple [begin])
                   end         (key->tuple [end])
                   range-query (.getRange tr (Range. begin end))]
               (range->kv range-query)))))))

(spec/fdef get-range
           :args (spec/cat :db db? :begin string? :end string?))

;; https://stackoverflow.com/a/21421524/2610955
;; Refer : https://forums.foundationdb.org/t/how-to-clear-all-keys-in-foundationdb-using-java/351/2

(defn get-all
  "Get all key values as a vector"
  [db]
  (.run db
        (reify
          java.util.function.Function
          (apply [this tr]
            (let [begin       (byte-array [])
                  end         (byte-array [0xFF])
                  range-query (.getRange tr (Range. begin end))]
              (range->kv range-query))))))

(spec/fdef get-all
           :args (spec/cat :db db?))

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
            (let [begin (key->tuple begin)
                  end   (key->tuple end)]
              (.clear tr (Range. begin end)))))))

(spec/fdef clear-range
           :args (spec/cat :db db? :begin string? :end string?))

;; https://stackoverflow.com/a/21421524/2610955
;; Refer : https://forums.foundationdb.org/t/how-to-clear-all-keys-in-foundationdb-using-java/351/2

(defn clear-all
  "Clear all  keys from the database"
  [db]
  (.run db
        (reify
          java.util.function.Function
          (apply [this tr]
            (let [begin (byte-array [])
                  end   (byte-array [0xFF])]
              (.clear tr (Range. begin end)))))))

(spec/fdef clear-all
           :args (spec/cat :db db?))

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
             (let [key         (KeySelector/lastLessThan (key->tuple [key]))
                   end         (.add key limit)
                   range-query (.getRange tr key end)]
               (range->kv range-query)))))))

(spec/fdef last-less-than
           :args (spec/cat :db db? :key serializable? :limit (spec/? pos-int?))
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
             (let [key         (KeySelector/lastLessOrEqual (key->tuple [key]))
                   end         (.add key limit)
                   range-query (.getRange tr key end)]
               (range->kv range-query)))))))

(spec/fdef last-less-or-equal
           :args (spec/cat :db db? :key serializable? :limit (spec/? pos-int?))
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
             (let [key         (KeySelector/firstGreaterThan (key->tuple [key]))
                   end         (.add key limit)
                   range-query (.getRange tr key end)]
               (range->kv range-query)))))))

(spec/fdef first-greater-than
           :args (spec/cat :db db? :key serializable? :limit (spec/? pos-int?))
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
             (let [key         (KeySelector/firstGreaterOrEqual (key->tuple [key]))
                   end         (.add key limit)
                   range-query (.getRange tr key end)]
               (range->kv range-query)))))))

(spec/fdef first-greater-or-equal
           :args (spec/cat :db db? :key serializable? :limit (spec/? pos-int?))
           :ret (spec/coll-of (spec/tuple serializable? serializable?)))
