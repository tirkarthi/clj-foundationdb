(defproject xtreak/clj-foundationdb "0.0.1"
  :description "A Clojure wrapper for FoundationDB"
  :url "http://github.com/tirkarthi/clj-foundationdb"
  :repositories {"local" ~(str (.toURI (java.io.File. "maven_repository")))}
  :license {:name "MIT license"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.foundationdb/fdb-java "5.2.5"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]]
                   :plugins [[lein-codox "0.10.3"]
                             [lein-cljfmt "0.5.7"]]}}
  :codox {:output-path "docs"
          :source-uri "http://github.com/tirkarthi/clj-foundationdb/blob/master/{filepath}#L{line}"})
