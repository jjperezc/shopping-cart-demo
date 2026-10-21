(ns shopping-cart-demo.routes
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [hiccup.page :refer [html5]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [shopping-cart-demo.auth :as auth]
            [shopping-cart-demo.db :as db]))

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

(defroutes app-routes
  (GET "/" [] (login-page))
  (POST "/login" [username password]
        (if (auth/authenticate username password)
          (if (= username "admin")
            (product-list-page (db/get-products))
            (html5 [:body [:h2 "Welcome, " username]]))
          (html5 [:body [:h2 "Invalid credentials"]])))

  ;;(POST "/login" request
  ;;(println "CONTENT-TYPE:" (:content-type request))
  ;;(println "FORM-PARAMS:" (:form-params request))
  ;;(println "PARAMS:" (:params request))
  ;;(println "BODY:" (:body request))
  ;;(str "form-params: " (:form-params request)
  ;;     " | params: " (:params request)))
          
  (route/not-found "Page not found"))
