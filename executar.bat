@echo off
chcp 65001 > nul
cd /d "%~dp0"
if not exist ArquivosDeRota.jar call compilar.bat
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar ArquivosDeRota.jar
