@echo off
chcp 65001 > nul
cd /d "%~dp0"
echo Compilando...
if exist bin rmdir /s /q bin
mkdir bin
javac -encoding UTF-8 -d bin -sourcepath src src\app\Main.java
if errorlevel 1 (
    echo.
    echo ERRO na compilacao. Verifique se o JDK esta instalado e no PATH.
    pause
    exit /b 1
)
jar cfe ArquivosDeRota.jar app.Main -C bin .
echo Pronto: ArquivosDeRota.jar
pause
