# logd
Simple and fast log storage.

## Quick start
```
$ cp example-config.yml logd.yml
$ vim logd.yml
$ psql
$> CREATE ROLE logd WITH LOGIN PASSWORD 'password';
$> CREATE DATABASE logd WITH OWNER logd;
$> \c logd
$> CREATE EXTENSION pg_trgm;
$> CREATE EXTENSION hstore;
$> CREATE TABLE events (
    id serial PRIMARY KEY,
    ts TIMESTAMP WITH TIME ZONE NOT NULL,
    message TEXT NOT NULL,
    attrs hstore
);
$ gradle shadowJar
$ java -jar build/libs/logd-all.jar migrate
$ java -jar build/libs/logd-all.jar serve

$ curl -XPOST -d '[{"ts":"2020-02-23T22:21:40+03:00","message":"Hello, logd!","attrs":{"level": "info"}}]' localhost:8080/events/json
$ http ':8080/events/search' 'from==2020-02-23T00:00:00+03:00'
$ http ':8080/events/search' 'from==2020-02-23T00:00:00+03:00' 'text==logd'
```

