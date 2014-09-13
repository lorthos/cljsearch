(ns cljsearch.index-test
  (:require [clojure.test :refer :all]
            [cljsearch.index :refer :all]))


(def TSV "Betty Smith|A Tree Grows in Brooklyn|9780061120077|The beloved American classic about a young girls coming-of-age at the turn of the century, Betty Smiths A Tree Grows in Brooklyn is a poignant and moving tale filled with compassion and cruelty, laughter and heartache, crowded with life and people and incident. The story of young, sensitive, and idealistic Francie Nolan and her bittersweet formative years in the slums of Williamsburg has enchanted and inspired millions of readers for more than sixty years.")
(def TSV2 "Suze Rotolo|A Freewheelin Time|9780767926881|Suze Rotolos memoir of her early 1960s life with Bob Dylan and the extraordinary period of artistic and political ferment in Greenwich Village.")


(deftest parsing-tsv
  (testing "parsing a TSV line"
    (is (= {:id "9780767926881", :title "A Freewheelin Time", :freqs {"dylan" 1, "suze" 1, "political" 1, "bob" 1,
                                                                      "extraordinary" 1, "greenwich" 1, "ferment" 1, "memoir" 1, "village" 1,
                                                                      "early" 1, "artistic" 1, "period" 1, "1960s" 1, "life" 1, "her" 1, "rotolos" 1}}
           (parse-single-tsv-line TSV2))))

  )

(deftest indexing
  (testing "Stateful operations through sharded indexes"
    (is (= {} (do (reset-inverted-index!)
                  (view-index))))
    (is (= {:y "x", :x "y"}
           (do
             (reset-inverted-index!)
             (reset! (get-underlying-index 0) {:x "y"})
             (reset! (get-underlying-index 1) {:y "x"})
             (view-index))))
    )

  (testing "merging wordcounts into index"
    (is (= {"term3" {"id3" 2, "id1" 1},
            "term2" {"id2" 1},
            "term1" {"id1" 2}}
           (do
             (reset-inverted-index!)
             (merge-single-term-to-index! "term1" 1 "id1")
             (merge-single-term-to-index! "term2" 1 "id2")
             (merge-single-term-to-index! "term3" 1 "id1")
             (merge-single-term-to-index! "term3" 2 "id3")
             (merge-single-term-to-index! "term1" 2 "id1")
             (view-index))))
    )

  (testing "indexing a single item"
    (is (= {"dylan" {"9780767926881" 1}, "suze" {"9780767926881" 1}, "political" {"9780767926881" 1}, "bob" {"9780767926881" 1}, "extraordinary" {"9780767926881" 1},
            "greenwich" {"9780767926881" 1}, "ferment" {"9780767926881" 1}, "memoir" {"9780767926881" 1}, "village" {"9780767926881" 1}, "early" {"9780767926881" 1},
            "artistic" {"9780767926881" 1}, "period" {"9780767926881" 1}, "1960s" {"9780767926881" 1}, "life" {"9780767926881" 1}, "her" {"9780767926881" 1},
            "rotolos" {"9780767926881" 1}}
           (do
             (reset-inverted-index!)
             (add-document-to-index! (parse-single-tsv-line TSV2))
             (view-index))))
    (is (= {"9780061120077" {:title "A Tree Grows in Brooklyn"}, "9780767926881" {:title "A Freewheelin Time"}}
           (do
             (reset-inverted-index!)
             (add-document-to-storage! (parse-single-tsv-line TSV2))
             (add-document-to-storage! (parse-single-tsv-line TSV))
             @document-store))))


  (testing "indexing entire TSV file"
    (is (= 4
           (do
             (reset-inverted-index!)
             (reset! document-store {})
             (parse-tsv-and-create-index! "test-resources/small.tsv")
             (count (keys @document-store)))))
    )
  )

