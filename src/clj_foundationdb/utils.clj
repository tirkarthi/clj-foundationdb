(ns clj-foundationdb.utils
  (:import (java.nio ByteBuffer)))

(defn encode-int
  [value]
  (let [output (byte-array 4)]
    (.putInt (ByteBuffer/wrap output) value)
    output))

(defn decode-int
  [value]
  (.getInt (ByteBuffer/wrap value)))

(defn bytes-to-str
  [bytes]
  (apply str (map char bytes)))
