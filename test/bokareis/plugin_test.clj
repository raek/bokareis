(ns bokareis.plugin-test
  (:use midje.sweet
        bokareis.main))

(fact "prefix-blog-root-to adds blog root to given attribute"
  (apply-plugin prefix-blog-root-to-plugin
                {:tag :a
                 :attrs {:href "foo.png"
                         :prefix-blog-root-to "href"}
                 :content ["look at foo!"]}
                {}
                nil)
  => [{:tag :a
       :attrs {:href "ROOT/foo.png"}
       :content ["look at foo!"]}])

