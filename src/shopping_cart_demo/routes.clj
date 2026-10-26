(ns shopping-cart-demo.routes
  (:require [compojure.core :refer [defroutes GET POST wrap-routes]]
            [compojure.route :as route]
            [hiccup.page :refer [html5 include-css]]
            [shopping-cart-demo.auth :as auth]
            [shopping-cart-demo.db :as db]
            [shopping-cart-demo.csv :as csv]
            [hiccup.util :refer [escape-html]]
            [shopping-cart-demo.validation :as validation] 
            [clojure.string :as str]))


(defn wrap-require-admin [handler]
  (fn [req]
    (let [username (get-in req [:session :username])]
      (cond
        (nil? username)
        {:status 302 :headers {"Location" "/"} :body ""}

        (not= username "admin")
        {:status 403 :headers {"Content-Type" "text/html"} :body "Forbidden"}

        :else (handler req)))))

(defn wrap-require-customer [handler]
  (fn [req]
    (let [username (get-in req [:session :username])]
      (cond
        (nil? username)
        {:status 302 :headers {"Location" "/"} :body ""}

        (= username "admin")
        {:status 403 :headers {"Content-Type" "text/html"} :body "Forbidden"}

        :else (handler req)))))

(defn- money [x]
  (format "$%.2f" (bigdec (or x 0))))

(defn base-layout [title username & content]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:title title] 
    (include-css "/static/style.css") ]
   [:body
    [:nav.navbar
     [:a.brand {:href (cond (= username "admin") "/products" username "/catalog" :else "/")}
      "Shopping Cart Demo 🛒"]
     (when (= username "admin")
       (list [:a {:href "/products"} "Products"]
             [:a {:href "/products/bulk"} "Bulk Upload"]))
     (when (and username (not= username "admin"))
       (list [:a {:href "/catalog"} "Shop"]
             [:a {:href "/cart"} "Cart"]
             [:a {:href "/orders"} "Orders"]))
     [:span.spacer]
     (when username
       (list [:span.nav-user (escape-html username)]
             [:a {:href "/logout"} "Logout"]))]
    [:div.container content]]))

(defn- clean-query
  "Only accept a single string, trimmed and capped at 100 chars."
  [q]
  (when (string? q)
    (let [q (str/trim q)]
      (subs q 0 (min 100 (count q))))))

(defn search-form [action q]
  [:form.search-bar {:method "GET" :action action}
   [:input {:type "text" :name "q" :value q
            :placeholder "Search by name, SKU or description"}]
   [:button.btn {:type "submit"} "Search"]
   (when-not (str/blank? q)
     [:a {:href action} "Clear"])])

(defn errors-page [username errors back-href]
  (base-layout "Please fix the errors" username
               [:div.card
                [:h2 "Please fix the following"]
                [:div.alert-error [:ul (for [e errors] [:li (escape-html e)])]]
                [:a.btn {:href back-href} "← Go back"]]))

;; --- Pages ---
(defn login-page
  ([] (login-page nil))
  ([error]
   (base-layout "Login" nil
                [:div.auth-card
                 [:div.auth-logo "🛒"]
                 [:h2 "Welcome back"]
                 [:p.muted "Sign in to start shopping"]
                 (when error [:div.alert-error (escape-html error)])
                 [:form {:method "POST" :action "/login"}
                  [:label "Username"
                   [:input {:type "text" :name "username" :required true :autofocus true}]]
                  [:label "Password"
                   [:input {:type "password" :name "password" :required true}]]
                  [:button.btn.btn-block {:type "submit"} "Sign in"]]])))

(defn product-list-page
  ([products username] (product-list-page products username nil))
  ([products username q]
   (base-layout "List Products" username
                [:div.toolbar
                 [:h2 "Products"]
                 [:div
                  [:a.btn {:href "/products/add"} "+ Add Product"] " "
                  [:a.btn.btn-outline {:href "/products/bulk"} "Bulk Upload"]]]
                (search-form "/products" q)
                (if (empty? products)
                  [:div.empty "No products found."]
                  [:div.table-wrap
                   [:table
                    [:thead
                     [:tr [:th "ID"] [:th "SKU"] [:th "Name"] [:th "Description"]
                      [:th.num "Price"] [:th "Actions"]]]
                    [:tbody
                     (for [{:keys [id sku name description price]} products]
                       [:tr
                        [:td id] [:td (escape-html sku)]
                        [:td (escape-html name)] [:td (escape-html description)]
                        [:td.num (money price)]
                        [:td
                         [:a.btn.btn-sm.btn-outline {:href (str "/products/edit/" id)} "Modify"] " "
                         [:form.inline-form {:method "POST" :action (str "/products/delete/" id)
                                             :onsubmit "return confirm('Delete this product?');"}
                          [:button.btn.btn-sm.btn-danger {:type "submit"} "Delete"]]]])]]]))))

