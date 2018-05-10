(ns class-scheduling.main
  (:import (com.apple.foundationdb Database)
           (com.apple.foundationdb FDB)
           (com.apple.foundationdb Range)
           (com.apple.foundationdb.tuple Tuple)
           (java.nio ByteBuffer))
  (:require [clj-foundationdb.core :refer :all]
            [clj-foundationdb.utils :refer :all]))

(def times ["2:00" "3:00" "4:00"
            "5:00" "6:00" "7:00"
            "8:00" "9:00" "10:00"
            "11:00" "12:00" "13:00"
            "14:00" "15:00" "16:00"
            "17:00" "18:00" "19:00"])

(def types ["chem" "bio" "cs"
            "geometry" "calc" "alg"
            "film" "music" "art" "dance"])

(def levels ["intro" "for dummies" "remedial"
             "101" "201" "301" "mastery"
             "lab" "seminar"])

(def all-classes
  (for [level levels
        type  types
        time  times]
    (clojure.string/join " " [level type time])))

(defn init-db
  [db]
  (tr! db
       (clear-all tr)))

(defn add-class
  [db classes]
  (tr! db
       (set-keys tr (map #(vector "class" %1) classes) 100)))

(defn current-class-attendance
  [db class-name]
  (let [record  ["class" class-name]]
    (tr! db
         (get-val tr record))))

(defn signup-class
  [db student class-name]
  (let [student-record ["attends" student]
        record        ["attends" student class-name]
        seats         (current-class-attendance db class-name)
        class-record  ["class" class-name]
        flag          ""]
    (tr! db
         (if (> (count (get-range tr student-record)) 5)
           (println "Student limit exceeded")
           (do
             (if (and (nil? (get-val tr record))
                      (not (zero? seats)))
               (do
                 (set-val tr record flag)
                 (set-val tr class-record (dec (int seats)))
                 (println "Successful sign up"))
               (println (str student " not able to sign up class : " class-name))))))))

(defn drop-class
  [db student class-name]
  (let [record       ["attends" student class-name]
        seats        (current-class-attendance db class-name)
        class-record ["class" class-name]]
    (tr! db
         (if (get-val tr record)
           (do
             (clear-key tr record)
             (set-val tr class-record (inc (int seats)))
             (println "Successful cancellation"))
           (println (str student " not enrolled in class : " class-name))))))

(defn switch-class
  [db student old-class new-class]
  (tr! db
       (drop-class tr student old-class)
       (signup-class tr student new-class)))

(defn available-classes
  [db]
  (let [record "class"]
    (tr! db
         (filter #(pos-int? (second %1)) (get-range tr record)))))

(comment
  (let [fd (select-api-version 510)]
    (with-open [db (open fd)]
      (tr! db
           (init-db db)
           (add-class db all-classes))))

  ;; signup class
  (let [fd (select-api-version 510)]
    (with-open [db (open fd)]
      (signup-class db "ram" "intro chem 10:00")))

  ;; drop class
  (let [fd (select-api-version 510)]
    (with-open [db (open fd)]
      (drop-class db "ram" "intro chem 10:00")))

  ;; switch class
  (let [fd (select-api-version 510)]
    (with-open [db (open fd)]
      (signup-class db "ram" "intro chem 10:00")
      (switch-class db "ram" "intro chem 10:00" "intro chem 11:00"))))
