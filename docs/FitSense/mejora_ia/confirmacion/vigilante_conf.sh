#!/bin/bash
# Descarga cada prediccion que aparezca en los logs de las corridas CONF-IA (las salidas caducan en 1 h).
R=$1; D=$R/recuperadas/todas; mkdir -p $D
while true; do
  for f in $R/CONF_*.log; do
    [ -f "$f" ] || continue
    for id in $(grep -oE "prediction_id=[a-z0-9]{26}|prediccion [a-z0-9]{26}" "$f" | grep -oE "[a-z0-9]{26}$" | sort -u); do
      [ -s $D/p_$id.json ] && grep -qE "\"status\": *\"(succeeded|failed|canceled)\"" $D/p_$id.json && continue
      curl -sS -A fitsense-mej/1.0 https://api.replicate.com/v1/predictions/$id > $D/p_$id.json.tmp && mv $D/p_$id.json.tmp $D/p_$id.json
    done
  done
  # predicciones de la cuenta aun no vistas en los logs (proceso detenido entre el POST y el registro)
  curl -sS -A fitsense-mej/1.0 https://api.replicate.com/v1/predictions > $D/lista_$(date +%H%M).json
  sleep 240
done
