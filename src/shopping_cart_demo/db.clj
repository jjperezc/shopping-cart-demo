(ns shopping-cart-demo.db
  (:require [next.jdbc :as jdbc]))

(def db-spec
  {:dbtype "postgresql"
   :dbname "shopping"
   :host "localhost"
   :user "postgres"
   :password "postgres"})

(def ds (jdbc/get-datasource db-spec))

(defn get-products []
  (jdbc/execute! ds ["SELECT id, name, description FROM products WHERE deleted = FALSE"]))
