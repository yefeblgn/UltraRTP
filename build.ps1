# ============================================================
#  UltraRTP - Derleme scripti
#  Maven kurulu değilse otomatik indirir (.tools klasörüne).
#  Kullanım:  sağ tık > "PowerShell ile çalıştır"  ya da
#             powershell -ExecutionPolicy Bypass -File build.ps1
# ============================================================

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Definition
$mavenVersion = "3.9.9"
$toolsDir = Join-Path $root ".tools"
$mavenHome = Join-Path $toolsDir "apache-maven-$mavenVersion"
$mavenBin = Join-Path $mavenHome "bin\mvn.cmd"

Write-Host ""
Write-Host "  UltraRTP derleyici" -ForegroundColor Cyan
Write-Host "  ------------------" -ForegroundColor DarkGray

# --- Java kontrolu -------------------------------------------------
$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
    Write-Host "  [x] Java bulunamadi. Java 21+ kurmalisin: https://adoptium.net" -ForegroundColor Red
    exit 1
}
Write-Host "  [+] Java bulundu" -ForegroundColor Green

# --- Maven kontrolu ------------------------------------------------
$systemMaven = Get-Command mvn -ErrorAction SilentlyContinue
if ($systemMaven) {
    $mavenBin = $systemMaven.Source
    Write-Host "  [+] Sistem Maven kullaniliyor" -ForegroundColor Green
}
elseif (Test-Path $mavenBin) {
    Write-Host "  [+] Yerel Maven bulundu" -ForegroundColor Green
}
else {
    Write-Host "  [~] Maven indiriliyor ($mavenVersion)..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
    $zipPath = Join-Path $toolsDir "maven.zip"
    $url = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
    try {
        Invoke-WebRequest -Uri $url -OutFile $zipPath -UseBasicParsing
        Expand-Archive -Path $zipPath -DestinationPath $toolsDir -Force
        Remove-Item $zipPath -Force
        Write-Host "  [+] Maven hazir" -ForegroundColor Green
    }
    catch {
        Write-Host "  [x] Maven indirilemedi: $_" -ForegroundColor Red
        Write-Host "      Elle kurup tekrar dene: https://maven.apache.org/download.cgi" -ForegroundColor DarkGray
        exit 1
    }
}

# --- Derleme -------------------------------------------------------
Write-Host "  [~] Derleniyor..." -ForegroundColor Yellow
Write-Host ""

& $mavenBin -f (Join-Path $root "pom.xml") clean package

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "  [x] Derleme basarisiz." -ForegroundColor Red
    exit $LASTEXITCODE
}

$jar = Get-ChildItem (Join-Path $root "target") -Filter "UltraRTP-*.jar" -ErrorAction SilentlyContinue |
       Where-Object { $_.Name -notlike "*original*" } | Select-Object -First 1

Write-Host ""
if ($jar) {
    Write-Host "  [+] Hazir: $($jar.FullName)" -ForegroundColor Green
    Write-Host "      Bu dosyayi sunucunun plugins klasorune at." -ForegroundColor DarkGray
}
else {
    Write-Host "  [!] Jar bulunamadi, target klasorunu kontrol et." -ForegroundColor Yellow
}
Write-Host ""
