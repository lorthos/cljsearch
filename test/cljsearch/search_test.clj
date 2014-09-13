(ns cljsearch.search-test
  (:import (java.io File))
  (:require [clojure.test :refer :all]
            [cljsearch.index :as index]
            [cljsearch.search :refer :all]
            [clojure.java.io :refer :all]))

(defn small-tsv-fixture [f]
  (index/reset-inverted-index!)
  (index/reset-document-store!)
  (index/parse-tsv-and-create-index! "test-resources/small.tsv")
  (try
    (f)
    (catch Exception _))
  (index/reset-inverted-index!)
  (index/reset-document-store!)
  )


(use-fixtures :once small-tsv-fixture)

(deftest searching
  (testing "test basic term retrieval"
    (is (= '({:id "9780061120077", :score 1})
           (search (index/view-index) "tree")))

    (is (= '({:id "9780061120077", :score 1/2} {:id "9780140432268", :score 1/3} {:id "9781400076215", :score 1/6})
           (search (index/view-index) "novel")))

    (is (= (search (index/view-index) "novel")
           (search-within-shards "novel")))

    (is (nil?
          (search-within-shards "nonexistingterm")))
    )


  (testing "enrich search results with metadata"
    (is (= '({:metadata {:title "A Tree Grows in Brooklyn"}, :id "9780061120077", :score 1/2}
             {:metadata {:title "Washington Square"}, :id "9780140432268", :score 1/3}
             {:metadata {:title "Jazz"}, :id "9781400076215", :score 1/6})
           (enrich-document-metadata
             (search-within-shards "novel")
             @index/document-store))

        (is (= '({:metadata {:title "A Tree Grows in Brooklyn"}, :id "9780061120077", :score 1})
               (enrich-document-metadata
                 (search-within-shards "people")
                 @index/document-store)))

        )

    ))

