(ns bokareis.main
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json])
  (:use [clojure.java.io :only [file] :rename {file f}])
  (:import (org.joda.time DateTime)))

(def post-meta-file-name "post.meta")

(def post-text-file-name "text.markdown")

(def post-text-key "text")

(def markdown-file-encoding "UTF-8")

(def json-file-encoding "UTF-8")

(declare list-posts read-post read-json-file relative-post-output-dir)

(defn -main [root]
  (.mkdir (f root "out"))
  (spit (f root "out" "index.html") "")
  (doseq [post (map read-post (list-posts (f root "posts")))
          :let [post-out-dir (f root "out" (relative-post-output-dir post))]]
    (.mkdirs post-out-dir)
    (spit (f post-out-dir "index.html") "")))

(defn list-posts [node]
  (if (.exists (f node post-meta-file-name))
    (list node)
    (mapcat list-posts (.listFiles node))))

(defn read-post [post-dir]
  (let [meta (read-json-file (f post-dir post-meta-file-name))
        text (slurp (f post-dir post-text-file-name) :encoding markdown-file-encoding)]
    (assoc meta post-text-key text, "published" (DateTime/parse (get meta "published")))))

(defn- read-json-file [file]
  (with-open [in (io/reader file :encoding json-file-encoding)]
    (json/read-json in false)))

(defn relative-post-output-dir [post]
  (let [{:strs [published slug]} post
        year  (format "%d"   (-> published .year .get))
        month (format "%02d" (-> published .monthOfYear .get))
        day   (format "%02d" (-> published .dayOfMonth .get))]
    (io/file year month day slug)))