(defn category-select [categories selected-id]
  [:select {:name "category_id" :required true}
   [:option {:value ""} "-- Select category --"]
   (for [{:keys [id name]} categories]
     [:option (cond-> {:value id}
                (= (str id) (str selected-id)) (assoc :selected true))
      (escape-html name)])])

(defn add-product-form [username categories]
  (base-layout "Add Product" username
               [:div.card
                [:h2 "Add Product"]
                [:form {:method "POST" :action "/products/add"}
                 [:label "Name" [:input {:type "text" :name "name" :required true}]]
                 [:label "SKU" [:input {:type "text" :name "sku" :required true}]]
                 [:label "Description" [:input {:type "text" :name "description"}]]
                 [:div.form-grid
                  [:label "Price" [:input {:type "text" :name "price" :required true}]]
                  [:label "Stock" [:input {:type "text" :name "stock" :required true}]]
                  [:label "Weight (kg)" [:input {:type "text" :name "weight"}]]
                  [:label "Category" (category-select categories nil)]]
                 [:div.form-actions
                  [:button.btn {:type "submit"} "Add product"]
                  [:a.btn.btn-outline {:href "/products"} "Cancel"]]]]))

(defn edit-product-form [id name description price stock weight category_id username categories]
  (base-layout "Edit Product" username
               [:div.card
                [:h2 "Edit Product"]
                [:form {:method "POST" :action (str "/products/edit/" id)}
                 [:label "Name" [:input {:type "text" :name "name" :value name :required true}]]
                 [:label "Description" [:input {:type "text" :name "description" :value description}]]
                 [:div.form-grid
                  [:label "Price" [:input {:type "text" :name "price" :value price :required true}]]
                  [:label "Stock" [:input {:type "text" :name "stock" :value stock :required true}]]
                  [:label "Weight (kg)" [:input {:type "text" :name "weight" :value weight}]]
                  [:label "Category" (category-select categories category_id)]]
                 [:div.form-actions
                  [:button.btn {:type "submit"} "Update product"]
                  [:a.btn.btn-outline {:href "/products"} "Cancel"]]]]))

(defn bulk-upload-form [username]
  (base-layout "Bulk Upload" username
               [:div.card
                [:h2 "Bulk Product Upload"]
                [:p.muted "CSV columns: " [:code "name,sku,description,category,price,stock,weight_kg"]]
                [:p.muted "Use the category name exactly as it appears in the catalog."]
                [:form {:method "POST" :action "/products/bulk" :enctype "multipart/form-data"}
                 [:label "CSV file" [:input {:type "file" :name "csvfile" :accept ".csv" :required true}]]
                 [:div.form-actions
                  [:button.btn {:type "submit"} "Upload"]
                  [:a.btn.btn-outline {:href "/products"} "Cancel"]]]]))

