#!/usr/bin/env bash
# H2-v2: predict_time de Replicate por id de prediccion (GET /v1/predictions/{id}: SIN coste, no crea predicciones).
# Uso: ./obtener_predict_time.sh <fichero con ids, uno por linea> > predict_time.csv
# El token no va en este script: lo pone el proxy del entorno (o REPLICATE_API_TOKEN en local).
set -euo pipefail
echo "prediction_id;predict_time_s;total_time_s;status;created_at"
while read -r id; do
  [ -z "$id" ] || [ "$id" = "desconocido" ] && continue
  curl -s --max-time 30 "https://api.replicate.com/v1/predictions/$id" \
       -H "Authorization: Bearer ${REPLICATE_API_TOKEN:-proxy}" \
    | python3 -I -c 'import sys,json; d=json.load(sys.stdin); m=d.get("metrics") or {}; print(";".join(str(x) for x in [d.get("id"), m.get("predict_time",""), m.get("total_time",""), d.get("status"), d.get("created_at")]))'
done < "$1"
