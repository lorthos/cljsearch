(ns cljsearch.serialization
  "This module handles the serialization of the in-memory index and document store"
  (:import (java.io FileOutputStream ObjectOutputStream FileInputStream ObjectInputStream)
           (java.util.zip GZIPOutputStream GZIPInputStream))
  (:require [clojure.java.io :as io]))

(defn write-object-to-file
  "Compresses and writes any Clojure data structture (or any serializable object for that matter)
  to disk"
  [obj path]
  (with-open [file-out (FileOutputStream. (io/file path))
              compress-out (GZIPOutputStream. file-out)
              obj-out (ObjectOutputStream. compress-out)]
    (doto obj-out
      (.writeObject obj)
      .flush)))

(defn read-object-from-file
  "UnCompresses and reads any Clojure data structture (or any serializable object for that matter)
  from disk"
  [path]
  (with-open [file-in (FileInputStream. (io/file path))
              compress-in (GZIPInputStream. file-in)
              obj-in (ObjectInputStream. compress-in)]
    (.readObject obj-in)))


(defn make-index-root-path!
  "based on the given path, select the folder for the index and prepare"
  [path]
  (let [root-path (io/file path)]
    (io/make-parents root-path)
    root-path))

(defn make-index-shard-path! [path shard-no]
  "based on the given path, select the shard path for the index and prepare"
  (let [root-path (make-index-root-path! path)
        shard-root-path (str root-path "/" shard-no ".ii")]
    (io/make-parents (io/file shard-root-path))
    shard-root-path))

(defn make-index-meta-path! [path]
  "based on the given path, select the metadata path for the index and prepare"
  (let [root-path (make-index-root-path! path)
        storage-root-path (str root-path "/storage.meta")]
    (io/make-parents (io/file storage-root-path))
    storage-root-path))

(defn serialize-all-shards-to-folder!
  "each shard can be represented as a seperate file on disk, this serializer is going to flush all the indexes to given folder"
  [folder-name sharded-inverted-index document-store]
  (println "Flushing Index to Disk")
  (dorun
    ;go through all the shards, persist them to the related file
    (pmap #(let [underlying-index (deref (get sharded-inverted-index %1))
                 underlying-index-path (make-index-shard-path! folder-name %1)]
            (write-object-to-file underlying-index underlying-index-path))
          (keys sharded-inverted-index)))
  (write-object-to-file document-store (make-index-meta-path! folder-name)))

(defn deserialize-single-shard-and-document-storage
  "used to deserialize a single shard from disk, along with the document storage, returns a data structure like the following
  {:shard #ref
  :metadata #ref}
  This is used when running the application in search only mode"
  [folder-name shard-no]
  (let [meta-path (make-index-meta-path! folder-name)
        meta (read-object-from-file meta-path)
        shard-path (make-index-shard-path! folder-name shard-no)
        shard (read-object-from-file shard-path)]
    {:shard shard :metadata meta}))