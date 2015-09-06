(defproject sentiment "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [compojure "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-json "0.4.0"]
                 [clj-time "0.11.0"]
                 [com.taoensso/faraday "1.7.1"]
                 [prismatic/schema "0.4.4"]
                 [metosin/schema-tools "0.5.2"]
                 [dire "0.5.3"]]
  :plugins [[lein-ring "0.9.6"]
            [lein-dynamodb-local "0.2.6"]]
  :ring {:handler sentiment.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [cheshire "5.5.0"]
                        [clj-http "2.0.0"]
                        [ring/ring-jetty-adapter "1.4.0"]]}})

