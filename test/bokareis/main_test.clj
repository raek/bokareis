(ns bokareis.main-test
  (:use midje.sweet
        bokareis.main
        bokareis.io
        [bokareis.util :only [with-temp-dir html-contains create-post]]
        [clojure.java.io :only [file] :rename {file f}]
        [clojure.data.json :only [json-str]])
  (:require [net.cgrand.enlive-html :as h])
  (:import (org.joda.time DateTime)))

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

