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

Get-CimInstance Win32_Process |
    Where-Object { $_.Name -like "java*" -and ($_.CommandLine -like "*com.example.ingestion.IngestionApplication*" -or $_.CommandLine -like "*com.example.processing.ProcessingApplication*" -or $_.CommandLine -like "*com.example.api.ApiApplication*") } |
    ForEach-Object { Stop-Process -Id $_.ProcessId -Force }

Get-ChildItem "$env:TEMP\\kafka-streams\\sales-processing-app*" -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue

Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$PSScriptRoot\ingestion-service'; mvn spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$PSScriptRoot\processing-service'; mvn spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$PSScriptRoot\api-service'; mvn spring-boot:run"
