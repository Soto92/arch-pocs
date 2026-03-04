#!/bin/bash

# Build the projects
echo "Building the projects..."
mvn -f ingestion/pom.xml clean package
mvn -f processing/pom.xml clean package
mvn -f storage/pom.xml clean package
mvn -f api/pom.xml clean package
echo "Build complete."

# Start the docker containers
echo "Starting the docker containers..."
docker-compose -f docker/docker-compose.yml up -d
echo "Docker containers started."

# Wait for the Flink cluster to be ready
echo "Waiting for the Flink cluster to be ready..."
sleep 10

# Submit the Flink jobs
echo "Submitting the Flink jobs..."
flink_jobmanager_container=$(docker ps --filter "name=jobmanager" --format "{{.ID}}")
docker exec -t $flink_jobmanager_container flink run -c RelationalDBIngestion /opt/flink/lib/ingestion.jar
docker exec -t $flink_jobmanager_container flink run -c FileSystemIngestion /opt/flink/lib/ingestion.jar
docker exec -t $flink_jobmanager_container flink run -c WebServiceIngestion /opt/flink/lib/ingestion.jar
docker exec -t $flink_jobmanager_container flink run -c TopSalesPerCity /opt/flink/lib/processing.jar
docker exec -t $flink_jobmanager_container flink run -c TopSalesman /opt/flink/lib/processing.jar
docker exec -t $flink_jobmanager_container flink run -c DatabaseSink /opt/flink/lib/storage.jar
echo "Flink jobs submitted."

echo "The data pipeline is up and running."
echo "You can access:"
echo "- Grafana (observability) at http://localhost:3001"
echo "- Marquez (data lineage) at http://localhost:3000"
echo "- API (results) at http://localhost:8080/topsalespercity and http://localhost:8080/topsalesman"
