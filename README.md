# 🧠 Eisenhower Matrix Web App

This is an open-source web application to create and manage Eisenhower matrices, also known as Priority matrices or Important-Urgent matrices. See it live on *[eisenhowermatrix.net](https://eisenhowermatrix.net)*.

Powered by Spring Boot and Web Components.

Matrix board             |  Tasks memo editor
:-------------------------:|:-------------------------:
![](https://user-images.githubusercontent.com/12188892/211003437-86d7ff28-45bd-4eb1-88b7-cab49ca40718.png) | ![](https://user-images.githubusercontent.com/12188892/211003448-c8f6b9bc-0a2a-46da-850d-3c21a7f13afa.png)





## Prerequisites
- Java 17+
- Maven
- [Microsoft SQL Server 2019](https://www.microsoft.com/fr-fr/sql-server/sql-server-downloads)

### Environment variables
- `JDBC_URL`: Database JDBC url for your SQL Server instance
- `DATABASE_USER`: Database user
- `DATABASE_PASSWORD`: Database password
- `JWT_SIGNING_KEY`: Secret key for signing your JWTs

## How to run

### Local

```
mvn compile
mvn spring-boot:run
```

### Docker image

```
mvn package
docker run rg.fr-par.scw.cloud/achampion/eisenhower-server:latest
```
