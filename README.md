# Overview

Simple authentication service.

## Compile

``` mvn package```

## Run

```java -jar service/target/auth-service-0.1-SNAPSHOT.jar
curl http://localhost:8080/ping```

## Build and run in container

```
cd service; mvn docker:build
docker run -it --rm -p 8080 auth-service
docker ps # Check assigned port
curl http://localhost:[port]/ping
```

## Sample queries
```
curl -v -X POST -d '{"username":"myname","password":"pass"}' http://localhost:8080/v0/login

curl -v -X POST -d '{"username":"eu2","password":"1234","fullname":"MI"}' http://localhost:8080/v0/register

```

# TODO
  - Middleware json serialization
  - Middleware EndpointException ?
  - HTTPS
  - Use auth-client for system tests
  - Move client to its own module (+common module)
  - hash(username) == userid primary key for table

