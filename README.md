# Bets

Приложение для генерации бросков по двум Kafka-партициям и расчета победителя.

Стек:
- backend: Spring Boot 4, Java 25, Kafka;
- frontend: React, TypeScript, Vite;
- инфраструктура: Docker Compose, Kafka UI.

## Структура

```text
bets/
├── backend/
├── frontend/
├── docker-compose.yml
└── settings.gradle.kts
```

## Запуск через Docker

```bash
docker compose up --build -d
```

После запуска:
- frontend: `http://localhost:5173`
- backend: `http://localhost:8082`
- Kafka UI: `http://localhost:8081`
- Kafka broker: `localhost:9092`

Backend опубликован на `8082`, потому что `8080` часто используется локальным запуском приложения.

## Локальный запуск

Сначала запустите Kafka:

```bash
docker compose up -d kafka kafka-ui
```

Запуск backend:

```bash
./gradlew :backend:bootRun
```

Backend будет доступен на `http://localhost:8080`.

Запуск frontend:

```bash
cd frontend
npm install
npm run dev
```

Frontend будет доступен на `http://localhost:5173` и будет проксировать `/api` на локальный backend.

## Проверки

Backend:

```bash
./gradlew :backend:test
```

Frontend:

```bash
cd frontend
npm run build
```
