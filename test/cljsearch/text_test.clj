(ns cljsearch.text-test
  (:require [clojure.test :refer :all]
            [cljsearch.text :refer :all])
  )

(def sentence "Harlem in the 1920s, the capital of Black America and jazz itself, is the setting for Toni Morrisons dazzling novel.")

(deftest tokenization
  (testing "punctuation tokenizer"
    (is (= '("harlem" "in" "the" "1920s" "the" "capital" "of" "black" "america" "and" "jazz" "itself" "is" "the" "setting" "for" "toni" "morrisons" "dazzling" "novel")
           (punctuation-tokenizer sentence))))
  (testing "stop-words-filtering"
    (is (= '("harlem" "1920s" "capital" "black" "america" "jazz" "itself" "setting" "for" "toni" "morrisons" "dazzling" "novel")
           (stopwords-filter (punctuation-tokenizer sentence))))
    )
  (testing "entire parsing flow"
    (is (= '("harlem" "1920s" "capital" "black" "america" "jazz" "itself" "setting" "for" "toni" "morrisons" "dazzling" "novel")
           (text->indexable sentence)
           ))
    )
  )
