# sentiment

API for User Sentiments, wrapped around DynamoDB.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

Otherwise:

    lein ring uberjar
    java -jar target/*-standalone.jar

## License

Copyright © 2015 Colin Webb 
