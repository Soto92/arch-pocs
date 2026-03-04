# Data Pipeline Project

This project implements a modern data pipeline to process data from multiple sources and provide aggregated results through an API.

## Challenge

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

## Architecture

The pipeline is designed with the following components:

- **Data Ingestion:** A set of connectors to ingest data from different sources:
  - Relational Database (e.g., PostgreSQL, MySQL)
  - File System (e.g., CSV, JSON files)
  - Web Service (e.g., SOAP, REST)
- **Data Processing:** An Apache Flink cluster to process the data in real-time. The processing logic will be written in Java.
- **Data Storage:** A PostgreSQL database to store the aggregated results.
- **API:** A Spring Boot application to expose the aggregated results through a REST API.
- **Data Lineage:** OpenLineage will be used to track data lineage.
- **Observability:** Prometheus and Grafana will be used for monitoring and observability.

## How to run

1.  Make sure you have Docker and Maven installed and available in your `PATH`.
2.  Run the `run.sh` script:
    ```bash
    bash run.sh
    ```
3.  Access the services:
    - Grafana (observability) at http://localhost:3001
    - Marquez (data lineage) at http://localhost:3000
    - API (results) at http://localhost:8080/topsalespercity and http://localhost:8080/topsalesman
