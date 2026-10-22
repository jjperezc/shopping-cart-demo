(ns shopping-cart-demo.csv
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

(defn parse-csv [file]
  (with-open [reader (io/reader file)]
    (let [rows (csv/read-csv reader)
          headers (map keyword (first rows))]
      (mapv #(zipmap headers %) (rest rows)))))