(ns bokareis.blog
  (:require [clojure.string :as str]
            [net.cgrand.enlive-html :as h]))

(defn make-blog [posts templates]
  {:posts-by-slug (into {} (for [post posts]
                             [(get post "slug") post]))
   :templates templates})

(defn post-by-slug [blog slug]
  (get-in blog [:posts-by-slug slug]))

(defn all-posts [blog]
  (vals (:posts-by-slug blog)))

(defn template [blog template-name]
  (get-in blog [:templates template-name]))

(defn post-path-segments [post]
  (let [{:strs [published slug]} post
        year  (format "%d"   (-> published .year .get))
        month (format "%02d" (-> published .monthOfYear .get))
        day   (format "%02d" (-> published .dayOfMonth .get))]
    [year month day slug]))

(defn post-title [post]
  (-> (get post "text")
      (h/select [:h1])
      (first)
      (:content)))

(defn post-url [post]
  (str "/"
       (str/join "/" (post-path-segments post))
       "/"))

