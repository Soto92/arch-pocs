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
  - Relational Database (PostgreSQL)
  - File System (CSV files)
  - Web Service (REST API)
- **Data Processing:** An Apache Flink cluster to process the data in real-time. The processing logic is written in Java.
- **Data Storage:** A PostgreSQL database to store the aggregated results.
- **API:** A Spring Boot application to expose the aggregated results through a REST API.
- **Data Lineage:** OpenLineage is used to track data lineage, with Marquez as the backend.
- **Observability:** Prometheus and Grafana are used for monitoring and observability.

## Technologies Used

- **Language:** Java 17
- **Build Tool:** Maven
- **Data Processing:** Apache Flink 1.14.4
- **Database:** PostgreSQL 13
- **API Framework:** Spring Boot 2.6.3
- **Data Lineage:** OpenLineage 0.10.0, Marquez 0.28.0
- **Observability:** Prometheus v2.33.3, Grafana 8.4.3
- **Containerization:** Docker, Docker Compose

## 4. Project Structure

The project is organized into the following directories:

- `api/`: The Spring Boot application for the REST API.
- `data/`: Sample data for the ingestion modules.
  - `db/`: SQL scripts to create and populate the database tables.
  - `files/`: CSV files for the file system ingestion.
- `docker/`: Docker-related files.
  - `docker-compose.yml`: The Docker Compose file to start all the services.
  - `prometheus.yml`: The configuration file for Prometheus.
  - `grafana.ini`: The configuration file for Grafana.
  - `dashboard.json`: The Grafana dashboard for Flink metrics.
  - `datasource.yml`: The Grafana data source configuration.
- `ingestion/`: The Flink jobs for data ingestion.
  - `ws-server/`: A simple web service to provide sample data.
- `processing/`: The Flink jobs for data processing.
- `storage/`: The Flink job for writing the aggregated results to the database.

## Ingestion Module

The ingestion module is responsible for reading data from different sources and making it available for processing.

- **Relational Database Ingestion:**
  - **File:** `ingestion/src/main/java/RelationalDBIngestion.java`
  - **Method:** This class uses the Flink JDBC connector to read data from the `sales` table in the PostgreSQL database.
- **File System Ingestion:**
  - **File:** `ingestion/src/main/java/FileSystemIngestion.java`
  - **Method:** This class uses the Flink File Source to read data from the `sales.csv` file.
- **Web Service Ingestion:**
  - **File:** `ingestion/src/main/java/WebServiceIngestion.java`
  - **Method:** This class uses a custom Flink source function to make HTTP requests to the web service at `http://localhost:8000/sales` and ingest the JSON data.
  - **Web Service:** The web service is implemented in `ingestion/ws-server/src/main/java/SalesWebService.java`.

## Processing Module

The processing module is responsible for transforming the ingested data and calculating the aggregated results.

- **Top Sales Per City:**
  - **File:** `processing/src/main/java/TopSalesPerCity.java`
  - **Method:** This class calculates the top sale for each city in a tumbling window of 10 seconds.
- **Top Salesman:**
  - **File:** `processing/src/main/java/TopSalesman.java`
  - **Method:** This class calculates the total sales for each salesman in a tumbling window of 10 seconds and then finds the top salesman in the country in another tumbling window of 10 seconds.

## Storage Module

The storage module is responsible for writing the aggregated results to the database.

- **File:** `storage/src/main/java/DatabaseSink.java`
- **Method:** This class uses the Flink JDBC sink to write the results of the processing pipelines to the `top_sales_per_city` and `top_salesman` tables in the PostgreSQL database.

## API Module

The API module is responsible for exposing the aggregated results through a REST API.

- **File:** `api/src/main/java/com/datapipeline/api/ApiController.java`
- **Endpoints:**
  - `GET /topsalespercity`: Returns the top sales per city.
  - `GET /topsalesman`: Returns the top salesman in the country.
- **Entities:**
  - `api/src/main/java/com/datapipeline/api/TopSalesPerCity.java`
  - `api/src/main/java/com/datapipeline/api/TopSalesman.java`
- **Repositories:**
  - `api/src/main/java/com/datapipeline/api/TopSalesPerCityRepository.java`
  - `api/src/main/java/com/datapipeline/api/TopSalesmanRepository.java`

## Data Lineage

Data lineage is implemented using OpenLineage and Marquez. The Flink jobs are configured to send lineage events to the Marquez backend. You can access the Marquez UI at `http://localhost:3000` to visualize the data lineage.

## Observability

Observability is implemented using Prometheus and Grafana. The Flink cluster is configured to expose metrics in a format that Prometheus can understand. Prometheus scrapes these metrics, and Grafana is used to visualize them. You can access the Grafana dashboard at `http://localhost:3001`.

## How to Run

1.  Make sure you have Docker and Maven installed and available in your `PATH`.
2.  Run the `run.sh` script:
    ```bash
    bash run.sh
    ```
3.  Access the services:
    - Grafana (observability) at http://localhost:3001
    - Marquez (data lineage) at http://localhost:3000
    - API (results) at http://localhost:8080/topsalespercity and http://localhost:8080/topsalesman
