(ns bokareis.main
  (:require [net.cgrand.enlive-html :as h]
            [bokareis.blog :as blog])
  (:use [clojure.java.io :only [file] :rename {file f}]
        [bokareis.io :only [read-blog
                            write-text-file
                            relative-post-output-dir]]
        [bokareis.plugin :only [expand]]))

(defn render-post [post blog]
  (let [post-tree (expand (blog/template blog :post) blog (get post "slug"))]
    (apply str (h/emit* post-tree))))

(defn render-index [blog]
  (->> (expand (blog/template blog :index) blog nil)
       (h/emit*)
       (apply str)))

(defn -main [root]
  (let [blog (read-blog root)
        out-dir (f root "out")]
    (write-text-file (render-index blog)
                     (f out-dir "index.html"))
    (doseq [post (blog/all-posts blog)]
      (write-text-file (render-post post blog)
                       (f out-dir
                          (relative-post-output-dir post)
                          "index.html")))))

