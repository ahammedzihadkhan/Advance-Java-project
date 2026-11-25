@echo off
cd /d "%~dp0"
javac --release 11 -encoding UTF-8 -cp lib\h2-2.3.232.jar --source-path src -d out src\cinema\Main.java
if errorlevel 1 (
    echo Build failed. This project needs JDK 11 or newer.
    pause
    exit /b 1
)
java -cp "out;lib\h2-2.3.232.jar" cinema.Main
if errorlevel 1 pause
