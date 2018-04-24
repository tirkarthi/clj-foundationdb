(defproject xtreak/clj-foundationdb "0.0.1-SNAPSHOT"
  :description "A Clojure wrapper for FoundationDB"
  :url "http://github.com/tirkarthi/clj-foundationdb"
  :repositories {"local" ~(str (.toURI (java.io.File. "maven_repository")))}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [fdb "5.1.5"]]
  :plugins [[lein-localrepo "0.5.4"]
            [lein-codox "0.10.3"]]
  :codox {:output-path "docs"
          :source-uri "http://github.com/tirkarthi/clj-http-ssrf/blob/master/{filepath}#L{line}"})
