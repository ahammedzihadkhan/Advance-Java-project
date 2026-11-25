@echo off
cd /d "%~dp0"
javac --release 11 -encoding UTF-8 -cp lib\h2-2.3.232.jar --source-path "src;test" -d out test\cinema\SelfTest.java
if errorlevel 1 exit /b 1
java -cp "out;lib\h2-2.3.232.jar" cinema.SelfTest
