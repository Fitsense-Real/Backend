#!/bin/bash
# Cada 120 s: guarda la lista de la cuenta y descarga las predicciones succeeded que aun no estan en todas/ (las salidas caducan en 1 h)
T=/home/user/conf_runs/recuperadas/todas
while true; do
  L=$T/lista_$(date +%H%M).json; curl -sS -A fitsense-mej/1.0 https://api.replicate.com/v1/predictions > $L
  [ -n "$L" ] && python3 -I -B - "$L" "$T" <<'P' | while read id; do curl -sS -A fitsense-mej/1.0 https://api.replicate.com/v1/predictions/$id -o $T/p_$id.json && echo "$(date -u +%T) descargada $id"; done
import json,sys,os
d=json.load(open(sys.argv[1]))
for p in d.get('results',[]):
    if p.get('status')=='succeeded' and (p.get('created_at') or '')>='2026-11-10T00:50' and not os.path.exists(f"{sys.argv[2]}/p_{p['id']}.json"): print(p['id'])
P
  sleep 120
done
