(ns bokareis.main-test
  (:use midje.sweet
        bokareis.main)
  (:use [clojure.java.io :only [file] :rename {file f}])
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

(fact "temp dir exists within body"
  (with-temp-dir t
    (.exists t) => truthy))

(fact "temp dir does not exist after body"
  (.exists (with-temp-dir t
             t))
  => falsey)

(fact
  (with-temp-dir root
    (.mkdir (f root "posts"))
    (.mkdir (f root "out"))
    (spit (f root "posts" "post.meta")
          "{\"slug\": \"hello-world\", \"published\": \"2012-02-11T14:47:00Z\"}")
    (spit (f root "posts" "text.markdown")
          "Hello world")
    (spit (f root "blog.meta")
          "{\"posts_dir\": \"posts\", \"output_dir\": \"out\"}")
    (-main (str root))
    (.exists (f root "out" "index.html"))
    => truthy
    (.exists (f root "out" "2012" "02" "11" "hello-world" "index.html"))
    => truthy))

