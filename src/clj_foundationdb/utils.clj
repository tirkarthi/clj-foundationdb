(ns clj-foundationdb.utils
  (:import (com.apple.foundationdb.tuple Tuple)))

(defn bytes-to-str
  "
  Convert bytes to string
  "
  [bytes]
  (apply str (map char bytes)))

(defn key->tuple
  "
  Pack the key with respect to Tuple encoding
  "
  [key]
  (let [key (if (sequential? key) key [key])]
    (.pack (Tuple/from (to-array key)))))

(defn bytes->key
  "
  Get the key from the KeyValue object. Since the keys might be nested use .getItems.
  Since all the keys might not be encoded with Tuple layer use stringification for those cases
  "
  [bytes]
  (let [key (.getKey bytes)]
    (try
      (.getItems (Tuple/fromBytes key))
      (catch IllegalArgumentException e (bytes-to-str key)))))

(defn bytes->value
  "
  Get the value from the KeyValue object.
  Since all the values might not be encoded with Tuple layer use stringification for those cases
  "
  [bytes]
  (let [value (.getValue bytes)]
    (try
      (.get (Tuple/fromBytes value) 0)
      (catch IllegalArgumentException e (bytes-to-str value)))))

(defn range->kv
  "
  Convert range object into a vector of key value pairs.
  "
  [range-query]
  (->> range-query
       (mapv #(vector
               (bytes->key %1)
               (bytes->value %1)))))
