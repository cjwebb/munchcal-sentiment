(ns sentiment.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :as middleware]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [taoensso.faraday :as db]
            [schema.core :as s]
            [schema.coerce :as coerce]
            [schema-tools.core :as st]
            [dire.core :refer :all]))

(def db-opts
  {:access-key (System/getenv "MC_AWS_ACCESS_KEY")
   :secret-key (System/getenv "MC_AWS_SECRET_KEY")
   :endpoint "https://dynamodb.eu-west-1.amazonaws.com"})
;   :endpoint (System/getenv "MC_AWS_DYNAMODB_ENDPOINT")})

;; -- data conversion / generation
(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn datetime-now []
  (f/unparse (f/formatters :date-time) (t/now)))

(defn munge-data [data]
  (assoc data :recipe-id (get-in data [:recipe :id])))

(defn un-munge [data]
  (dissoc data :recipe-id))

;; -- database access
(defn insert! [data]
  (let [generated {:datetime (datetime-now)
                   :id (uuid)}
        d (merge data generated)]
    (do 
      (db/put-item db-opts :mc-sen-user-favs d)
      generated)))

(defn get! [data]
  (db/query db-opts :mc-sen-user-favs
            {:user-id [:eq (:user-id data)]
             :datetime [:lt (:marker data)]}
            {:order :desc}))

(defn delete!
  "Firstly, query secondary index to find hash & range,
   and then conditionally delete item"
  [id]
  (let [rows (db/query db-opts :mc-sen-user-favs
                       {:id [:eq id]}
                       {:index "id-index"})
        item (first rows)]
    (db/delete-item db-opts :mc-sen-user-favs
                    {:user-id (:user-id item)
                     :datetime (:datetime item)}
                    {:expected {:id [:eq id]}})))

;; -- routes
;; todo - handler marker + limit properly
(s/defschema GetRequest
  {:params
   {:user-id s/Str
    (s/optional-key :marker) s/Str
    (s/optional-key :limit) s/Str}})

(defn default-marker []
  (f/unparse (f/formatters :date-time)
             (t/plus (t/now) (t/days 1))))

(defn calculate-marker [results]
  (when-let [marker (:datetime (last results))]
    {:marker marker}))

(defn get-user-favourites [req]
  (let [params  (st/select-schema req GetRequest)
        data    (merge {:marker (default-marker)} (:params params))
        results (map un-munge (get! data))
        marker  (calculate-marker results)]
    {:body
      (merge {:data results} marker)}))

(s/defschema PostRequest
  {:user-id s/Str
   :recipe {:id s/Str s/Any s/Any}})

(defn post-user-favourites [req]
  (let [{post-body :body} req
        data (st/select-schema post-body PostRequest)]
    {:status 201
     :body (insert! (munge-data post-body))}))

(defn delete-user-favourites [id]
  (do
    (delete! id)
    {:status 204}))

(defroutes app-routes
  (GET "/" [] {:body {:application-name "sentiment"}})
  (GET "/favourites" req (get-user-favourites req))
  (POST "/favourites" req (post-user-favourites req))
  (DELETE "/favourites/:id" [id] (delete-user-favourites id))
  (route/not-found "Not Found"))

(with-handler! #'app-routes
  java.lang.Throwable
  (fn [e & args]
    (do
      (println e)
      {:status 500 :body {:error {:message (.getMessage e)}}})))

(def app
  (wrap-defaults 
    (middleware/wrap-json-body
      (middleware/wrap-json-response app-routes) {:keywords? true}) api-defaults))

