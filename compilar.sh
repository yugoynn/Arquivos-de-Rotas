#!/bin/sh
# Compila o projeto e gera ArquivosDeRota.jar
cd "$(dirname "$0")" || exit 1
rm -rf bin && mkdir bin
javac -encoding UTF-8 -d bin -sourcepath src src/app/Main.java || { echo "ERRO na compilação. O JDK está instalado? (javac -version)"; exit 1; }
jar cfe ArquivosDeRota.jar app.Main -C bin .
echo "Pronto: ArquivosDeRota.jar"
