# logd
Simple and fast log storage.

## Quick start
```bash
$ cp example-config.yml logd.yml
$ arangosh
127.0.0.1:8529@_system> db._createDatabase("logd")
127.0.0.1:8529@_system> var users = require("@arangodb/users")
127.0.0.1:8529@_system> users.save("root@logd", "password")
127.0.0.1:8529@_system> users.grantDatabase("root@logd", "logd")
$ gradle shadowJar
$ java -jar build/libs/logd-all.jar initdb
$ java -jar build/libs/logd-all.jar serve

$ curl -XPOST -d '{"ts":"2020-02-23T22:21:40+03:00","message":"It works!","attrs":{"key": "value"}}' localhost:8080/events/json
```
