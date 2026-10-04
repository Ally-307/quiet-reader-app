$ErrorActionPreference = "Stop"

$androidRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $androidRoot
$sdkRoot = "C:\Users\lenovo\Desktop\workspace\project_009_cocktail-recipe\data\android-toolchain\sdk"
$gradleExecutable = "C:\Users\lenovo\Desktop\workspace\project_009_cocktail-recipe\data\android-toolchain\gradle\gradle-9.3.1\bin\gradle.bat"
$jdkRoot = "C:\Program Files\ojdkbuild\java-17-openjdk-17.0.3.0.6-1"
$builtApk = Join-Path $androidRoot "app\build\outputs\apk\debug\app-debug.apk"
$outputDir = Join-Path $projectRoot "output"
$outputApk = Join-Path $outputDir "quiet-reader-v0.1.0-debug.apk"

foreach ($required in @(
    (Join-Path $jdkRoot "bin\java.exe"),
    (Join-Path $sdkRoot "platforms\android-35\android.jar"),
    (Join-Path $sdkRoot "build-tools\35.0.0\apksigner.bat"),
    $gradleExecutable
)) {
    if (-not (Test-Path -LiteralPath $required)) {
        throw "构建依赖不存在：$required"
    }
}

$env:JAVA_HOME = $jdkRoot
$env:ANDROID_SDK_ROOT = $sdkRoot
$env:Path = "$jdkRoot\bin;$sdkRoot\platform-tools;$env:Path"

Push-Location $androidRoot
try {
    & $gradleExecutable --no-daemon assembleDebug --console=plain
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle 构建失败，退出码：$LASTEXITCODE"
    }
}
finally {
    Pop-Location
}

if (-not (Test-Path -LiteralPath $builtApk)) {
    throw "未找到构建产物：$builtApk"
}

New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
Copy-Item -LiteralPath $builtApk -Destination $outputApk -Force
& (Join-Path $sdkRoot "build-tools\35.0.0\apksigner.bat") verify --verbose $outputApk
& (Join-Path $sdkRoot "build-tools\35.0.0\zipalign.exe") -c 4 $outputApk

$hash = (Get-FileHash -LiteralPath $outputApk -Algorithm SHA256).Hash
Write-Host "[完成] APK：$outputApk"
Write-Host "[完成] SHA-256：$hash"
