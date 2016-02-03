# Overview

Simple authentication service.

## Compile

``` mvn package```

## Run

```
java -jar service/target/auth-service-0.1-SNAPSHOT.jar
curl http://localhost:8080/ping```
```

## Build and run in container

```
cd service; mvn docker:build
docker run -it --rm -p 8080 auth-service
docker ps # Check assigned port
curl http://localhost:[port]/ping
```

## Run the client

```
cd bin

./auth-client register -u user1 -p 12345 -f Me

./auth-client login -u user1 -p 12345
Status code: OK
Payload: {"status":"200","session":"dXNlcjE=|MjAxNi0wMi0wNFQyMDo0NDowOS45NDla|pfTsGzEu1eRdkFI+24VRWTh6YHBaN9exy7bNulHU8FOWOiFtF5mh6dFbrRREEm8p","username":"user1"}

./auth-client activity -u user1 -a user1 -s 'dXNlcjE=|MjAxNi0wMi0wNFQyMDo0NDowOS45NDla|pfTsGzEu1eRdkFI+24VRWTh6YHBaN9exy7bNulHU8FOWOiFtF5mh6dFbrRREEm8p'
Status code: OK
Payload: {"logins":["2016-02-03T20:35:42.178Z","2016-02-03T20:36:07.771Z","2016-02-03T20:39:13.719Z","2016-02-03T20:42:38.007Z","2016-02-03T20:44:09.945Z"]}
```



## Sample queries

```
curl -v -X POST -d '{"username":"myname","password":"pass"}' http://localhost:8080/v0/login

curl -v -X POST -d '{"username":"eu2","password":"1234","fullname":"MI"}' http://localhost:8080/v0/register

```

# TODO
  - Validate and encode all input data to avoid code injection
  - HTTPS
  - Adapt auth-client for system tests
  - Move client to its own module (+common module)
  - Use QueryBuilder from datastax driver

