(ns bokareis.io
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.java.shell :as sh]
            [net.cgrand.enlive-html :as h]
            [bokareis.blog :as blog])
  (:use [clojure.java.io :only [file] :rename {file f}])
  (:import (org.joda.time DateTime)))

(declare read-blog list-posts slurp-shell-out render-and-slurp-markdown
         read-template read-post read-json-file write-text-file)

(def post-meta-file-name "post.meta")

(def post-text-file-name "text.markdown")

(def markdown-file-encoding "UTF-8")

(def json-file-encoding "UTF-8")

(def html-file-encoding "UTF-8")

(def markdown-command "markdown")

(defn read-blog [root]
  (let [posts (map read-post (list-posts (f root "posts")))
        post-template (read-template (f root "templates" "post.html"))
        index-template (read-template (f root "templates" "index.html"))
        templates {:post post-template
                   :index index-template}]
    (blog/make-blog posts templates)))

(defn list-posts [node]
  (if (.exists (f node post-meta-file-name))
    (list node)
    (mapcat list-posts (.listFiles node))))

(defn read-template [file]
  (h/html-resource (io/reader file :encoding html-file-encoding)))

(defn read-post [post-dir]
  (let [meta (read-json-file (f post-dir post-meta-file-name))
        text (render-and-slurp-markdown (f post-dir post-text-file-name))
        html-text (h/html-snippet text)]
    (assoc meta
      "text" html-text
      "published" (DateTime/parse (get meta "published")))))

(defn- read-json-file [file]
  (with-open [in (io/reader file :encoding json-file-encoding)]
    (json/read-json in false)))

(defn render-and-slurp-markdown
  "Process the file through Markdown and slurp the result as a
   string. The resulting HTML text is decoded using UTF-8."
  [file]
  (slurp-shell-out markdown-command (str file) :out-enc markdown-file-encoding))

(defn- slurp-shell-out
  "Same as clojure.java.shell/sh, but returns only the stdout of the
   sub-process. An exception is thrown if the sub-process exits with
   non-zero status. This function does not touch the :out-enc option."
  [command & args]
  (let [{:keys [exit, out], :as m} (apply sh/sh command args)]
    (if-not (zero? exit)
      (throw (RuntimeException.
              (str "Command exited with non-zero status: " command)))
      out)))

(defn write-text-file [string file]
  (.mkdirs (.getParentFile file))
  (spit file string :encoding html-file-encoding))

(defn relative-post-output-dir [post]
  (apply io/file (blog/post-path-segments post)))

