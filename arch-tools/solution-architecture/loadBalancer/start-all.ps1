$ErrorActionPreference = 'Stop'

$baseDir = Split-Path -Parent $MyInvocation.MyCommand.Path

$env:TARGETS = "http://localhost:3001,http://localhost:3002"
$env:RATE_LIMIT = "100"

function Test-PortInUse {
  param([int]$Port)
  return [bool](Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue)
}

foreach ($p in 3000,3001,3002) {
  if (Test-PortInUse $p) {
    Write-Host "Port $p is already in use. Run .\\stop-all.ps1 first."
    exit 1
  }
}

Start-Process -WorkingDirectory $baseDir -NoNewWindow -FilePath "node" -ArgumentList "backend.js 3001 server-a"
Start-Process -WorkingDirectory $baseDir -NoNewWindow -FilePath "node" -ArgumentList "backend.js 3002 server-b"
Start-Process -WorkingDirectory $baseDir -NoNewWindow -FilePath "node" -ArgumentList "load-balancer.js"

Write-Host "Started backends and load balancer."
Write-Host "Load balancer: http://localhost:3000"
Write-Host "Backends: http://localhost:3001, http://localhost:3002"
