$ErrorActionPreference = "Stop"

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "docker command not found"
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "mvn command not found"
}

docker compose up -d postgres zookeeper kafka users-mock-api | Out-Null
Start-Sleep -Seconds 8

docker exec pipeline-kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic sales.raw 2>$null | Out-Null
Start-Sleep -Seconds 2
docker exec pipeline-kafka kafka-topics --bootstrap-server localhost:9092 --create --if-not-exists --topic sales.raw --partitions 1 --replication-factor 1 | Out-Null

$sql = @"
alter table sales_source alter column source type varchar(32);
alter table city_sales_totals alter column source type varchar(32);
alter table top_sales_per_city alter column source type varchar(32);
alter table salesman_totals alter column source type varchar(32);
alter table top_salesman_country alter column source type varchar(32);

truncate city_sales_totals, top_sales_per_city, salesman_totals, top_salesman_country;

update sales_source
set source = 'DB',
    published = false;
"@

docker exec pipeline-postgres psql -U pipeline -d pipeline -c $sql | Out-Null

Get-CimInstance Win32_Process |
    Where-Object { $_.Name -like "java*" -and ($_.CommandLine -like "*com.example.ingestion.IngestionApplication*" -or $_.CommandLine -like "*com.example.processing.ProcessingApplication*" -or $_.CommandLine -like "*com.example.api.ApiApplication*") } |
    ForEach-Object { Stop-Process -Id $_.ProcessId -Force }

Get-ChildItem "$env:TEMP\\kafka-streams\\sales-processing-app*" -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue

Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$PSScriptRoot\ingestion-service'; mvn spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$PSScriptRoot\processing-service'; mvn spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$PSScriptRoot\api-service'; mvn spring-boot:run"
