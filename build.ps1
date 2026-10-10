# LDuels Build Script
Write-Host "Compiling LDuels..." -ForegroundColor Cyan

$buildDir = "out/build-temp"
$outputJarDir = "out/artifacts/LDuels_jar"
$outputJar = "$outputJarDir/LDuels.jar"

if (Test-Path $buildDir) { Remove-Item -Recurse -Force $buildDir }
if (!(Test-Path $outputJarDir)) { New-Item -ItemType Directory -Path $outputJarDir -Force | Out-Null }
New-Item -ItemType Directory -Path $buildDir -Force | Out-Null

# Classpath: all jars in libs
$cp = (Get-ChildItem libs/*.jar | ForEach-Object { $_.FullName }) -join ";"

# Compile java files with Java 8 compatibility (class file version 52.0)
$javaFiles = Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -source 1.8 -target 1.8 -cp "$cp" -d $buildDir $javaFiles

if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed!" -ForegroundColor Red
    exit 1
}

Write-Host "Extracting shaded dependencies (Jackson, SQLite)..." -ForegroundColor Cyan
# Extract bundled libs into buildDir
$jarTool = "C:\Program Files\Java\jdk-17\bin\jar.exe"
$bundledLibs = @(
    "libs/jackson-annotations-2.13.1.jar",
    "libs/jackson-core-2.13.1.jar",
    "libs/jackson-databind-2.13.1.jar",
    "libs/sqlite-jdbc-3.51.3.0.jar"
)

foreach ($lib in $bundledLibs) {
    & $jarTool xf (Resolve-Path $lib).Path
}
# Move extracted files to buildDir
Get-ChildItem -Exclude "src","libs","out",".idea",".git","build.ps1",".gitignore","ArenaRegen.jar","LDuels.iml","src.rar" | ForEach-Object {
    Move-Item $_.FullName $buildDir -Force
}

# Copy resources from src (plugin.yml, config.yml, lang.yml)
Copy-Item "src/plugin.yml" "$buildDir/plugin.yml" -Force
Copy-Item "src/config.yml" "$buildDir/config.yml" -Force
Copy-Item "src/lang.yml" "$buildDir/lang.yml" -Force

Write-Host "Packaging into $outputJar..." -ForegroundColor Cyan
& $jarTool cf $outputJar -C $buildDir .

Remove-Item -Recurse -Force $buildDir

$size = (Get-Item $outputJar).Length / 1MB
Write-Host ("LDuels.jar successfully built! Size: {0:N2} MB" -f $size) -ForegroundColor Green

# Copy to Desktop locations if available
$desktopPaths = @(
    "C:\Users\bugra\OneDrive\Desktop\LDuels.jar",
    "C:\Users\bugra\Desktop\LDuels.jar",
    "C:\Users\bugra\OneDrive\Desktop\liman\plugins\LDuels.jar",
    "C:\Users\bugra\Desktop\liman\plugins\LDuels.jar"
)

foreach ($target in $desktopPaths) {
    $parent = Split-Path $target -Parent
    if (Test-Path $parent) {
        Copy-Item $outputJar $target -Force
        Write-Host "Copied updated JAR to $target" -ForegroundColor Yellow
    }
}

