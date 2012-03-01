(ns bokareis.main
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [net.cgrand.enlive-html :as h])
  (:use [clojure.java.io :only [file] :rename {file f}])
  (:import (org.joda.time DateTime)))

(def post-meta-file-name "post.meta")

(def post-text-file-name "text.markdown")

(def markdown-file-encoding "UTF-8")

(def json-file-encoding "UTF-8")

(def html-file-encoding "UTF-8")

(def markdown-command "markdown")

(declare list-posts slurp-shell-out render-and-slurp-markdown read-post
         read-json-file render-post render-index relative-post-output-dir
         post-title post-url post-path-segments)

(defn -main [root]
  (let [posts (map read-post (list-posts (f root "posts")))
        out-dir (f root "out")]
    (.mkdirs out-dir)
    (render-index posts out-dir)
    (doseq [post posts]
      (render-post post out-dir))))

(defn list-posts [node]
  (if (.exists (f node post-meta-file-name))
    (list node)
    (mapcat list-posts (.listFiles node))))

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

(defn render-post [post out-dir]
  (let [post-out-dir (f out-dir (relative-post-output-dir post))]
    (.mkdirs post-out-dir)
    (spit (f post-out-dir "index.html")
          (apply str (h/emit* (get post "text")))
          :encoding html-file-encoding)))

(defn relative-post-output-dir [post]
  (apply io/file (post-path-segments post)))

(defn render-index [posts out-dir]
  (with-open [writer (io/writer (io/file out-dir "index.html")
                                :encoding html-file-encoding)]
    (.write writer "<ul>\n")
    (doseq [post posts]
      (.write writer (format "<li><a href=\"%s\">%s</a></li>\n"
                             (post-url post)
                             (apply str (h/emit* (post-title post))))))
    (.write writer "</ul>")))

(defn post-title [post]
  (-> (get post "text")
      (h/select [:h1])
      (first)
      (:content)))

(defn post-url [post]
  (str "/"
       (str/join "/" (post-path-segments post))
       "/"))

(defn post-path-segments [post]
  (let [{:strs [published slug]} post
        year  (format "%d"   (-> published .year .get))
        month (format "%02d" (-> published .monthOfYear .get))
        day   (format "%02d" (-> published .dayOfMonth .get))]
    [year month day slug]))

