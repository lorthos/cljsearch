(ns cljsearch.index
  "An index component will take a plain text corpus as input
  (one TSV file on the local filesystem, uncompressed) and
  produce a queryable index (format at your discretion).


  -> Note: A search component will take the queryable index
  and a single search term as input, and return ranked article titles whose
  article body contains that term.

  -> So this means the title needs to be stored in the index or the file seek will be slow
  -> This will impact the size of the index"
  (:require [cljsearch.env :as env]
            [cljsearch.text :as text]
            [clojure.string :as string]))

;storage and inverted index related stuff
(def document-store
  "stores the non-transformed parts of the document, such as Title, since by the problem definition,
  it must be present in the search results"
  (atom {}))

(defn reset-document-store!
  "reset the document store content"
  []
  (reset! document-store {}))

(def sharded-inverted-index
  "A data structure that holds all the shards as Clojure Atoms"
  (reduce #(merge %1 {%2 (atom {})}) {} (range 0 (:number-of-shards env/props))))

(defn find-shard-number-based-on-term
  "simple hashing to associate a shard with a term"
  [term]
  (mod (hash term) (:number-of-shards env/props)))

(defn get-underlying-index
  "returns the underlying atomic reference to the index data structure for the given shard-number"
  [shard-number]
  (get sharded-inverted-index shard-number))

(defn view-index
  "used to create a single data structure from all shards, only for testing"
  []
  (reduce merge (map #(deref %1) (vals sharded-inverted-index))))

(defn reset-inverted-index!
  "reset the index content"
  []
  (dorun (map #(reset! (get sharded-inverted-index %1) {}) (keys sharded-inverted-index))))


;indexing operations

(defn merge-single-term-to-index!
  "merges a single term occurance into the index, such as
  word1 3 id1

  \"term3\" {
            :id1 1
            :id2 2
            :id3 3
            }
  "
  [term term-count doc-id]
  (let [new-term {doc-id term-count}]
    (swap! (-> term
               find-shard-number-based-on-term
               get-underlying-index)
           (fn [m] (assoc m term (merge (get m term) new-term))))))

(defn add-document-to-index!
  "Adds a single item to the index, item structucture should be matching the tsv-line parser
  documented at: parse-single-tsv-line function

  indexing: for each token in the body of the item, update the inverted index"
  [parsed-corpus-item]
  (let [word-counts (:freqs parsed-corpus-item)]
    (dorun
      ;adjust the index for all the words one by one
      (map #(merge-single-term-to-index! (first %1) (second %1) (:id parsed-corpus-item)) word-counts))))

(defn add-document-to-storage!
  "Adds a single item to the storage,storing the necessary fields of the document"
  [parsed-corpus-item]
  (swap! document-store assoc (:id parsed-corpus-item) {:title (:title parsed-corpus-item)}))

(defn parse-single-tsv-line
  "read the entire tsv line and return a parsed data structure."
  [^String line]
  ;content should have 4 elements
  (let [content (string/split line #"\|")
        content-count (count content)]
    (if (= 4 content-count)
      (let [title (nth content 1)
            id (nth content 2)
            body (text/text->indexable (nth content 3))
            freqs (frequencies body)]
        {:id id :title title :freqs freqs})
      nil)))

(defn parse-tsv-and-create-index!
  "given the path to the tsv-file, create a sequence of of the lines and parse them based on the indexing config"
  [tsv-path]
  (with-open [rdr (clojure.java.io/reader tsv-path)]
    (let [lines (line-seq rdr)]
      (dorun
        (pmap #(let [parsed (parse-single-tsv-line %1)]
                (add-document-to-storage! parsed)
                (add-document-to-index! parsed)
                )
              lines)))))