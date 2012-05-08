(ns bokareis.main
  (:require [clojure.string :as str]
            [net.cgrand.enlive-html :as h]
            [bokareis.blog :as blog])
  (:use [clojure.java.io :only [file] :rename {file f}]
        [bokareis.io :only [read-blog
                            write-text-file
                            relative-post-output-dir]]))

(declare render-post render-index post-title post-url expand-links
         post-link-node? expand-post-link expand-post-template
         post-text-node? expand-index-template posts-node?)

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

(defn render-post [post blog]
  (let [text-tree (expand-links (get post "text") (partial blog/post-by-slug blog))
        post-tree (expand-post-template (blog/template blog :post) text-tree)]
    (apply str (h/emit* post-tree))))

(defn render-index [blog]
  (let [posts-tree {:tag :ul
                    :content (for [post (blog/all-posts blog)
                                   :let [url (post-url post)
                                         title (post-title post)]]
                               {:tag :li
                                :content [{:tag :a
                                           :attrs {:href url}
                                           :content title}]})}
        template (blog/template blog :index)]
    (->> posts-tree
         (expand-index-template template)
         (h/emit*)
         (apply str))))

(defn post-title [post]
  (-> (get post "text")
      (h/select [:h1])
      (first)
      (:content)))

(defn post-url [post]
  (str "/"
       (str/join "/" (blog/post-path-segments post))
       "/"))

(defn expand-links [tree posts-by-slug]
  (h/at tree [(h/pred post-link-node?)] (expand-post-link posts-by-slug)))

(defn post-link-node? [node]
  (= (:tag node) :post-link))

(defn expand-post-link [posts-by-slug]
  (fn [node]
    (let [slug (get-in node [:attrs :to])
          post (posts-by-slug slug)]
      (if post
        (let [url (post-url post)
              title (post-title post)]
          {:tag :a
           :attrs {:href url}
           :content title})
        (throw (RuntimeException. (str "Unknown post slug: " slug)))))))

(defn expand-post-template [template text-tree]
  (h/at template [(h/pred post-text-node?)] (constantly text-tree)))

(defn post-text-node? [node]
  (= (:tag node) :post-text))

(defn expand-index-template [template post-list-tree]
  (h/at template [(h/pred posts-node?)] (constantly post-list-tree)))

(defn posts-node? [node]
  (= (:tag node) :posts))
