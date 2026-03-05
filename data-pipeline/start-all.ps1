$ErrorActionPreference = "Stop"

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "docker command not found"
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "mvn command not found"
}

docker compose up -d
Start-Sleep -Seconds 10

docker exec pipeline-kafka kafka-topics --bootstrap-server localhost:9092 --create --if-not-exists --topic sales.raw --partitions 1 --replication-factor 1 | Out-Null

mvn clean package -DskipTests

Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$PSScriptRoot\ingestion-service'; mvn spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$PSScriptRoot\processing-service'; mvn spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$PSScriptRoot\api-service'; mvn spring-boot:run"