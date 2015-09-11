(ns mooncake.db.mongo
  (:require [monger.core :as mcore]
            [monger.operators :as mop]
            [monger.collection :as mcoll]
            [clojure.tools.logging :as log]
            [clojure.walk :as walk]))

(defprotocol Database
  (fetch [this coll k keywordise?]
    "Find the item based on a key.")
  (fetch-all [this coll keywordise?]
    "Find all items based on a collection.")
  (find-item [this coll query-m keywordise?]
    "Find an item matching the query-map.")
  (find-items-by-key-values [this coll k values keywordise?]
    "Find items whose key 'k' matches one of the given values")
  (store! [this coll item]
    "Store the given map and return it.")
  (store-with-id! [this coll key-param item]
    "Store the given map using the value of the kw key-param and return it.")
  (upsert! [this coll query item]
    "Update item that corresponds to query, or if none exist insert item."))

(defn dissoc-id
  ([item]
   (dissoc-id item true))
  ([item keywordise?]
   (dissoc item (if keywordise? :_id "_id"))))

(defn key-values->mongo-query-map [k values]
  {k {mop/$in values}})

(defrecord MongoDatabase [mongo-db]
  Database
  (fetch [this coll k keywordise?]
    (when k
      (-> (mcoll/find-map-by-id mongo-db coll k [] keywordise?)
          (dissoc-id keywordise?))))
  (fetch-all [this coll keywordise?]
    (let [result-m (->> (mcoll/find-maps mongo-db coll)
                        (map dissoc-id))]
      (if keywordise?
        result-m
        (map walk/stringify-keys result-m))))
  (find-item [this coll query-m keywordise?]
    (when query-m
      (-> (mcoll/find-one-as-map mongo-db coll query-m [] keywordise?)
          (dissoc-id keywordise?))))
  (find-items-by-key-values [this coll k values keywordise?]
    (if values
      (let [mongo-key-in-query (key-values->mongo-query-map k values)
            result-m (->> (mcoll/find-maps mongo-db coll mongo-key-in-query)
                          (map dissoc-id))]
        (if keywordise?
          result-m
          (map walk/stringify-keys result-m)))
      []))
  (store! [this coll item]
    (-> (mcoll/insert-and-return mongo-db coll item)
        (dissoc :_id)))
  (store-with-id! [this coll key-param item]
    (->> (assoc item :_id (key-param item))
         (store! this coll)))
  (upsert! [this coll query item]
    (mcoll/upsert mongo-db coll query item)))

(defn create-database [mongodb]
  (MongoDatabase. mongodb))

(defn get-mongo-db-and-conn [mongo-uri]
  (log/debug "Connecting to mongo.")
  (let [db-and-conn (mcore/connect-via-uri mongo-uri)]
    (log/debug "Connected to mongo.")
    db-and-conn))

(defn get-mongo-db [mongo-uri]
  (:db (get-mongo-db-and-conn mongo-uri)))