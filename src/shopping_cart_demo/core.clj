(ns shopping-cart-demo.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [shopping-cart-demo.routes :refer [app-routes]])
  (:gen-class))

(defn -main []
  (run-jetty app-routes {:port 3000 :join? false}))
