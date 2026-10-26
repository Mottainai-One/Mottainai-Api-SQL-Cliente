$ErrorActionPreference = 'Stop'

$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
    throw 'Java 21 is required. Install a JDK and add java to PATH.'
}

$javaVersion = (& java -version 2>&1 | Select-Object -First 1)
if ($javaVersion -notmatch '21\.') {
    throw "Java 21 is required. Detected: $javaVersion"
}

$localMavenExecutable = Join-Path (Get-Location) '.tools\apache-maven-3.9.16\bin\mvn.cmd'
$maven = Get-Command mvn -ErrorAction SilentlyContinue
$mavenExecutable = if (Test-Path -LiteralPath $localMavenExecutable) {
    $localMavenExecutable
} elseif ($maven) {
    $maven.Source
} else {
    $null
}
if (-not $mavenExecutable) {
    $mavenCache = Join-Path $env:USERPROFILE '.m2\wrapper\dists\apache-maven-3.9.16'
    if (Test-Path -LiteralPath $mavenCache) {
        $cachedExecutable = Get-ChildItem -LiteralPath $mavenCache -Directory |
            ForEach-Object { Join-Path $_.FullName 'bin\mvn.cmd' } |
            Where-Object { Test-Path -LiteralPath $_ } |
            Select-Object -First 1
        if ($cachedExecutable) {
            $cachedHome = Split-Path -Parent (Split-Path -Parent $cachedExecutable)
            $localMavenHome = Join-Path (Get-Location) '.tools\apache-maven-3.9.16'
            New-Item -ItemType Directory -Force -Path $localMavenHome | Out-Null
            Copy-Item -Path (Join-Path $cachedHome '*') -Destination $localMavenHome -Recurse -Force
            $mavenExecutable = Join-Path $localMavenHome 'bin\mvn.cmd'
        }
    }
}
if (-not $mavenExecutable) {
    throw 'Apache Maven 3.9.16 or newer is required. Install Maven and add mvn to PATH.'
}

function Set-LocalDefault([string] $name, [string] $value) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
    }
}

function New-LocalKey {
    $bytes = [byte[]]::new(32)
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }
    return [Convert]::ToBase64String($bytes)
}

$env:DB_URL = 'jdbc:postgresql://127.0.0.1:1/mottainai'
$env:DB_USER = 'swagger-disabled'
$env:DB_PASSWORD = 'swagger-disabled'
Set-LocalDefault 'FIREBASE_PROJECT_ID' 'demo-mottainai-client'
Set-LocalDefault 'JWT_SECRET' (New-LocalKey)
Set-LocalDefault 'JWT_ISSUER' 'mottainai-client-api-local'
Set-LocalDefault 'CUSTOMER_DATA_ENCRYPTION_KEY' (New-LocalKey)
Set-LocalDefault 'STAFF_DATA_ENCRYPTION_KEY' (New-LocalKey)
Set-LocalDefault 'CUSTOMER_DATA_KEY_VERSION' '1'
$env:CUSTOMER_PASSWORD_RECOVERY_URL = ''
$env:MAIL_HOST = ''
$env:MAIL_USERNAME = ''
$env:MAIL_PASSWORD = ''
$env:MAIL_SMTP_AUTH = 'false'
$env:MAIL_SMTP_STARTTLS = 'false'

Write-Host 'Starting the API. Swagger UI will be available at http://localhost:8081/swagger-ui.html.'
Write-Host 'Generated local-only keys are held in this PowerShell process and are not saved to files.'
Write-Host 'Database access and SMTP are disabled in this launcher. It is for viewing the API documentation only.'
Write-Host 'For business-route tests, start the API manually with a verified disposable database and local test mail settings.'
& $mavenExecutable spring-boot:run
exit $LASTEXITCODE
