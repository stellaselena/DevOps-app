# Book Api
The purpose of this app is to demonstrate DevOps [infra](https://github.com/athenus-rufus/exam-infra) for PGR301 exam.

### Postman collection
[![Run in Postman](https://run.pstmn.io/button.svg)](https://app.getpostman.com/run-collection/f400859b1ba3d94e4fee)

### Swagger documentation
Once the application is running, swagger documentation can be accessed from [swagger-ui.html](http://localhost:8081/swagger-ui.html#/book-controller)

### Docker
From root directory:
- Run `mvn clean install`
- Build an image `docker build --build-arg JAR_FILE=target/book-1.0-SNAPSHOT.jar .`
- Run image as container `docker run -p 4000:8081 <image id>`

### Metrics

| Type          | Name                 | Endpoint    |     
|:-------------:| ---------------------|:------------------------------------:|
| Meter         | books                | `POST` `GET` `/books`                   |
| Meter         | books                | `GET` `PUT` `PATCH` `DELETE` `/books/{id}`| 
| Meter         | books                | `GET` `/books/name/{name}`             | 
| Meter         | books                | `PATCH` `/books/{id}/price`            | 
| Counter       | greeting             | `GET` `/`                              |
| Counter       | books available      | `POST` `DELETE` `/books`                | 
| Histogram     | books price change   | `PATCH` `/books/{id}/price`            | 
| Histogram     | request body length  | `PATCH` `/books/{id}`                  | 
| Gauge         | books count          | `DELETE` `/books/{id}`                 | 
| Timer         | merge patch book     | `PATCH` `/books/{id}`                  | 

  
