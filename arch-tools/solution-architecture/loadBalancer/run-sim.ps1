param(
  [string]$Target = "http://localhost:3000",
  [int]$Rate = 250,
  [int]$Duration = 5,
  [int]$Concurrency = 50
)

$ErrorActionPreference = 'Stop'

$baseDir = Split-Path -Parent $MyInvocation.MyCommand.Path

$env:TARGET = $Target
$env:RATE = "$Rate"
$env:DURATION = "$Duration"
$env:CONCURRENCY = "$Concurrency"

node "$baseDir\simulate.js"
