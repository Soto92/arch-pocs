$ErrorActionPreference = 'Stop'

function Stop-PortProcess {
  param([int]$Port)
  $conns = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
  foreach ($c in $conns) {
    if ($c.OwningProcess -and $c.OwningProcess -ne 0) {
      try {
        Stop-Process -Id $c.OwningProcess -Force -ErrorAction Stop
        Write-Host "Stopped process $($c.OwningProcess) on port $Port"
      } catch {
        Write-Host "Failed to stop process $($c.OwningProcess) on port ${Port}: $($_.Exception.Message)"
      }
    }
  }
}

Stop-PortProcess 3000
Stop-PortProcess 3001
Stop-PortProcess 3002
