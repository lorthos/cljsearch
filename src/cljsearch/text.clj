(ns cljsearch.text
  "contains the text processing related stuff such as tokenizing, filtering etc.."
  (:require [clojure.string :as string]
            [cljsearch.env :as env]))

(defn stopwords-filter
  "Filters out the stop words from the given seqence of tokens"
  [tokens]
  (filter (complement (:stop-words env/props)) tokens))

(def common-transformer
  "Common text fransformations such as lower-case and trim"
  (comp
    string/trim
    string/lower-case))

(defn punctuation-tokenizer
  "given an arbitrary String containing a bag of words, Split the sentence into a sequence of tokenized text,
  Puctuation tokenier is actually a generator since it creates a bigger output than the original input"
  [^String text]
  (map common-transformer (string/split text #"[\s\W]+")))

(defn text->indexable
  "turns the given text into an indexable list of tokens"
  [^String text]
  (->> text
       punctuation-tokenizer
       stopwords-filter))