(defn bulk-result-page [results username]
  (let [ok (count (filter #(= :success (:status %)) results))]
    (base-layout "Bulk Upload Results" username
                 [:h2 "Bulk Upload Results"]
                 [:p (str ok " inserted, " (- (count results) ok) " failed")]
                 [:div.table-wrap
                  [:table
                   [:thead [:tr [:th "Row"] [:th "Status"] [:th "Message"]]]
                   [:tbody
                    (for [{:keys [row status message]} results]
                      [:tr
                       [:td row]
                       [:td [:span {:class (str "badge badge-" (name status))} (name status)]]
                       [:td (escape-html (or message ""))]])]]]
                 [:p.muted "Results have also been logged to logs/bulk_upload.log"]
                 [:a.btn {:href "/products"} "Back to Products"])))

(defn product-catalog-page
  ([products username cart-id] (product-catalog-page products username cart-id nil))
  ([products username cart-id q]
   (base-layout "Product Catalog" username
                [:div.toolbar
                 [:h2 "Welcome, " (escape-html username)]
                 [:a.btn {:href "/cart"} "🛒 View Cart"]]
                (search-form "/catalog" q)
                (if (empty? products)
                  [:div.empty "No products found."]
                  [:div.product-grid
                   (for [{:keys [id name description price]} products]
                     [:div.product-card
                      [:div.thumb "🛍️"]
                      [:h3 (escape-html name)]
                      [:p.desc (escape-html description)]
                      [:div.price (money price)]
                      [:form.add-row {:method "POST" :action (str "/cart/" cart-id "/add")}
                       [:input {:type "hidden" :name "product_id" :value id}]
                       [:input.qty {:type "number" :name "quantity" :value "1" :min "1" :max "99"}]
                       [:button.btn {:type "submit"} "Add to cart"]]])]))))

(defn cart-page [items cart-id username]
  (let [line  (fn [{:keys [quantity price]}]
                (* (bigdec (or quantity 0)) (bigdec (or price 0))))
        total (reduce + 0M (map line items))]
    (base-layout "Your Cart" username
                 [:div.toolbar
                  [:h2 "Your Cart"]
                  [:a.btn.btn-outline {:href "/catalog"} "← Continue shopping"]]
                 (if (empty? items)
                   [:div.empty "Your cart is empty."]
                   (list
                    [:div.table-wrap
                     [:table
                      [:thead [:tr [:th "Product"] [:th.num "Qty"] [:th.num "Price"] [:th.num "Subtotal"] [:th ""]]]
                      [:tbody
                       (for [{:keys [id name quantity price] :as item} items]
                         [:tr [:td (escape-html name)]
                          [:td.num quantity] [:td.num (money price)] [:td.num (money (line item))]
                          [:td
                           [:form.inline-form {:method "POST" :action (str "/cart/item/" id "/remove")}
                            [:button.btn.btn-sm.btn-danger {:type "submit"} "Remove"]]]])]]]
                    [:div.cart-summary
                     [:span "Total: " [:span.total (money total)]]
                     [:form {:method "POST" :action (str "/cart/" cart-id "/checkout")}
                      [:button.btn {:type "submit"} "Checkout"]]])))))

(defn order-history-page [orders username]
  (base-layout "Order History" username
               [:div.toolbar
                [:h2 "Order History"]
                [:a.btn.btn-outline {:href "/catalog"} "Back to Shop"]]
               (if (empty? orders)
                 [:div.empty "You have no orders yet."]
                 [:div.table-wrap
                  [:table
                   [:thead [:tr [:th "Order ID"] [:th "Date"] [:th.num "Total"] [:th "Details"]]]
                   [:tbody
                    (for [{:keys [id created_at total]} orders]
                      [:tr
                       [:td id]
                       [:td (str created_at)]
                       [:td.num (money total)]
                       [:td [:a.btn.btn-sm {:href (str "/orders/" id)} "View items"]]])]]])))

(defn order-items-page [items order-id username]
  (base-layout "Order Items" username
               [:div.toolbar
                [:h2 "Items in Order " (escape-html (str order-id))]
                [:a.btn.btn-outline {:href "/orders"} "← Back to Orders"]]
               [:div.table-wrap
                [:table
                 [:thead [:tr [:th "Product"] [:th.num "Qty"] [:th.num "Price"]]]
                 [:tbody
                  (for [{:keys [name quantity price]} items]
                    [:tr [:td (escape-html name)] [:td.num quantity] [:td.num (money price)]])]]]))


;; --- Routes ---
;; --- Public routes ---
(defroutes public-routes
  (GET "/" [] (login-page))

  (POST "/login" [username password]
    (if (auth/authenticate username password)
      (if (= username "admin")
        {:status 303 :headers {"Location" "/products"}
         :session (with-meta {:username username} {:recreate true})
         :body ""}
        (let [cart-id (db/create-cart username)]
          {:status 303 :headers {"Location" "/catalog"}
           :session (with-meta {:username username :cart-id cart-id} {:recreate true})
           :body ""}))
      {:status 200
       :body {:status 401 :body (login-page "Invalid username or password")}}))

  (GET "/logout" []
    {:status 302 :headers {"Location" "/"} :session nil :body ""}))

;; --- Admin routes ---
(defroutes admin-routes
  (GET "/products" [q :as {{:keys [username]} :session}]
    (let [q (clean-query q)]
      (product-list-page (db/get-products q) username q)))

  (GET "/products/add" [:as {{:keys [username]} :session}]
    (add-product-form username (db/get-categories)))

  (POST "/products/add" [category_id name sku description price stock weight
                         :as {{:keys [username]} :session}]
    (let [{:keys [errors product]}
          (validation/validate-product {:name name :sku sku :description description
                                        :category category_id :price price
                                        :stock stock :weight_kg weight})]
      (if (seq errors)
        (html5 [:body [:ul (for [e errors] [:li e])]
                (errors-page username errors "/products/add")])
        (do (db/add-product (:category product) (:name product) (:sku product)
                            (:description product) (:price product) (:stock product)
                            (:weight_kg product) username)
            (product-list-page (db/get-products) username)))))

  (GET "/products/edit/:id" [id :as {{:keys [username]} :session}]
    (let [product (first (db/get-product (Integer/parseInt id)))]
      (edit-product-form (:id product) (:name product) (:description product)
                         (:price product) (:stock product) (:weight_kg product)
                         (:category_id product) username (db/get-categories))))

  (POST "/products/edit/:id" [id name description price stock weight category_id
                              :as {{:keys [username]} :session}]
    (let [{:keys [errors product]}
          (validation/validate-product-edit {:name name :description description
                                             :category category_id :price price
                                             :stock stock :weight_kg weight})]
      (if (seq errors)
        (html5 [:body [:ul (for [e errors] [:li e])]
                (errors-page username errors (str "/products/edit/" id))])
        (do (db/update-product (Integer/parseInt id) (:name product) (:description product)
                               (:price product) (:stock product) (:weight_kg product)
                               (:category product) username)
            (product-list-page (db/get-products) username)))))

  (POST "/products/delete/:id" [id :as {{:keys [username]} :session}]
    (db/delete-product (Integer/parseInt id) username)
    {:status 303 :headers {"Location" "/products"} :body ""})

  (GET "/products/bulk" [:as {{:keys [username]} :session}]
    (bulk-upload-form username))

  (POST "/products/bulk" [csvfile :as {{:keys [username]} :session}]
    (let [rows (csv/parse-csv (:tempfile csvfile))
          results (db/bulk-insert-products rows username)]
      (bulk-result-page results username))))

;; --- Customer routes ---
(defroutes customer-routes
  
  (GET "/catalog" [q :as {{:keys [username cart-id]} :session}]
    (let [q (clean-query q)]
      (product-catalog-page (db/get-products q) username cart-id q)))
  
  (GET "/cart" [:as {{:keys [cart-id username]} :session}]
    (cart-page (db/get-cart-items cart-id) cart-id username))

  (GET "/cart/:id" [:as {{:keys [cart-id username]} :session}]
    (cart-page (db/get-cart-items cart-id) cart-id username))

  (POST "/cart/:id/add" [product_id quantity :as {{:keys [cart-id username]} :session}]
    (do (db/add-to-cart cart-id
                        (Integer/parseInt product_id)
                        (Integer/parseInt quantity))
        (cart-page (db/get-cart-items cart-id) cart-id username)))
  
  (POST "/cart/item/:item-id/remove" [item-id :as {{:keys [cart-id]} :session}]
    (db/remove-cart-item cart-id (Integer/parseInt item-id))
    {:status 303 :headers {"Location" "/cart"} :body ""})
  

  (POST "/cart/:id/checkout" [:as {{:keys [username cart-id] :as session} :session}]
    (if (empty? (db/get-cart-items cart-id)) 
      {:status 303 :headers {"Location" "/cart"} :body ""}
      (do (db/checkout-cart cart-id username)
          (let [new-cart-id (db/create-cart username)]
            {:status 303
             :headers {"Location" "/orders"}
             :session (assoc session :cart-id new-cart-id)
             :body ""}))))

  (GET "/orders" [:as {{:keys [username]} :session}]
    (order-history-page (db/get-orders username) username))

  (GET "/orders/:id" [id :as {{:keys [username]} :session}]
    (order-items-page (db/get-order-items (Integer/parseInt id) username) id username)))

;; --- Combined ---
(defroutes app-routes
  public-routes
  (route/resources "/static")
  (wrap-routes admin-routes wrap-require-admin)
  (wrap-routes customer-routes wrap-require-customer)
  (route/not-found "Page not found"))