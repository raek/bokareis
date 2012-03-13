(ns bokareis.util
  (:use midje.sweet
        [clojure.java.io :only [file] :rename {file f}])
  (:require [net.cgrand.enlive-html :as h])
  (:import (java.io File)))

(defn create-temp-dir []
  (let [file (File/createTempFile "bokareis-test" "")]
    (when-not (.delete file)
      (throw (RuntimeException.)))
    (when-not (.mkdir file)
      (throw (RuntimeException.)))
    file))

(defn delete-file-tree [^File tree]
  (when (.isDirectory tree)
    (doseq [child (.listFiles tree)]
      (delete-file-tree child)))
  (.delete tree))

(defmacro with-temp-dir [sym & body]
  `(let [~sym (create-temp-dir)]
     (try (do
            ~@body)
          (finally
           (delete-file-tree ~sym)))))

(defn html-contains [html-string]
  (fn [html-tree]
    ((contains html-string) (apply str (h/emit* html-tree)))))

(fact "temp dir exists within body"
  (with-temp-dir t
    (.exists t) => truthy))

(fact "temp dir does not exist after body"
  (.exists (with-temp-dir t
             t))
  => falsey)

(defn create-post [post-dir meta-content text-content]
  (.mkdirs post-dir)
  (spit (f post-dir "post.meta") meta-content)
  (spit (f post-dir "text.markdown") text-content))
