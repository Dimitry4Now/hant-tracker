# hant-tracker

## Structure

- `design/` — design canvas artboards (`.dc.html`) and reference mockups
- `frontend/` — Angular app
- `api/` — Spring Boot API (Java 21, Gradle, PostgreSQL, JPA/Hibernate)

## Frontend

```
cd frontend
npm install
npm start
```

## API

Requires a PostgreSQL database. Configure connection in
`api/src/main/resources/application.properties`.

```
cd api
./gradlew bootRun
```
