(ns bokareis.main
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.java.shell :as sh])
  (:use [clojure.java.io :only [file] :rename {file f}])
  (:import (org.joda.time DateTime)))

(def post-meta-file-name "post.meta")

(def post-text-file-name "text.markdown")

(def post-text-key "text")

(def markdown-file-encoding "UTF-8")

(def json-file-encoding "UTF-8")

(def markdown-command "markdown")

(declare list-posts read-post read-json-file relative-post-output-dir)

(defn -main [root]
  (.mkdir (f root "out"))
  (spit (f root "out" "index.html") "")
  (doseq [post (map read-post (list-posts (f root "posts")))
          :let [post-out-dir (f root "out" (relative-post-output-dir post))]]
    (.mkdirs post-out-dir)
    (spit (f post-out-dir "index.html") (get post "text"))))

(defn list-posts [node]
  (if (.exists (f node post-meta-file-name))
    (list node)
    (mapcat list-posts (.listFiles node))))

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

(defn render-and-slurp-markdown
  "Process the file through Markdown and slurp the result as a
   string. The resulting HTML text is decoded using UTF-8."
  [file]
  (slurp-shell-out markdown-command (str file) :out-enc markdown-file-encoding))

(defn read-post [post-dir]
  (let [meta (read-json-file (f post-dir post-meta-file-name))
        text (render-and-slurp-markdown (f post-dir post-text-file-name))]
    (assoc meta
      post-text-key text
      "published" (DateTime/parse (get meta "published")))))

(defn- read-json-file [file]
  (with-open [in (io/reader file :encoding json-file-encoding)]
    (json/read-json in false)))

(defn relative-post-output-dir [post]
  (let [{:strs [published slug]} post
        year  (format "%d"   (-> published .year .get))
        month (format "%02d" (-> published .monthOfYear .get))
        day   (format "%02d" (-> published .dayOfMonth .get))]
    (io/file year month day slug)))

