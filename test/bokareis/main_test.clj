(ns bokareis.main-test
  (:use midje.sweet
        bokareis.main
        [clojure.java.io :only [file] :rename {file f}]
        [clojure.data.json :only [json-str]])
  (:require [net.cgrand.enlive-html :as h])
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

(fact "application renders posts"
  (with-temp-dir root
    (create-post (f root "posts")
                 (json-str {"slug" "hello-world"
                            "published" "2012-02-11T14:47:00Z"})
                 "# Hello world\n\nThis is a paragraph\n\n<post-link to=\"hello-world\"></post-link>\n")
    (write-text-file "{}" (f root "blog.meta"))
    (write-text-file "This is a post.<post-text></post-text>"
                     (f root "templates" "post.html"))
    (write-text-file "This is the index.<posts></posts>"
                     (f root "templates" "index.html"))
    (-main (str root))
    (slurp (f root "out" "index.html"))
    => (contains "<a href=\"/2012/02/11/hello-world/\">Hello world</a>")
    (slurp (f root "out" "2012" "02" "11" "hello-world" "index.html"))
    => (contains "<h1>Hello world</h1>")
    (slurp (f root "out" "2012" "02" "11" "hello-world" "index.html"))
    => (contains "<a href=\"/2012/02/11/hello-world/\">Hello world</a>")
    (slurp (f root "out" "index.html"))
    => (contains "This is the index.")
    (slurp (f root "out" "2012" "02" "11" "hello-world" "index.html"))
    => (contains "This is a post.")))

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

(fact "read-post can read a post"
  (with-temp-dir root
    (create-post (f root "a")
                 (json-str {"slug" "foo-bar"
                            "published" "2012-02-11T14:47:00Z"})
                 "Foo bar.")
    (read-post (f root "a"))
    => (contains {"slug" "foo-bar"
                  "published" (DateTime/parse "2012-02-11T14:47:00Z")
                  "text" (html-contains "Foo bar.")})))

(fact
  (relative-post-output-dir {"slug" "foo-bar"
                             "published" (DateTime/parse "2012-02-11T14:47:00Z")})
  => (f "2012" "02" "11" "foo-bar"))

(fact "render-post can handle links to other posts"
  (render-post {"slug" "foo-bar"
                "published" (DateTime/parse "2012-03-01T16:41:00Z")
                "text" (h/html-snippet "<h1>Foo Bar</h1><p><post-link to=\"hello-world\"></p>")}
               {:posts-by-slug {"hello-world" {"slug" "hello-world"
                                               "published" (DateTime/parse "2012-02-11T14:47:00Z")
                                               "text" (h/html-snippet "<h1>Hello world</h1>\n\n<p>This is a paragraph</p>\n")}}
                :templates {:post {:tag :post-text}}})
  => (contains "<a href=\"/2012/02/11/hello-world/\">Hello world</a>"))

