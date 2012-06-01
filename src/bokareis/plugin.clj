(ns bokareis.plugin
  (:require [bokareis.blog :as blog]
            [net.cgrand.enlive-html :as h]))

(def plugins [])

(defn register-plugin [matcher transformer]
  (alter-var-root #'plugins conj {:matcher matcher, :transformer transformer}))

(defmacro defplugin [id]
  `(register-plugin (var ~(symbol (str (name id) "-node?")))
                    (var ~(symbol (str "expand-" (name id) "-node")))))

(defn apply-plugin [plugin tree blog self-slug]
  (let [{:keys [matcher transformer]} plugin]
    (h/at tree [(h/pred matcher)] #(transformer % blog self-slug))))

(defn expand [tree blog self-slug]
  (reduce #(apply-plugin %2 %1 blog self-slug) tree plugins))

(defn post-link-node? [node]
  (= (:tag node) :post-link))

(defn expand-post-link-node [node blog _]
  (let [slug (get-in node [:attrs :to])
        post (blog/post-by-slug blog slug)]
    (if post
      (let [url (blog/post-url post)
            title (blog/post-title post)]
        {:tag :a
         :attrs {:href url}
         :content title})
      (throw (RuntimeException. (str "Unknown post slug: " slug))))))

(defplugin post-link)

(defn post-text-node? [node]
  (= (:tag node) :post-text))

(defn expand-post-text-node [_ blog slug]
  (let [post (blog/post-by-slug blog slug)
        text-tree (get post "text")]
    (expand text-tree blog slug)))

(defplugin post-text)

(defn posts-node? [node]
  (= (:tag node) :posts))

(defn expand-posts-node [_ blog _]
  {:tag :ul
   :content (for [post (blog/all-posts blog)
                  :let [url (blog/post-url post)
                        title (blog/post-title post)]]
              {:tag :li
               :content [{:tag :a
                          :attrs {:href url}
                          :content title}]})})

(defplugin posts)

(defn prefix-blog-root-to-node? [node]
  (contains? (:attrs node) :prefix-blog-root-to))

(defn expand-prefix-blog-root-to-node [node blog slug]
  (let [attr-key (-> node :attrs :prefix-blog-root-to keyword)]
    (-> node
        (update-in [:attrs] dissoc :prefix-blog-root-to)
        (update-in [:attrs attr-key] #(str "ROOT/" %)))))

(defplugin prefix-blog-root-to)

