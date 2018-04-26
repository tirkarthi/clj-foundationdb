# clj-foundationdb [![Build Status](https://travis-ci.org/tirkarthi/clj-foundationdb.svg?branch=master)](https://travis-ci.org/tirkarthi/clj-foundationdb)

A Clojure wrapper for FoundationDB

## Installation

At the moment I cannot publish it in Clojars since the java driver was not yet released in Maven or other repo to use it as a dependency. You can clone the repo and install the java library through lein [localrepo](https://github.com/kumarshantanu/lein-localrepo) to use it.

### Install FoundationDB jar

Since FoundationDB is not present in the Maven repo I am using lein localrepo for installing the jar and using it in the project.

JAR can be downloaded from Apple's [download page](https://apple.github.io/foundationdb/downloads.html)

```
$ lein localrepo install maven_repository/fdb-java-5.1.5.jar fdb 5.1.5
```

GitHub issue : https://github.com/apple/foundationdb/issues/219

## Documentation

Docs are available at https://tirkarthi.github.io/clj-foundationdb

I am also trying to port a class scheduling app to Clojure using the library. Currently the API is still in a lot of fluctuation and hence the examples might not work but you can view the source.

Reference implementation in Java : https://apple.github.io/foundationdb/class-scheduling-java.html

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
(let [fd (. FDB selectAPIVersion 510)
      key "foo"
      value "1"]
      (with-open [db (.open fd)]
	        (quick-bench (doall (for [_ (range 100)] (.run db (set-val key value)))))))
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
  [key value n]
  (reify
    java.util.function.Function
    (apply [this tr]
      (dotimes [_ n]
          (.set tr key value)))))

clj-foundationdb.core> (let [fd (. FDB selectAPIVersion 510)
                             key (.getBytes "foo")
                             value (.getBytes "10000")]
                         (with-open [db (.open fd)]
                           (quick-bench (.run db (set-val-n key value 100000)))))
Evaluation count : 6 in 6 samples of 1 calls.
             Execution time mean : 625.745197 ms
    Execution time std-deviation : 72.918243 ms
   Execution time lower quantile : 542.717590 ms ( 2.5%)
   Execution time upper quantile : 718.452721 ms (97.5%)
                   Overhead used : 2.028213 ns

```

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
