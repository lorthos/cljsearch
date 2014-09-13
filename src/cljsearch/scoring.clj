(ns cljsearch.scoring)


(defn td-idf-scorer
  "given an index match for a single term,returns a sorted set of document id and score maps

  calculates scores based on TF/IDF for all the documents containing the term

  Recalling from cljsearch.index and test-resources/index.end; An Index Match for the term TERM3 Looks Like the Following:

  {
     :id1 1
     :id2 2
     :id3 3
  }
  We need to walk the dictionary and calculate based on this,  the output should look like:
  ({:id :id3, :score 1/2} {:id :id2, :score 1/3} {:id :id1, :score 1/6})
  "
  [index-match]
  (let [idf (reduce + (vals index-match))
        scores (map #(identity {:id %1 :score (/ (get index-match %1) idf)}) (keys index-match))]
    (sort-by :score > scores)
    ))

