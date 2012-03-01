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
         read-json-file render-post render-index write-text-file
         relative-post-output-dir post-title post-url post-path-segments
         expand-links post-link-node? expand-post-link)

(defn -main [root]
  (let [posts (map read-post (list-posts (f root "posts")))
        posts-by-slug (into {} (for [post posts]
                                 [(get post "slug") post]))
        out-dir (f root "out")]
    (write-text-file (render-index posts)
                     (f out-dir "index.html"))
    (doseq [post posts]
      (write-text-file (render-post post posts-by-slug)
                       (f out-dir
                          (relative-post-output-dir post)
                          "index.html")))))

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

(defn render-post [post posts-by-slug]
  (apply str (h/emit* (expand-links (get post "text") posts-by-slug))))

(defn render-index [posts]
  (apply str
         (concat ["<ul>\n"]
                 (for [post posts]
                   (format "<li><a href=\"%s\">%s</a></li>\n"
                           (post-url post)
                           (apply str (h/emit* (post-title post)))))
                 ["</ul>"])))

(defn write-text-file [string file]
  (.mkdirs (.getParentFile file))
  (spit file string :encoding html-file-encoding))

(defn relative-post-output-dir [post]
  (apply io/file (post-path-segments post)))

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

(defn expand-links [tree posts-by-slug]
  (h/at tree [(h/pred post-link-node?)] (expand-post-link posts-by-slug)))

(defn post-link-node? [node]
  (= (:tag node) :post-link))

(defn expand-post-link [posts-by-slug]
  (fn [node]
    (let [slug (get-in node [:attrs :to])
          post (get posts-by-slug slug)]
      (if post
        (let [url (post-url post)
              title (post-title post)]
          {:tag :a
           :attrs {:href url}
           :content title})
        (throw (RuntimeException. (str "Unknown post slug: " slug)))))))
