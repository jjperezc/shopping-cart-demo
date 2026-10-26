(ns shopping-cart-demo.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [clojure.string :as str]
            [shopping-cart-demo.validation :as validation]))

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

(def log-file (str "logs/bulk_upload_" (.format (java.time.LocalDateTime/now) (java.time.format.DateTimeFormatter/ofPattern "yyyyMMdd_HHmmss")) ".log"))

(defn log-result [result]
  (spit log-file
        (str (java.time.LocalDateTime/now) " | Row " (:row result)
             " | Status: " (:status result)
             (when (:message result) (str " | Message: " (:message result)))
             "\n")
        :append true))

(defn- like-pattern
  "Builds an ILIKE pattern, escaping user-typed % _ and \\ so they match literally."
  [q]
  (str "%" (str/replace (str/trim q) #"([\\%_])" "\\\\$1") "%"))

(defn get-products
  ([] (get-products nil))
  ([q]
   (if (str/blank? q)
     (jdbc/execute! ds
                    ["SELECT id, sku, name, description, price
         FROM products WHERE deleted = FALSE ORDER BY name"])
     (let [p (like-pattern q)]
       (jdbc/execute! ds
                      ["SELECT id, sku, name, description, price
           FROM products
           WHERE deleted = FALSE
             AND (name ILIKE ? OR sku ILIKE ? OR description ILIKE ?)
           ORDER BY name" p p p])))))

(defn get-product [id]
  (jdbc/execute! ds ["SELECT id, name, description, price, stock, weight_kg, category_id FROM products WHERE id = ?" id]))

(defn add-product [category-id name sku description price stock weight created-by]
  (jdbc/execute! ds
                    ["INSERT INTO products (category_id, name, sku, description, price, stock, weight_kg, created_by)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                  category-id name sku description price stock weight created-by]))

(defn update-product [id name description price stock weight category-id updated-by]
  (jdbc/execute! ds
                   ["UPDATE products SET name = ?, description = ?, price = ?, stock = ?, weight_kg = ?, category_id = ?, updated_by = ? 
                     WHERE id = ?" name description price stock weight category-id updated-by id]))

(defn delete-product [id updated-by]
  (jdbc/execute! ds
                   ["UPDATE products SET deleted = TRUE, updated_by = ? WHERE id = ?" updated-by id]))

(defn get-categories []
  (jdbc/execute! ds ["SELECT id, name FROM categories WHERE deleted = FALSE ORDER BY name"]))

(defn- category-lookup
  "Map of lower-cased category name -> id, loaded once per upload."
  []
  (into {}
        (map (fn [{:keys [id name]}]
               [(str/lower-case (str/trim name)) id]))
        (get-categories)))

(defn bulk-insert-products [rows created-by]
  (let [categories (category-lookup)]
    (vec
     (map-indexed
      (fn [idx row]
        (let [row-num  (inc idx)
              cat-name (some-> (:category row) str/trim)
              cat-id   (get categories (some-> cat-name str/lower-case))
              
              row      (assoc row :category (some-> cat-id str))
              {:keys [errors product]} (validation/validate-product row)
              result
              (cond
                (str/blank? cat-name)
                {:row row-num :status :error :message "Category is required"}

                (nil? cat-id)
                {:row row-num :status :error
                 :message (str "Unknown category: " cat-name)}

                (seq errors)
                {:row row-num :status :error :message (str/join "; " errors)}

                :else
                (try
                  (jdbc/execute! ds
                                 ["INSERT INTO products (category_id, name, sku, description, price, stock, weight_kg, created_by)
                      VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                                  (:category product) (:name product) (:sku product)
                                  (:description product) (:price product) (:stock product)
                                  (:weight_kg product) created-by])
                  {:row row-num :status :success :message "Inserted successfully"}
                  (catch Exception e
                    {:row row-num :status :error :message "Could not insert row"})))]
          (log-result result)
          result))
      rows))))

(defn create-cart [username]
  (let [result (jdbc/execute! ds
                              ["INSERT INTO carts (created_by) VALUES (?) RETURNING id" username])]
    (:id (first result))))

(defn add-to-cart [cart-id product-id quantity]
  (let [product (first (jdbc/execute! ds ["SELECT price FROM products WHERE id = ?" product-id]))]
    (jdbc/execute! ds
                   ["INSERT INTO cart_items (cart_id, product_id, quantity, price)
                     VALUES (?, ?, ?, ?)
                     ON CONFLICT (cart_id, product_id)
                     DO UPDATE SET quantity = cart_items.quantity + EXCLUDED.quantity"
                    cart-id product-id quantity (:price product)])))

(defn remove-cart-item [cart-id item-id]
  (jdbc/execute! ds
                 ["DELETE FROM cart_items WHERE id = ? AND cart_id = ?" item-id cart-id]))

(defn get-cart-items [cart-id]
  (jdbc/execute! ds
                 ["SELECT ci.id, p.name, ci.quantity, ci.price
                   FROM cart_items ci
                   JOIN products p ON ci.product_id = p.id
                   WHERE ci.cart_id = ?" cart-id]))

(defn checkout-cart [cart-id username]
  (jdbc/with-transaction [tx ds]
    (let [order (first (jdbc/execute! tx
                                      ["INSERT INTO orders (cart_id, created_by, total)
                           SELECT id, ?, total_price FROM carts WHERE id = ? RETURNING id"
                                       username cart-id]
                                      {:builder-fn rs/as-unqualified-maps}))]
      (jdbc/execute! tx
                     ["INSERT INTO order_items (order_id, product_id, quantity, price)
          SELECT ?, product_id, quantity, price FROM cart_items WHERE cart_id = ?"
                      (:id order) cart-id])
      (:id order))))

(defn get-orders [username]
  (jdbc/execute! ds
                 ["SELECT id, created_at, total
      FROM orders
      WHERE created_by = ?
      ORDER BY created_at DESC" username]))

(defn get-order-items [order-id username]
  (jdbc/execute! ds
                 ["SELECT oi.id, p.name, oi.quantity, oi.price
        FROM order_items oi
        JOIN products p ON oi.product_id = p.id
        JOIN orders o ON oi.order_id = o.id
        WHERE oi.order_id = ? AND o.created_by = ?" order-id username]))