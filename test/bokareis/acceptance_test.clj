(ns bokareis.acceptance-test
  (:use midje.sweet
        bokareis.main
        bokareis.io
        [bokareis.util :only [with-temp-dir html-contains create-post]]
        [clojure.java.io :only [file] :rename {file f}]
        [clojure.data.json :only [json-str]]))

(def default-templates
  {"post.html" "<post-text></post-text>"
   "index.html" "<posts></posts>"})

(defn create-blog-fixture
  ([root]
     (create-blog-fixture root {}))
  ([root templates]
     (let [templates (merge default-templates templates)]
       (doseq [page ["post.html" "index.html"]]
         (write-text-file (get templates page)
                          (f root "templates" page))))))

(fact "it renders index page with list of posts"
  (with-temp-dir root
    (create-blog-fixture root)
    (create-post (f root "posts")
                 (json-str {"slug" "hello-world"
                            "published" "2012-02-11T14:47:00Z"})
                 "# Hello world")
    (-main (str root))
    (slurp (f root "out" "index.html"))
    => (contains "<a href=\"/2012/02/11/hello-world/\">Hello world</a>")))

(fact "it renders index page using a template"
  (with-temp-dir root
    (create-blog-fixture root {"index.html" "This is the index."})
    (-main (str root))
    (slurp (f root "out" "index.html"))
    => (contains "This is the index.")))

(fact "it renders title on post pages"
  (with-temp-dir root
    (create-blog-fixture root)
    (create-post (f root "posts")
                 (json-str {"slug" "hello-world"
                            "published" "2012-02-11T14:47:00Z"})
                 "# Hello world")
    (-main (str root))
    (slurp (f root "out" "2012" "02" "11" "hello-world" "index.html"))
    => (contains "<h1>Hello world</h1>")))

(fact "it expands links to other posts"
  (with-temp-dir root
    (create-blog-fixture root)
    (create-post (f root "posts" "one")
                 (json-str {"slug" "post-one"
                            "published" "2012-02-11T14:47:00Z"})
                 "# Post One")
    (create-post (f root "posts" "two")
                 (json-str {"slug" "post-two"
                            "published" "2012-02-11T14:48:00Z"})
                 "# Post Two\n<post-link to=\"post-one\"></post-link>")
    (-main (str root))
    (slurp (f root "out" "2012" "02" "11" "post-two" "index.html"))
    => (contains "<a href=\"/2012/02/11/post-one/\">Post One</a>")))

(fact "it renders a post page using a template"
  (with-temp-dir root
    (create-blog-fixture root {"post.html" "This is a post."})
    (create-post (f root "posts")
                 (json-str {"slug" "hello-world"
                            "published" "2012-02-11T14:47:00Z"})
                 "")
    (-main (str root))
    (slurp (f root "out" "2012" "02" "11" "hello-world" "index.html"))
    => (contains "This is a post.")))