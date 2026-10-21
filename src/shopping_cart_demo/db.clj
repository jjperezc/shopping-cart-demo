(ns shopping-cart-demo.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(def db-spec
  {:dbtype "postgresql"
   :dbname "shopping"
   :host "localhost"
   :user "postgres"
   :password "postgres"})

;;(def ds (jdbc/get-datasource db-spec))
(def ds
  (jdbc/with-options (jdbc/get-datasource db-spec)
    {:builder-fn rs/as-unqualified-maps}))

(defn get-products []
  (jdbc/execute! ds ["SELECT id, name, description FROM products WHERE deleted = FALSE"]))

(defn get-product [id]
  (jdbc/execute! ds ["SELECT id, name, description, price, stock, weight_kg, category_id FROM products WHERE id = ?" id]))

(defn add-product [category-id name sku description price stock weight created-by]
  (jdbc/execute! ds
                 ["INSERT INTO products (category_id, name, sku, description, price, stock, weight_kg, created_by)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                  category-id name sku description price stock weight created-by]))

(defn update-product [id name description price stock weight category-id]
  (jdbc/execute! ds
                 ["UPDATE products SET name = ?, description = ?, price = ?, stock = ?, weight_kg = ?, category_id = ? WHERE id = ?" name description price stock weight category-id id]))

(defn delete-product [id]
  (jdbc/execute! ds
                 ["UPDATE products SET deleted = TRUE WHERE id = ?" "admin" id]))