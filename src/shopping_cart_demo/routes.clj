(ns shopping-cart-demo.routes
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [hiccup.page :refer [html5]]
            [shopping-cart-demo.auth :as auth]
            [shopping-cart-demo.db :as db]))


;; --- Pages ---
(defn login-page []
  (html5
   [:head [:title "Login"]]
   [:body
    [:h2 "Sign In"]
    [:form {:method "POST" :action "/login"}
     "Username: " [:input {:type "text" :name "username"}] [:br]
     "Password: " [:input {:type "password" :name "password"}] [:br]
     [:input {:type "submit" :value "Login"} ]]]))

(defn product-list-page [products]
  (html5
   [:head [:title "List Products"]]
   [:body
    [:h2 "Products"]
    [:div {:style "float:right;"}
     [:a {:href "/products/add"} "Add Product"] " | "
     [:a {:href "/products/bulk"} "Bulk Products"]]
    [:table
     [:tr [:th "ID"] [:th "Name"] [:th "Description"] [:th "Actions"]]
     (for [{:keys [id name description]} products]
       [:tr
        [:td id] [:td name] [:td description]
        [:td
         [:a {:href (str "/products/edit/" id)} "Modify"] " | "
         [:a {:href (str "/products/delete/" id)} "Delete"]]])]]))

(defn add-product-form []
  (html5
   [:head [:title "Add Product"]]
   [:body
    [:h2 "Add Product"]
    [:form {:method "POST" :action "/products/add"}
     "Name: " [:input {:type "text" :name "name"}] [:br]
     "SKU: " [:input {:type "text" :name "sku"}] [:br]
     "Description: " [:input {:type "text" :name "description"}] [:br]
     "Price: " [:input {:type "text" :name "price"}] [:br]
     "Stock: " [:input {:type "text" :name "stock"}] [:br]
     "Weight: " [:input {:type "text" :name "weight"}] [:br]
     "Category ID: " [:input {:type "text" :name "category_id"}] [:br]
     [:input {:type "submit" :value "Add"}]]]))

(defn edit-product-form [id name description price stock weight category_id]
  (html5
   [:head [:title "Edit Product"]]
   [:body
    [:h2 "Edit Product"]
    [:form {:method "POST" :action (str "/products/edit/" id)}
     "Name: " [:input {:type "text" :name "name" :value name}] [:br]
     "Description: " [:input {:type "text" :name "description" :value description}] [:br]
     "Price: " [:input {:type "text" :name "price" :value price}] [:br]
     "Stock: " [:input {:type "text" :name "stock" :value stock}] [:br]
     "Weight: " [:input {:type "text" :name "weight" :value weight}] [:br]
     "Category ID: " [:input {:type "text" :name "category_id" :value category_id}] [:br]
     [:input {:type "submit" :value "Update"}]]]))

;; --- Routes ---
(defroutes app-routes
  (GET "/" [] (login-page))

  (POST "/login" [username password]
        (if (auth/authenticate username password)
          (if (= username "admin")
            (product-list-page (db/get-products))
            (html5 [:body [:h2 "Welcome, " username]]))
          (html5 [:body [:h2 "Invalid credentials"]])))
  
  ;; Products --
  ;; Add product
  (GET "/products/add" [] (add-product-form))
  (POST "/products/add" [category_id name sku description price stock weight]
    (do (db/add-product (Integer/parseInt category_id) name sku description
                        (Double/parseDouble price) (Integer/parseInt stock)
                        (Double/parseDouble weight) "admin")
        (product-list-page (db/get-products))))
  
  ;; Edit product
  (GET "/products/edit/:id" [id]
    (let [product (first (db/get-product (Integer/parseInt id)))]
      (edit-product-form (:id product) (:name product) (:description product) (:price product) (:stock product) (:weight_kg product) (:category_id product))))
  (POST "/products/edit/:id" [id name description price stock weight category_id]
    (do (db/update-product (Integer/parseInt id) name description (Double/parseDouble price) (Integer/parseInt stock) (Double/parseDouble weight) (Integer/parseInt category_id))
        (product-list-page (db/get-products))))
  
  ;; Delete product
  (GET "/products/delete/:id" [id]
    (do (db/delete-product (Integer/parseInt id))
        (product-list-page (db/get-products))))

  ;;(POST "/login" request
  ;;(println "CONTENT-TYPE:" (:content-type request))
  ;;(println "FORM-PARAMS:" (:form-params request))
  ;;(println "PARAMS:" (:params request))
  ;;(println "BODY:" (:body request))
  ;;(str "form-params: " (:form-params request)
  ;;     " | params: " (:params request)))
          
  (route/not-found "Page not found"))
