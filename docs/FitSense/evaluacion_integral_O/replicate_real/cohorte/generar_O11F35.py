#!/usr/bin/env python3
"""Escenario COMPLEMENTARIO O11-F35 (orden del 06/11/2026, Replicate real).

Copia exacta de la fila O11 de la cohorte congelada (cohorte/O_PERFILES.csv, SHA-256 9b996825...) con SOLO estos cambios:
  - id                 O11 -> O11F35  (usuario distinto; no sustituye a O11)
  - minutos_sesion     30  -> 35      (el unico cambio del perfil)
  - patron/escenario   COMPLEMENTARIO_F35
  - semana1..semana4   "H" (O11 no tiene guion porque se esperaba sin plan; con 1 sesion semanal se usa cumplimiento
                       total, el guion neutro: no provoca ninguna adaptacion)
  - esperado           texto del resultado esperado
La cohorte original no se modifica. Uso: python3 -I generar_O11F35.py <O_PERFILES.csv> <salida.csv>
"""
import csv
import sys

src, out = sys.argv[1], sys.argv[2]
with open(src, newline="", encoding="utf-8") as f:
    r = csv.DictReader(f)
    cab = r.fieldnames
    o11 = next(x for x in r if x["id"] == "O11")
assert o11["minutos_sesion"] == "30" and o11["dias_por_semana"] == "1" and o11["goal_type"] == "INCREASE_STRENGTH"
f35 = dict(o11)
f35.update({"id": "O11F35", "minutos_sesion": "35", "patron": "COMPLEMENTARIO_F35", "escenario": "COMPLEMENTARIO_F35",
            "semana1": "H", "semana2": "H", "semana3": "H", "semana4": "H",
            "esperado": "COMPLEMENTARIO de O11 con 35 min (O11 necesita ~32): plan valido si es factible bajo todas las "
                        "restricciones (V24, V25 y demas sin cambios); no sustituye a O11"})
with open(out, "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=cab, lineterminator="\n")
    w.writeheader()
    w.writerow(f35)
cambios = [k for k in cab if o11[k] != f35[k]]
print("columnas cambiadas:", cambios)
