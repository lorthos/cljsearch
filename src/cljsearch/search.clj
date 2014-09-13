(ns cljsearch.search
  "A search component will take the queryable index and a single search term as input,
   and return ranked article titles whose article body contains that term."
  (:require [cljsearch.index :as index]
            [cljsearch.text :as text]
            [cljsearch.scoring :as scr])
  )


(defn search-within-shards
  "gets a single search term and searches within the assigned shards

  Search term will be transformed/analyzed the same way as the indexing process"
  [single-search-term]
  (let [index-search-term (text/common-transformer single-search-term)
        underlying-shard (-> index-search-term
                             index/find-shard-number-based-on-term
                             index/get-underlying-index)
        index-match (get @underlying-shard index-search-term)]
    (if-not (nil? index-match)
      (let [scores (scr/td-idf-scorer index-match)]
        scores)
      nil)))

(defn search
  "gets an index reference and a single search term.

  Search term will be transformed/analyzed the same way as the indexing process"
  [inverted-index single-search-term]
  (let [index-search-term (text/common-transformer single-search-term)
        index-match (get inverted-index index-search-term)]
    (if-not (nil? index-match)
      (let [scores (scr/td-idf-scorer index-match)]
        scores)
      nil)))


(defn enrich-document-metadata
  "inverted index only stores document ids, any metada related with the document is store in
  document-storage.

  This function enriches the search results with the matching document metadata in the
  document storage
  "
  [search-results document-store]
  (when-not (nil? search-results)
    (map #(assoc %1 :metadata (get document-store (:id %1))) search-results)))

(defn pretty-print-search-results
  "Pretty print an ordered list of search results"
  [enriched-results]
  (println (str "Found " (count enriched-results) " matching documents"))
  (dorun (map #(println (str "ID: " (:id %1) " Title: " (->> %1
                                                             :metadata
                                                             :title) " Score: " (:score %1) " == " (double (:score %1))))
              enriched-results))
  )