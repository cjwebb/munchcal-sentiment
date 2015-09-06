(ns sentiment.handler-test
  (:require [clojure.test :refer :all]
            [sentiment.handler :refer [app]]
            [clj-http.client :as client]
            [ring.adapter.jetty :refer [run-jetty]]))

(def test-port 5744)
(def test-host "localhost")
(def base-url (str "http://" test-host ":" test-port))

(defn start-server []
  (loop [server (run-jetty app {:port test-port
                                :join? false})]
    (if (.isStarted server)
      server
      (recur server))))

(defn stop-server [server]
  (.stop server))

(defn http-get [url]
  (client/get url {:throw-exceptions false :as :json}))

(defn http-post [url body]
  (client/post url {:form-params body :content-type :json}))

(deftest sentiment-app
  (testing "home route returns application info"
    (let [server (start-server)
          response (http-get base-url)]
      (is (= (:status response) 200))
      (is (= (:body response) {:application-name "sentiment"}))
      (stop-server server)))

  (testing "not-found route"
    (let [server (start-server)
          response (http-get (str base-url "/invalid"))]
      (is (= (:status response) 404))
      (stop-server server)))

  (testing "roundtrip post-get favourites"
    (let [server (start-server)
          post-response (http-post (str base-url "/favourites")
                                   {:user-id "id-a"
                                    :recipe {:id "id123"
                                             :name "Recipe A"}})
          get-response (http-get (str base-url "/favourites?user-id=id"))]
      (is (= (:status post-response) 201))
      (is (= (:status get-response) 200))
      (stop-server server))))

