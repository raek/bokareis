(ns bokareis.plugin-test
  (:use midje.sweet
        bokareis.plugin))

(fact "prefix-blog-root-to adds blog root to given attribute"
  (apply-plugin {:matcher prefix-blog-root-to-node?
                 :transformer expand-prefix-blog-root-to-node}
                {:tag :a
                 :attrs {:href "foo.png"
                         :prefix-blog-root-to "href"}
                 :content ["look at foo!"]}
                {}
                nil)
  => [{:tag :a
       :attrs {:href "ROOT/foo.png"}
       :content ["look at foo!"]}])

