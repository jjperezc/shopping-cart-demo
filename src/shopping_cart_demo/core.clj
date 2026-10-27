(ns shopping-cart-demo.core
  (:require [ring.adapter.jetty :refer [run-jetty]] 
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.session :refer [wrap-session]]
            [shopping-cart-demo.routes :refer [app-routes]])
  (:gen-class))

(def app
  (-> app-routes
      wrap-keyword-params
      wrap-params
      wrap-multipart-params
      (wrap-session {:cookie-attrs {:http-only true :same-site :lax}})))

(defn -main []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (run-jetty app {:port port :join? true})))