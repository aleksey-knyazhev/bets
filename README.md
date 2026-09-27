# Bets

Приложение для генерации бросков по двум Kafka-партициям и расчета победителя.

## Асинхронность и многопоточность

Генерация бросков разделена по Kafka-партициям:
- `Партиция 0` хранит броски первого участника;
- `Партиция 1` хранит броски второго участника.

Backend отправляет сгенерированные броски в Kafka, а расчет результата читает данные из обеих партиций.
Kafka используется как асинхронный брокер сообщений между этапом генерации и этапом расчета.

При подсчете результата backend читает две Kafka-партиции параллельно. Для этого используется Java `StructuredTaskScope`:
- одна задача читает `Партиция 0`;
- вторая задача читает `Партиция 1`;
- основной поток ждет завершения обеих задач и передает данные в расчет победителя.

Такой подход дает структурированную многопоточность: параллельные задачи создаются внутри одного ограниченного scope, а ошибки и прерывания обрабатываются централизованно.

Инструменты:
- Kafka partitions — разделение потоков данных по партициям;
- Kafka Consumer API — чтение сообщений из конкретных партиций;
- Java 25 `StructuredTaskScope` — параллельное чтение партиций;
- virtual threads включены в Spring Boot через `spring.threads.virtual.enabled=true`.

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
