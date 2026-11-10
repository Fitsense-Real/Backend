"""CONF-IA. Quita de las medidas de dosis y tiempo los planes de RESPALDO publicados por un FALLO DEL PROVEEDOR
(error de E/S, timeout o prediccion fallida): ese plan es del motor de reglas, no de la IA.

Es la misma regla que analisis_mej_r2.py ya aplica a las respuestas recuperadas (--rec: «sus metricas de
dosis/tiempo no se usan (no son de la IA)»), extendida al caso en que el primer intento fue rechazado y el
fallo llego en un reintento. Se aplica igual a todas las condiciones. No cambia la validez a la primera, los
rechazos ni los codigos; los respaldos por rechazos agotados (>= 3 propuestas y >= 3 rechazos) se dejan
como estan, porque ese plan es el que recibe el usuario por decision del backend.

Uso: depurar_respaldos_proveedor.py <metricas_entrada.json> <metricas_salida.json>
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from analisis_mej_r2 import resumen_gen  # noqa: E402

VACIAR = {"estado_dosis": "", "series_por_grupo": {}, "grupos_bajo_objetivo": [], "aerobico": (None, None),
          "clases_sesion": [], "pct_de_m": [], "sesiones": 0, "colocaciones": []}

m = json.load(open(sys.argv[1]))
det = m.get("_determinista", {}).get("perfiles", {})
for et, r in m.items():
    if et.startswith("_") or r.get("tipo") != "GEN":
        continue
    for pid, p in r["perfiles"].items():
        por_rechazos = p.get("propuestas", 0) >= 3 and p.get("rechazadas", 0) >= 3
        if p.get("fuente") == "RULE_ENGINE" and not por_rechazos:
            p.update(VACIAR)
            p["nota"] = "respaldo por fallo del proveedor: dosis y tiempo excluidos (no son de la IA)"
            print(f"{et} {pid}: dosis/tiempo excluidos (respaldo por fallo del proveedor)")
    r["resumen"] = resumen_gen(r["perfiles"], det)
json.dump(m, open(sys.argv[2], "w"), ensure_ascii=False, indent=1)
