param(
    [switch]$Detach
)

# Kill anything already on port 8081
$proc = Get-NetTCPConnection -LocalPort 8081 -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -First 1
if ($proc) {
    Stop-Process -Id $proc -Force
    Write-Host "Stopped old process (PID $proc) on port 8081"
}

# Set Java
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

$jar = Get-Item "backend\build\libs\*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -notlike "*-plain.jar" } | Select-Object -First 1

if (-not $jar) {
    Write-Host "Building JAR (first time only)..."
    .\gradlew.bat :backend:bootJar
    $jar = Get-Item "backend\build\libs\*.jar" | Where-Object { $_.Name -notlike "*-plain.jar" } | Select-Object -First 1
}

# If `adb` is available, set up reverse port forwarding so a physical device can reach localhost:8081
$adbCmd = Get-Command adb -ErrorAction SilentlyContinue
if ($adbCmd) {
    Write-Host "Running 'adb reverse tcp:8081 tcp:8081' to expose localhost to device..."
    try {
        & adb reverse tcp:8081 tcp:8081 2>$null
        $rev = & adb reverse --list 2>$null
        if ($rev) { Write-Host "adb reverse mappings:\n$rev" }
    } catch {
        Write-Host "adb reverse failed or device not connected; continuing anyway."
    }
} else {
    Write-Host "adb not found in PATH; skipping adb reverse."
}
Write-Host "Starting server: $($jar.Name)"
Write-Host "Server will be ready at http://localhost:8081"
if ($Detach) {
    Write-Host "Starting server detached (background)..."
    $args = "-jar", "$($jar.FullName)", "--spring.profiles.active=dev"
    Start-Process -FilePath "java" -ArgumentList $args -WindowStyle Minimized | Out-Null
    Write-Host "Server started in background (use Task Manager or Get-Process java to inspect)."
    return
} else {
    Write-Host "Press Ctrl+C to stop."
    java -jar $jar.FullName --spring.profiles.active=dev
}
