Write-Host "Building the projects..."

mvn -f ingestion/pom.xml clean package
mvn -f processing/pom.xml clean package
mvn -f storage/pom.xml clean package
mvn -f api/pom.xml clean package

Write-Host "Build complete."

Write-Host "Starting the docker containers..."
docker compose -f docker/docker-compose.yml up -d
Write-Host "Docker containers started."

Write-Host "Waiting for the Flink cluster to be ready..."
Start-Sleep -Seconds 10

Write-Host "Submitting the Flink jobs..."

$flink_jobmanager_container = docker ps --filter "name=jobmanager" --format "{{.ID}}"

docker exec -t $flink_jobmanager_container flink run -c RelationalDBIngestion /opt/flink/lib/ingestion.jar
docker exec -t $flink_jobmanager_container flink run -c FileSystemIngestion /opt/flink/lib/ingestion.jar
docker exec -t $flink_jobmanager_container flink run -c WebServiceIngestion /opt/flink/lib/ingestion.jar
docker exec -t $flink_jobmanager_container flink run -c TopSalesPerCity /opt/flink/lib/processing.jar
docker exec -t $flink_jobmanager_container flink run -c TopSalesman /opt/flink/lib/processing.jar
docker exec -t $flink_jobmanager_container flink run -c DatabaseSink /opt/flink/lib/storage.jar

Write-Host "Flink jobs submitted."

Write-Host "The data pipeline is up and running."
Write-Host "Grafana: http://localhost:3001"
Write-Host "Marquez: http://localhost:3000"
Write-Host "API: http://localhost:8080/topsalespercity"
Write-Host "API: http://localhost:8080/topsalesman"