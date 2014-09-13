(ns cljsearch.core
  "Entry point for the application"
  (:require [cljsearch.index :as index]
            [cljsearch.search :as search]
            [cljsearch.serialization :as ser]
            [cljsearch.text :as text])
  (:gen-class))

(defonce input-path "test-resources/data.tsv")
(defonce index-path "test-resources/data-index")


(defn interactive-mode
  "starts an interactive mode that once indexes the data in memory and displays a CLI to query the index repeteadly"
  []
  (println "Going to index File:" input-path)
  (println "Press Enter to Start indexing...")
  (read-line)
  (println "Indexing...")
  (time (index/parse-tsv-and-create-index! input-path))
  (println "Index Creation Successfull...")


  (println "Enter single search term:")

  (loop [input (read-line)]
    (when-not (= ":q" input)
      (println (str "You entered: >>" input "<<"))
      (println (take 10 (repeat "+")))
      (time (println (-> (search/search-within-shards input)
                         (search/enrich-document-metadata @index/document-store)
                         search/pretty-print-search-results)))
      (println (take 10 (repeat "+")))
      (recur (read-line))))

  (println "Quit")
  (System/exit 0)
  )

(defn index-mode
  "indexes the file, writes all shards and metadata to a single folder"
  [inpath outpath]
  (time (do (println "Indexing...")
            (index/parse-tsv-and-create-index! inpath)
            (println "Index Creation Successfull...")))
  (time (do
          (println "Saving Index To Disk...")
          (ser/serialize-all-shards-to-folder! outpath index/sharded-inverted-index @index/document-store)
          (println "Index Saved To Disk...")))
  (System/exit 0)
  )

(defn search-mode
  "using the same hash that was used to route the indexed terms, find the appropiate shard, load it and search it"
  [input-term inpath]
  (println "Load Index and Search...")
  (time
    (let [shard-no (index/find-shard-number-based-on-term (text/common-transformer input-term))
          de-serialized (ser/deserialize-single-shard-and-document-storage inpath shard-no)
          ii (:shard de-serialized)
          meta (:metadata de-serialized)]
      (println (-> (search/search ii input-term)
                   (search/enrich-document-metadata meta)
                   search/pretty-print-search-results))))
  (System/exit 0)
  )

(defn -main
  "Main Command Line Interface"
  [& args]
  (println args)
  (let [^String mode (first args)]
    (cond
      (.equals mode "index") (index-mode input-path index-path)
      (.equals mode "search") (search-mode (second args) index-path)
      (.equals mode "interactive") (interactive-mode)
      :else (interactive-mode))))

