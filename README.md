# Bets

Application skeleton with Spring Boot 4, Java 25, PostgreSQL, Liquibase, React, TypeScript, and Vite.

## Layout

```text
bets/
├── backend/
├── frontend/
├── docker-compose.yml
└── settings.gradle.kts
```

## Run PostgreSQL

```bash
docker compose up -d postgres
```

## Run backend

```bash
./gradlew :backend:bootRun
```

The backend listens on `http://localhost:8080`.

## Run frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend listens on `http://localhost:5173` and proxies `/api` to the backend.
