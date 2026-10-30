#!/bin/sh
# Abre a aplicação com telas. Use ./executar.sh --console para rodar sem telas (Ctrl+C para parar)
# ou ./executar.sh --console --uma-vez para processar uma vez e sair.
cd "$(dirname "$0")" || exit 1
[ -f ArquivosDeRota.jar ] || ./compilar.sh || exit 1
exec java -jar ArquivosDeRota.jar "$@"
