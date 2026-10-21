(ns shopping-cart-demo.auth)

(def users {"admin" "admin123"
            "user1" "user123"
            "user2" "user234"
            "user3" "user345"})

(defn authenticate [username password]
  (= (get users username) password))
  