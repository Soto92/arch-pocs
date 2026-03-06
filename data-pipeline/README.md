# Data Pipeline

Monorepo with Java 17 + Maven 3.9.12 for a multi-source ingestion pipeline, Kafka Streams processing, PostgreSQL aggregated storage, and API delivery.

## From Challenge

create a modern data pipeline with:

- ingestion for 3 different data sources (Relational DB, File system and traditional WS-\*)
- modern processing with spark, Flink or Kafka Streams.
- Data Lineage
- Observability
- pipeline must have at leat 2 pipelines:
  - Top sales per city
  - top salesman in the whole country
- The final Aggregated results must be in a dedicated DB and API
- restrictions: Python, Red-shift, Hadoop.

## Stack

- Java 17
- Maven 3.9.12
- Spring Boot 3.3.x
- PostgreSQL
- Kafka + Kafka Streams
- Apache CXF
- OpenLineage/Marquez
- Prometheus + Grafana

## Modules

- `ingestion-service`: reads relational data and files, publishes normalized events to Kafka topic `sales.raw`.
- `processing-service`: consumes events, computes ranking aggregates, writes tables in PostgreSQL.
- `api-service`: exposes HTTP endpoints for final rankings.

## Endpoints

- `GET /api/top-sales-per-city`
- `GET /api/top-salesman-country`

### Complete endpoints:

- http://localhost:8080/api/top-sales-per-city
- http://localhost:8080/api/top-salesman-country

Health check:

- http://localhost:8080/actuator/health
- http://localhost:8081/actuator/health
- http://localhost:8082/actuator/health

Marquez API/UI:

- http://localhost:5000/api/v1/namespaces

## Infra

`docker-compose.yml` starts:

- PostgreSQL (`localhost:5432`)
- Zookeeper (`localhost:2181`)
- Kafka (`localhost:9092`)
- Marquez API/UI (`localhost:5000`)
- Prometheus (`localhost:9090`)
- Grafana (`localhost:3000`)
- Mock Sales API (`localhost:8088/sales`)

## Run

1. Start infra:
   ```bash
   docker compose up -d
   ```
2. Build all modules:
   ```bash
   mvn clean package
   ```
3. Start services in separate terminals:
   ```bash
   mvn -pl ingestion-service spring-boot:run
   mvn -pl processing-service spring-boot:run
   mvn -pl api-service spring-boot:run
   ```

## Run With One Command

```powershell
.\start-all.ps1
```

## Reprocess Pipeline

```powershell
.\reprocess-all.ps1
```

## Data Model

Init script: `infra/postgres/init.sql`

Input source table:

- `sales_source(sale_id, city, salesman, amount, event_time, published)`

Aggregation tables:

- `city_sales_totals`
- `top_sales_per_city`
- `salesman_totals`
- `top_salesman_country`

## Source Customization

- Relational source: edit rows in `sales_source` table.
- File source: add/edit files in `data/inbox`.
- API source: edit `data/mock-api/__files/sales-response.json`.

Unified sales schema for all sources:

- `saleId`
- `city`
- `salesman`
- `amount`
- `eventTime`
- `source`

Source flag values:

- `DB` for relational database sales
- `FS` for file system sales
- `WS` for API sales

Final endpoint payloads also include `source`.

## Observability

### Graphana

Each service exposes:

- `/actuator/health`
- `/actuator/prometheus`
