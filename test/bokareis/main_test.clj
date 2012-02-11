(ns bokareis.main-test
  (:use midje.sweet
        bokareis.main)
  (:use [clojure.java.io :only [file] :rename {file f}])
  (:import (java.io File)
           (org.joda.time DateTime)))

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

(defn create-post [post-dir meta-content text-content]
  (.mkdirs post-dir)
  (spit (f post-dir "post.meta") meta-content)
  (spit (f post-dir "text.markdown") text-content))

(fact "application renders posts"
  (with-temp-dir root
    (create-post (f root "posts")
                 "{\"slug\": \"hello-world\", \"published\": \"2012-02-11T14:47:00Z\"}"
                 "Hello world")
    (spit (f root "blog.meta")
          "{}")
    (-main (str root))
    (.exists (f root "out" "index.html"))
    => truthy
    (.exists (f root "out" "2012" "02" "11" "hello-world" "index.html"))
    => truthy))

(fact "list-post finds a post"
  (with-temp-dir root
    (create-post (f root "a") "" "")
    (list-posts root)
    => [(f root "a")]))

(fact "list-post finds all posts in a directory"
  (with-temp-dir root
    (create-post (f root "a") "" "")
    (create-post (f root "b") "" "")
    (create-post (f root "c") "" "")
    (list-posts root)
    => (just [(f root "a") (f root "b") (f root "c")] :in-any-order)))

(fact "list-post finds posts regardless of directory nesting"
  (with-temp-dir root
    (create-post (f root "a") "" "")
    (create-post (f root "b" "b") "" "")
    (create-post (f root "c" "c" "c") "" "")
    (list-posts root)
    => (just [(f root "a") (f root "b" "b") (f root "c" "c" "c")] :in-any-order)))

(fact "list-post should ignore unknown files"
  (with-temp-dir root
    (spit (f root "a.txt") "")
    (list-posts root)
    => []))

(fact
  (with-temp-dir root
    (create-post (f root "a")
                 "{\"slug\": \"foo-bar\", \"published\": \"2012-02-11T14:47:00Z\"}"
                 "Foo bar.")
    (read-post (f root "a"))
    => {"slug" "foo-bar"
        "published" (DateTime/parse "2012-02-11T14:47:00Z")
        "text" "Foo bar."}))

(fact
  (relative-post-output-dir {"slug" "foo-bar"
                             "published" (DateTime/parse "2012-02-11T14:47:00Z")})
  => (f "2012" "02" "11" "foo-bar"))

