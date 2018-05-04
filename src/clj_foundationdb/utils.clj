(ns clj-foundationdb.utils
  (:import (com.apple.foundationdb.tuple Tuple)))

(defn key->tuple
  [key]
  (.pack (Tuple/from (to-array key))))

(defn bytes->key
  [bytes]
  (.getItems (Tuple/fromBytes (.getKey bytes))))

(defn bytes->value
  [bytes]
  (.get (Tuple/fromBytes (.getValue bytes)) 0))
