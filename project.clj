(defproject cljsearch "0.1.0-SNAPSHOT"
            :description "Indexing TSV files for Discovery"
            :url "http://example.com/FIXME"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.6.0"]]
            :aot [cljsearch.core] :main cljsearch.core
            :min-lein-version "2.0.0"
            :jvm-opts ["-Xmx1G"]
            :warn-on-reflection true
            )
