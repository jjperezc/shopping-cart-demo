(ns shopping-cart-demo.core
  (:require [ring.adapter.jetty :refer [run-jetty]] 
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [shopping-cart-demo.routes :refer [app-routes]])
  (:gen-class))

(def app
  (-> app-routes
      wrap-keyword-params
      wrap-params
      wrap-multipart-params))

(defn -main []
  (run-jetty app {:port 3000 :join? false}))