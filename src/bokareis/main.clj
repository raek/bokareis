(ns bokareis.main
  (:require [clojure.string :as str]
            [net.cgrand.enlive-html :as h]
            [bokareis.blog :as blog])
  (:use [clojure.java.io :only [file] :rename {file f}]
        [bokareis.io :only [read-blog
                            write-text-file
                            relative-post-output-dir]]))

(declare plugins -main expand apply-plugin render-post render-index post-title
         post-url expand-links post-link-node? expand-post-link
         expand-post-template post-text-node? expand-post-text-node
         expand-index-template posts-node? expand-posts-node)

(def plugins
  [{:matcher     #'post-link-node?
    :transformer #'expand-post-link}
   {:matcher     #'post-text-node?
    :transformer #'expand-post-text-node}
   {:matcher     #'posts-node?
    :transformer #'expand-posts-node}])

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

(defn expand [tree blog self-slug]
  (reduce #(apply-plugin %2 %1 blog self-slug) tree plugins))

(defn apply-plugin [plugin tree blog self-slug]
  (let [{:keys [matcher transformer]} plugin]
    (h/at tree [(h/pred matcher)] #(transformer % blog self-slug))))

(defn render-post [post blog]
  (let [post-tree (expand (blog/template blog :post) blog (get post "slug"))]
    (apply str (h/emit* post-tree))))

(defn render-index [blog]
  (->> (expand (blog/template blog :index) blog nil)
       (h/emit*)
       (apply str)))

(defn post-title [post]
  (-> (get post "text")
      (h/select [:h1])
      (first)
      (:content)))

(defn post-url [post]
  (str "/"
       (str/join "/" (blog/post-path-segments post))
       "/"))

(defn post-link-node? [node]
  (= (:tag node) :post-link))

(defn expand-post-link [node blog _]
  (let [slug (get-in node [:attrs :to])
        post (blog/post-by-slug blog slug)]
    (if post
      (let [url (post-url post)
            title (post-title post)]
        {:tag :a
         :attrs {:href url}
         :content title})
      (throw (RuntimeException. (str "Unknown post slug: " slug))))))

(defn post-text-node? [node]
  (= (:tag node) :post-text))

(defn expand-post-text-node [_ blog slug]
  (let [post (blog/post-by-slug blog slug)
        text-tree (get post "text")]
    (expand text-tree blog slug)))

(defn posts-node? [node]
  (= (:tag node) :posts))

(defn expand-posts-node [_ blog _]
  {:tag :ul
   :content (for [post (blog/all-posts blog)
                  :let [url (post-url post)
                        title (post-title post)]]
              {:tag :li
               :content [{:tag :a
                          :attrs {:href url}
                          :content title}]})})
