(ns cljsearch.scoring-test
  (:import (java.io File))
  (:require [clojure.test :refer :all]
            [cljsearch.scoring :refer :all]))

(deftest scoring
  (testing "naive scorer"
    (is (= '({:id :id3, :score 1/2} {:id :id2, :score 1/3} {:id :id1, :score 1/6})
           (td-idf-scorer {
                            :id1 1
                            :id2 2
                            :id3 3
                            })))
    ))
