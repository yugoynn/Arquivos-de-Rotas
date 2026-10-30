@echo off
rem Parte 1 sem telas: monitora C:\Teste a cada 10 segundos. Ctrl+C para parar.
rem Para processar so uma vez e sair: executar-console.bat --uma-vez
chcp 65001 > nul
cd /d "%~dp0"
if not exist ArquivosDeRota.jar call compilar.bat
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar ArquivosDeRota.jar --console %*
pause
