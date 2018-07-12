# clj-foundationdb [![Build Status](https://travis-ci.org/tirkarthi/clj-foundationdb.svg?branch=master)](https://travis-ci.org/tirkarthi/clj-foundationdb)

A Clojure wrapper for FoundationDB

## Documentation

Docs are available at https://tirkarthi.github.io/clj-foundationdb

I am also trying to port a class scheduling app to Clojure using the library. Currently the API is still in a lot of fluctuation and hence the examples might not work but you can view the source at examples folder.

Reference implementation in Java : https://apple.github.io/foundationdb/class-scheduling-java.html

## Examples

```clojure

(ns examples.main
  (:require [clj-foundationdb.core :refer :all]
            [clj-foundationdb.utils :refer :all]))

;; Set a key

(let [fd    (select-api-version 520)
      key   "foo"
      value 1]
  (with-open [db (open fd)]
    (tr! db
         (set-val tr key value))))

nil

;; Get a key

(let [fd    (select-api-version 520)
      key   "foo"]
  (with-open [db (open fd)]
    (tr! db
         (get-val tr key))))

1

;; Perform multiple operations in a single transaction

(let [fd    (select-api-version 520)
      key   "foo"
      value 1]
  (with-open [db (open fd)]
    (tr! db
         (set-val tr key value)
         (get-val tr key))))

1

;; Set multiple keys with same value

(let [fd    (select-api-version 520)
      key   [["bar"] ["car"] ["dar"] ["far"]]
      value 1]
  (with-open [db (open fd)]
    (tr! db
         (set-keys tr key value))))

nil

;; Get a range of keys

(let [fd    (select-api-version 520)
      begin "car"
      end   "far"]
  (with-open [db (open fd)]
    (tr! db
         (get-range tr begin end))))

[[["car"] 1] [["dar"] 1]]

;; Get all keys

(let [fd    (select-api-version 520)]
  (with-open [db (open fd)]
    (tr! db
         (get-all tr))))

[[["bar"] 1] [["car"] 1] [["dar"] 1] [["far"] 1] [["foo"] 1]]

;; First key less than given key

(let [fd    (select-api-version 520)
      key   "car"]
  (with-open [db (open fd)]
    (tr! db
         (last-less-than tr key))))

[[["bar"] 1]]

;; First key greater than given key

(let [fd    (select-api-version 520)
      key   "car"]
  (with-open [db (open fd)]
    (tr! db
         (first-greater-than tr key))))

[[["dar"] 1]]

;; Nested keys

(let [fd      (select-api-version 520)
      classes [["class" "intro"] ["class" "algebra"] ["class" "maths"] ["class" "bio"]]
      time    "10:00"]
  (with-open [db (open fd)]
    (tr! db
         (set-keys tr classes time)
         (get-val tr ["class" "algebra"]))))

"10:00"

;; Automatic subspace prefix within a context

(let [fd      (select-api-version 520)
      classes ["intro" "algebra" "maths" "bio"]
      time    "10:00"]
  (with-open [db (open fd)]
    (tr! db
         (with-subspace "class"
           (mapv #(set-val tr %1 time) classes)
           (get-val tr "algebra")))))

"10:00"
```

## Transaction basic example

A basic example of the transactional nature of FoundationDB can be explained as below :

* Set a key "foo" with value "1" and inside the transaction the value is set as "1"
* Try changing the value to "2". You can see that inside the transaction the value of "foo" is "2" once it's changed. But when there is an exception the value is rolled back to "1"
* Get the value of "foo" and it should return "1" since the previous transaction aborted with an exception and hence the value is not updated

```clojure
clj-foundationdb.core> (let [fd    (select-api-version 520)
                             key   "foo"
                             value "1"]
                         (with-open [db (open fd)]
                           (tr! db
                                (set-val tr key value)
                                (get-val tr key))))

1

clj-foundationdb.core> (let [fd    (select-api-version 520)
                             key   "foo"
                             value "2"]
                         (with-open [db (open fd)]
                           (tr! db
                                (println (get-val tr key))
                                (set-val tr key value)
                                (println (get-val tr key))
                                (/ 1 0))))

1
2
ArithmeticException Divide by zero  clojure.lang.Numbers.divide (Numbers.java:163)

clj-foundationdb.core> (let [fd    (select-api-version 520)
                             key   "foo"
                             value "1"]
                         (with-open [db (open fd)]
                           (tr! db
                                (get-val tr key))))
1
```

## Tuple encoding and alternatives

