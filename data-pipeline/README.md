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

## Architecture Diagram

```
                +-------------------+
                |   Relational DB   |
                |   (PostgreSQL)    |
                +---------+---------+
                          |
                          |
                          v
+--------------+    +-------------+      +------------------+
| File System  | -> | Ingestion   | ---> | Kafka (Optional) |
| CSV / JSON   |    | Services    |      | Event Bus        |
+--------------+    +-------------+      +--------+---------+
                          |                        |
                          v                        v
                   +--------------------------------------+
                   |   Processing Layer (Apache Spark)    |
                   |                                      |
                   | Pipeline 1: Top Sales per City       |
                   | Pipeline 2: Top Salesman Country     |
                   +-------------------+------------------+
                                       |
                                       v
                             +------------------+
                             | Hadoop / DataLake|
                             | (Raw / Processed)|
                             +---------+--------+
                                       |
                                       v
                                +-------------+
                                |  Redshift   |
                                | Aggregation |
                                +------+------+
                                       |
                                       v
                                 +-----------+
                                 |   API     |
                                 | SpringBoot|
                                 +-----------+

Monitoring / Observability:
Prometheus + Grafana

Lineage:
OpenLineage / Marquez
```
