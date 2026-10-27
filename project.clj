(defproject shopping-cart-demo "0.1.0-SNAPSHOT"
  :description "Shopping cart demo"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [ring "1.10.0"]
                 [compojure "1.7.0"]
                 [hiccup "1.0.5"]
                 [seancorfield/next.jdbc "1.2.659"]
                 [org.postgresql/postgresql "42.7.3"]
                 [org.clojure/data.csv "1.1.0"]]
  :main shopping-cart-demo.core
  :uberjar-name "shopping-cart-demo.jar"
  :profiles {:uberjar {:aot :all}})