This library uses tuple encoding so that manual encoding and decoding is not necessary. If you want a more thin wrapper please use [clj_fdb](https://github.com/vedang/clj_fdb/). Relevant issue : https://github.com/tirkarthi/clj-foundationdb/issues/15

## Stability

The API is still in early development stages and is subjected to change. It's building up nicely and comments are welcome.

### JDK 7 support

The underlying Java library uses lambdas and hence the minimum supported version is JDK 8.

## Benchmarks

Benchmarks are quite tricky to get right. The main factor with FoundationDB is that everything opens up a new transaction and hence making it slow. But bulk writes are faster.

### System Info

```
➜  clj-foundationdb git:(master) ✗ system_profiler SPHardwareDataType
Hardware:

    Hardware Overview:

      Model Name: MacBook Air
      Model Identifier: MacBookAir7,2
      Processor Name: Intel Core i5
      Processor Speed: 1.6 GHz
      Number of Processors: 1
      Total Number of Cores: 2
      L2 Cache (per Core): 256 KB
      L3 Cache: 3 MB
      Memory: 4 GB
      Boot ROM Version: MBA71.0166.B06
      SMC Version (system): 2.27f2
      Serial Number (system): C02Q4LHTG940
      Hardware UUID: 4ECF3B7B-6444-51AA-BB60-BCE6C285D90D
```

### FoundationDB single writes

```
(let [fd    (select-api-version 520)
      key   "foo"
      value "1"]
      (with-open [db (open fd)]
	        (quick-bench (doall (for [_ (range 100)] (tr! db (set-val tr key value)))))))
Evaluation count : 6 in 6 samples of 1 calls.
             Execution time mean : 1.845528 sec
    Execution time std-deviation : 58.156807 ms
   Execution time lower quantile : 1.795201 sec ( 2.5%)
   Execution time upper quantile : 1.934851 sec (97.5%)
                   Overhead used : 2.028213 ns

Found 1 outliers in 6 samples (16.6667 %)
	low-severe	 1 (16.6667 %)
 Variance from outliers : 13.8889 % Variance is moderately inflated by outliers
```

### FoundationDB batch writes


```
(defn set-val-n
  "Set a value for the key"
  [tr key value n]
  (dotimes [_ n]
     (.set tr key value)))

clj-foundationdb.core> (let [fd (select-api-version 520)
                             key (.getBytes "foo")
                             value (.getBytes "10000")]
                         (with-open [db (open fd)]
                           (quick-bench (tr! db (set-val-n tr key value 100000)))))
Evaluation count : 6 in 6 samples of 1 calls.
             Execution time mean : 625.745197 ms
    Execution time std-deviation : 72.918243 ms
   Execution time lower quantile : 542.717590 ms ( 2.5%)
   Execution time upper quantile : 718.452721 ms (97.5%)
                   Overhead used : 2.028213 ns

```

### FoundationDB write 1 million keys with 10 parallel clients and 100k keys per client

1 million keys set in 23.74 seconds

```clojure
(let [fd (select-api-version 520)
      kv (map #(vector (str %1) %1) (range 100000))]
  (time (let [clients (repeatedly 10 #(future
                                        (with-open [db (open fd)]
                                          (tr! db
                                               (doall (doseq [[k v] kv]
                                                        (set-val tr k v)))))))]
          (doall (map deref clients))
          "Finished")))
"Elapsed time: 23740.242199 msecs"
"Finished"
```

### FoundationDB read 100k keys with 10 parallel clients and 10k keys per client

100k keys read took 16.81 seconds

```
(let [fd (select-api-version 520)
      kv (map #(vector (str %1) %1) (range 100000))]
  (time (with-open [db (open fd)]
          (let [clients (repeatedly 10 #(future
                                          (tr! db
                                               (doall (doseq [k (range 10000)]
                                                        (get-val tr k))))))]
            (doall (map deref clients)))
          "Finished")))
"Elapsed time: 16818.560737 msecs"
"Finished"
```

You can find some more numbers for FoundationDB [here](https://apple.github.io/foundationdb/benchmarking.html)

### Redis

```
➜  redis git:(unstable) redis-benchmark -t set
====== SET ======
  100000 requests completed in 1.59 seconds
  50 parallel clients
  3 bytes payload
  keep alive: 1

96.52% <= 1 milliseconds
99.95% <= 2 milliseconds
99.97% <= 6 milliseconds
100.00% <= 6 milliseconds
62853.55 requests per second
```

### Redis with pipelining

```
➜  redis git:(unstable) redis-benchmark -t set -P 160
====== SET ======
  100000 requests completed in 0.17 seconds
  50 parallel clients
  3 bytes payload
  keep alive: 1

0.00% <= 1 milliseconds
0.64% <= 2 milliseconds
1.12% <= 5 milliseconds
2.72% <= 6 milliseconds
4.48% <= 10 milliseconds
8.48% <= 11 milliseconds
36.96% <= 12 milliseconds
65.12% <= 13 milliseconds
82.40% <= 14 milliseconds
95.36% <= 15 milliseconds
99.04% <= 16 milliseconds
100.00% <= 16 milliseconds
595238.12 requests per second
```

## License

Copyright © 2018 Karthikeyan S

Distributed under the MIT License
