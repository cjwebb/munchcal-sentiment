(ns sentiment.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :as middleware]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [taoensso.faraday :as db]))

(def db-opts
  {:access-key (System/getProperty "MC_AWS_ACCESS_KEY")
   :secret-key (System/getProperty "MC_AWS_SECRET_KEY")
   :endpoint "https://dynamodb.eu-west-1.amazonaws.com"})

;; -- data conversion
(defn munge-data [data]
  (assoc data :recipe-id (get-in data [:recipe :id])))

(defn un-munge [data]
  (dissoc data :recipe-id))

;; -- database access
(defn insert! [data]
  (let [now (f/unparse (f/formatters :date-time) (t/now))
        d (assoc data :datetime now)]
    (do 
      (db/put-item db-opts :mc-sen-user-favs d)
      d)))

(defn get! [data]
  (db/query db-opts :mc-sen-user-favs
            {:user-id [:eq (:user-id data)]
             :datetime [:lt (:from data)]}))

;; -- routes
;; todo - set 'from' value when it doesn't exist
;;      - ensure user-id is specified
;;      - return pagination key
;;      - define a limit (default 9)
(defn get-user-favourites [req]
  (let [{{user-id :user-id from :from} :params} req]
    {:body 
      {:data 
        (map un-munge (get! {:user-id user-id :from from}))}}))

;; todo - schema validation
(defn post-user-favourites [req]
  (let [{post-body :body} req,
        {{id :id} :params} req]
    {:body (insert! (munge-data post-body))}))

;; todo - use a schema
(def home-info
  {:favourites {:path "/favourites"
                :get {:params {:user-id "user id, uuid"
                               :from "pagination key, date-time"}}
                :post {:params {:user-id "user id, uuid"
                                :recipe "recipe data, json"}}}})

(defroutes app-routes
  (GET "/" [] {:body home-info})
  (GET "/favourites" req (get-user-favourites req))
  (POST "/favourites" req (post-user-favourites req))
  (route/not-found "Not Found"))

;; todo - error handling
(def app
  (wrap-defaults 
    (middleware/wrap-json-body
      (middleware/wrap-json-response app-routes) {:keywords? true}) api-defaults))

