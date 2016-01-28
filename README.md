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
