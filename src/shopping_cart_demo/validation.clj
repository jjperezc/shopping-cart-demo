(ns shopping-cart-demo.validation
  (:require [clojure.string :as str]))

(defn- parse-int [s]
  (try (Integer/parseInt (str/trim s)) (catch Exception _ nil)))

;;(defn- parse-double [s]
;;  (try (Double/parseDouble (str/trim s)) (catch Exception _ nil)))

(defn validate-product
  "Returns {:errors [...]} or {:product {...parsed values...}}"
  [{:keys [name sku description category price stock weight_kg]}]
  (let [name        (str/trim (or name ""))
        sku         (str/trim (or sku ""))
        description (str/trim (or description ""))
        category    (when-not (str/blank? category) (parse-int category))
        price       (when-not (str/blank? price) (parse-double price))
        stock       (when-not (str/blank? stock) (parse-int stock))
        weight      (when-not (str/blank? weight_kg) (parse-double weight_kg))
        errors
        (cond-> []
          (or (str/blank? name) (> (count name) 100))
          (conj "Name is required (max 100 characters)")

          (not (re-matches #"[A-Za-z0-9._-]{1,50}" sku))
          (conj "SKU must be 1-50 characters: letters, digits, . _ -")

          (> (count description) 500)
          (conj "Description too long (max 500 characters)")

          (re-find #"\p{Cntrl}" (str name description))
          (conj "Control characters are not allowed")

          (or (nil? category) (neg? category))
          (conj "Category must be a positive whole number")

          (or (nil? price) (neg? price) (> price 1000000))
          (conj "Price must be a number between 0 and 1,000,000")

          (or (nil? stock) (neg? stock) (> stock 1000000))
          (conj "Stock must be a whole number between 0 and 1,000,000")

          (and (not (str/blank? weight_kg)) (or (nil? weight) (neg? weight)))
          (conj "Weight must be a positive number"))]
    (if (seq errors)
      {:errors errors}
      {:product {:name name :sku sku :description description
                 :category category :price price :stock stock :weight_kg weight}})))

(defn validate-product-edit
  "Returns {:errors [...]} or {:product {...parsed values...}}"
  [{:keys [name description category price stock weight_kg]}]
  (let [name        (str/trim (or name ""))
        description (str/trim (or description ""))
        category    (when-not (str/blank? category) (parse-int category))
        price       (when-not (str/blank? price) (parse-double price))
        stock       (when-not (str/blank? stock) (parse-int stock))
        weight      (when-not (str/blank? weight_kg) (parse-double weight_kg))
        errors
        (cond-> []
          (or (str/blank? name) (> (count name) 100))
          (conj "Name is required (max 100 characters)")

          (> (count description) 500)
          (conj "Description too long (max 500 characters)")

          (re-find #"\p{Cntrl}" (str name description))
          (conj "Control characters are not allowed")

          (or (nil? category) (neg? category))
          (conj "Category must be a positive whole number")

          (or (nil? price) (neg? price) (> price 1000000))
          (conj "Price must be a number between 0 and 1,000,000")

          (or (nil? stock) (neg? stock) (> stock 1000000))
          (conj "Stock must be a whole number between 0 and 1,000,000")

          (and (not (str/blank? weight_kg)) (or (nil? weight) (neg? weight)))
          (conj "Weight must be a positive number"))]
    (if (seq errors)
      {:errors errors}
      {:product {:name name :description description
                 :category category :price price :stock stock :weight_kg weight}})))