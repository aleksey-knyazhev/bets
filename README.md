# Bets

Приложение для генерации бросков по двум Kafka-партициям и расчета победителя, используя многопоточность

## Асинхронность и многопоточность

Генерация бросков разделена по Kafka-партициям:
- `Партиция 0` хранит броски первого участника
- `Партиция 1` хранит броски второго участника

Backend отправляет сгенерированные броски в Kafka, а расчет результата читает данные из обеих партиций

При подсчете результата backend читает две Kafka-партиции паралтлельно. Для этого используется Java `StructuredTaskScope`:
- одна задача читает `Партиция 0`. Вторая задача читает `Партиция 1`
- основной поток ждет завершения обеих задач и передает данные в расчет победителя
- `StructuredTaskScope` дает структурированную многопоточность, дочерние задачи создаются внутри одного ограниченного scope

Инструменты:
- Kafka partitions — разделение потоков данных по партициям
- Java 25 `StructuredTaskScope` — параллельное чтение партиций
- virtual threads включены в Spring Boot через `spring.threads.virtual.enabled=true`

Стек:
- backend: Spring Boot 4, Java 25, Kafka
- frontend: React, TypeScript, Vite
- инфраструктура: Docker Compose, Kafka UI

## Запуск через Docker

```bash
docker compose up --build -d
```

После запуска:
- frontend: `http://localhost:5173`
- backend: `http://localhost:8082`
- Kafka UI: `http://localhost:8081`
- Kafka broker: `localhost:9092`
