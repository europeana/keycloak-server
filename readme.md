Keycloak Server in a Spring-Boot App 
------------------------------------

To start the application you'll need to build the project with: 
```
mvn package
```

and start the Spring-Boot app:
```
java -jar target/keycloak-server-*.jar
```

The embedded Keycloak server is now reachable via http://localhost:8080/auth.

By default it will use an in-memory database, but you can specify a postgres 
database in the keycloak.properties file.




