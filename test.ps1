$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$testDir = Join-Path $PSScriptRoot 'work\core-tests'
New-Item -ItemType Directory -Force -Path $testDir | Out-Null
$jsonJar = Join-Path $testDir 'json-20240303.jar'
if (-not (Test-Path $jsonJar)) {
    Invoke-WebRequest 'https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar' -OutFile $jsonJar
}
$source = 'app\src\main\java\com\eyedrop\manager'
& javac -encoding UTF-8 -cp $jsonJar -d $testDir "$source\Schedule.java" "$source\Constraints.java" "$source\Store.java" 'tests\CoreTest.java' 'tests\ConstraintsTest.java'
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }
& java -cp "$testDir;$jsonJar" CoreTest
if ($LASTEXITCODE -ne 0) { throw 'CoreTest failed.' }
& java -cp "$testDir;$jsonJar" ConstraintsTest
if ($LASTEXITCODE -ne 0) { throw 'ConstraintsTest failed.' }
