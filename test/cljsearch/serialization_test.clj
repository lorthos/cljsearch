(ns cljsearch.serialization-test
  (:import (java.io File))
  (:require [clojure.test :refer :all]
            [cljsearch.index :as index]
            [cljsearch.search :as s]
            [cljsearch.serialization :refer :all]
            [clojure.java.io :as io]))

(defn delete-file-recursively
  "Delete file f. If it's a directory, recursively delete all its contents.
Raise an exception if any deletion fails unless silently is true."
  [f & [silently]]
  (let [f (io/file f)]
    (if (.isDirectory ^File f)
      (doseq [child (.listFiles f)]
        (delete-file-recursively child silently)))
    (io/delete-file f silently)))


(defonce root-path "target/tmp/output/")

(defn small-tsv-fixture [f]
  (index/reset-inverted-index!)
  (index/reset-document-store!)
  (index/parse-tsv-and-create-index! "test-resources/small.tsv")
  (try
    (f)
    (catch Exception _))
  (index/reset-inverted-index!)
  (index/reset-document-store!)
  (delete-file-recursively root-path false)
  )


(use-fixtures :once small-tsv-fixture)


(deftest serialization
  (testing "serialization of the sharded index"
    (is (not (nil?
               (serialize-all-shards-to-folder! root-path index/sharded-inverted-index @index/document-store))))
    (is (= '({:id "9780061120077", :score 1/2} {:id "9780140432268", :score 1/3} {:id "9781400076215", :score 1/6})
           (s/search (index/view-index) "novel")))
    )

  (testing "de-serialization of a single shard"
    (is (= '({:id "9780061120077", :score 1/2} {:id "9780140432268", :score 1/3} {:id "9781400076215", :score 1/6})
           (let [shard-no (index/find-shard-number-based-on-term "novel")
                 de-serialized (deserialize-single-shard-and-document-storage root-path shard-no)
                 shard (:shard de-serialized)]
             (s/search shard "novel"))))
    )
  )
