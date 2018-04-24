(ns class-scheduling.main
  (:import (com.apple.foundationdb Database)
           (com.apple.foundationdb FDB)
           (com.apple.foundationdb Range)
           (com.apple.foundationdb.tuple Tuple)
           (java.nio ByteBuffer))
  (:require [clj-foundationdb.core :refer :all]
            [clj-foundationdb.utils :refer :all]))


(def times '("2:00", "3:00", "4:00",
             "5:00", "6:00", "7:00", "8:00", "9:00", "10:00", "11:00", "12:00", "13:00",
             "14:00", "15:00", "16:00", "17:00", "18:00", "19:00"))

(def types '("chem", "bio", "cs",
             "geometry", "calc", "alg", "film", "music", "art", "dance"))

(def levels '("intro", "for dummies",
              "remedial", "101", "201", "301", "mastery", "lab", "seminar"))


(defn get-class-names
  []
  (for [level levels
        type  types
        time  times]
    (clojure.string/join " " [level type time])))


(defn init-db
  []
  (let [fd (. FDB selectAPIVersion 510)
        attendance-records (Tuple/from (to-array ["attends"]))
        class-records (Tuple/from (to-array ["class"]))]
    (with-open [db (.open fd)]
      (.run db (clear-range attendance-records))
      (.run db (clear-range class-records)))))


(defn add-class
  [classes]
  (let [fd            (. FDB selectAPIVersion 510)]
    (with-open [db (.open fd)]
      (.run db (set-keys (map #(vector "class" %1) classes) "100")))))


(defn current-class-attendance
  [class-name]
  (let [fd      (. FDB selectAPIVersion 510)
        record  ["class" class-name]]
    (with-open [db (.open fd)]
      (if-let [value (.run db (get-val record))]
        (Integer/parseInt value)))))


(defn signup-class
  [student class-name]
  (let [fd                (. FDB selectAPIVersion 510)
        record            (.pack (Tuple/from (to-array ["attends" student class-name])))
        class             (.pack (Tuple/from (to-array ["class" class-name])))
        seats             (current-class-attendance class-name)
        flag              (.pack (Tuple/from (to-array [""])))
        already-signed-up (.pack (Tuple/from (to-array ["attends" student class-name])))]
    (with-open [db (.open fd)]
      (if (and (not (.run db (get-val record)))
               (not (zero? seats)))
        (do
          (.run db (set-val record flag))
          (.run db (set-val class (str (dec (int seats)))))
          "Unable to sign up")))))


(defn drop-class
  [student class-name]
  (let [fd     (. FDB selectAPIVersion 510)
        record (.pack (Tuple/from (to-array ["attends" student class-name])))]
    (with-open [db (.open fd)]
      (.run db (clear-val record)))))


(defn available-classes
  []
  (let [fd     (. FDB selectAPIVersion 510)
        record "class"]
    (with-open [db (.open fd)]
      (.run db (get-range record)))))
