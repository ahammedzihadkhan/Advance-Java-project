@echo off
rem Builds dist\Matinee\Matinee.exe (with its own Java runtime, so no Java install is needed)
rem and dist\Matinee-portable.zip to hand out.
cd /d "%~dp0"

set "JDK_BIN=%JAVA_HOME%\bin"
rem Without JAVA_HOME, use the newest JDK under Program Files\Java (folders sort by version).
if not exist "%JDK_BIN%\jpackage.exe" for /d %%D in ("C:\Program Files\Java\jdk-*") do if exist "%%D\bin\jpackage.exe" set "JDK_BIN=%%D\bin"
if not exist "%JDK_BIN%\jpackage.exe" (
    echo Could not find jpackage. Install JDK 17 or newer, or set JAVA_HOME.
    exit /b 1
)

if exist build rmdir /s /q build
if exist dist rmdir /s /q dist

"%JDK_BIN%\javac.exe" --release 11 -encoding UTF-8 -cp lib\h2-2.3.232.jar --source-path src -d build\classes src\cinema\Main.java || goto :fail
mkdir build\input
copy lib\h2-2.3.232.jar build\input\ >nul
echo Class-Path: h2-2.3.232.jar> build\manifest.txt
"%JDK_BIN%\jar.exe" --create --file build\input\matinee.jar --manifest build\manifest.txt --main-class cinema.Main -C build\classes . || goto :fail

rem Modules found with jdeps; keeping only these makes the bundled runtime much smaller.
"%JDK_BIN%\jpackage.exe" --type app-image --name Matinee --app-version 1.0 --vendor Matinee ^
    --description "Matinee cinema box office" ^
    --input build\input --main-jar matinee.jar --main-class cinema.Main ^
    --add-modules java.base,java.compiler,java.desktop,java.instrument,java.management,java.naming,java.scripting,java.sql,jdk.net ^
    --java-options "-Dmatinee.db=jdbc:h2:~/Matinee/cinema" ^
    --dest dist || goto :fail

rem Windows' built-in tar writes .zip with -a and, unlike Compress-Archive, returns an error code on failure.
tar -a -c -f dist\Matinee-portable.zip -C dist Matinee || goto :fail
echo.
echo Done. Run dist\Matinee\Matinee.exe, or share dist\Matinee-portable.zip.
exit /b 0

:fail
echo Packaging failed.
exit /b 1
