#!/usr/bin/env python3
"""MEJ · metricas de generacion y adaptacion con las mismas definiciones para GRA (PR-1.7/V2.1) y MEJ (PR-1.8/V2.2).

Uso: analisis_mej.py <salida_dir> ETIQUETA=dir[@ids],dir[@ids]...[+oraculo[@ids],oraculo[@ids]...] ...
  @ids (O01:O02:...) limita esa carpeta a esos perfiles (para combinar una corrida completada por partes, I-MEJ-1).
Generacion (19 perfiles factibles; O11 aparte), de generacion_ia.csv y limitaciones.jsonl:
  primera propuesta valida, propuestas, rechazadas y codigos, planes publicados desde la IA o por respaldo,
  DOSIS_LIMITADA tras la generacion y grupos por debajo.
Adaptacion (ciclos con IA), de adaptaciones.csv del oraculo v1.4 y factibilidad.csv:
  estado V32 por sesion, pertinencia, SIN_EFECTO evitable (la sonda encontro alguna modificacion valida).
"""
import csv
import json
import os
import re
import sys
from collections import Counter

csv.field_size_limit(sys.maxsize)
FACT = [f"O{i:02d}" for i in range(1, 21) if i != 11]


def leer(r):
    return list(csv.DictReader(open(r, newline="", encoding="utf-8"))) if os.path.exists(r) else []


def separa(x):
    d, _, ids = x.partition("@")
    return d, (set(ids.split(":")) if ids else None)


def generacion(dirs):
    filas = []
    for x in dirs:
        d, ids = separa(x)
        filas += [r for r in leer(os.path.join(d, "generacion_ia.csv")) if ids is None or r["id"] in ids]
    if not filas:
        return None
    por = {}
    for r in filas:
        if r["id"] not in FACT or r["intento"] == "0":
            continue
        por.setdefault(r["id"], []).append(r)
    codigos = Counter()
    primera_ok = rechazadas = propuestas = ia = respaldo = 0
    detalle = {}
    for pid in FACT:
        ints = sorted(por.get(pid, []), key=lambda r: int(r["intento"]))
        propuestas += len(ints)
        rech = [r for r in ints if r["rechazada_por_el_backend"] == "SI"]
        rechazadas += len(rech)
        for r in rech:
            codigos.update(re.findall(r"\b(V\d+):", r["motivos"]))
        ok1 = bool(ints) and ints[0]["rechazada_por_el_backend"] == "NO"
        primera_ok += ok1
        fuente = ints[-1]["generation_source"] if ints else ""
        ia += fuente == "AI"
        respaldo += fuente == "RULE_ENGINE"
        detalle[pid] = {"primera_valida": ok1, "intentos": len(ints), "fuente": fuente}
    lim = Counter()
    planes_lim = 0
    for dd in dirs:
        d, ids = separa(dd)
        ruta = os.path.join(d, "limitaciones.jsonl")
        if not os.path.exists(ruta):
            continue
        for l in open(ruta, encoding="utf-8"):
            x = json.loads(l)
            if x["momento"] != "TRAS_GENERACION" or x["id"] not in FACT or (ids is not None and x["id"] not in ids):
                continue
            for L in x["cuerpo"].get("limitations", []):
                if L["code"] == "DOSIS_LIMITADA":
                    planes_lim += 1
                    m = re.search(r"orientativa en: (.+?)\. Es un plan", L["message"])
                    if m:
                        for part in re.split(r", (?![^(]*\))", m.group(1)):
                            lim[re.sub(r" \(.*", "", part)] += 1
                    detalle.setdefault(x["id"], {})["dosis_limitada"] = True
    return {"perfiles": len(FACT), "primera_valida": primera_ok, "propuestas": propuestas, "rechazadas": rechazadas,
            "codigos_rechazo": dict(codigos), "publicados_ia": ia, "respaldo": respaldo,
            "planes_dosis_limitada": planes_lim, "grupos_bajo_objetivo": dict(lim), "detalle": detalle}


def adaptacion(dirs, oraculos):
    if not oraculos:
        return None
    filas = []
    for x in oraculos:
        o, ids = separa(x)
        filas += [r for r in leer(os.path.join(o, "adaptaciones.csv")) if int(r["llamadas_ia"] or 0) > 0
                  and (ids is None or r["id"] in ids)]
    fact = {}
    for dd in dirs:
        d, ids = separa(dd)
        for r in leer(os.path.join(d, "factibilidad.csv")):
            if ids is None or r["etiqueta"].split("/")[0] in ids:
                fact[(r["etiqueta"].split("/")[0], r["sesion"])] = int(r["validas"] or 0)
    evitables = [f"{r['id']} c{r['ciclo']}" for r in filas if r["estado_v32"] == "SIN_EFECTO"
                 and fact.get((r["id"], r["sesion"]), 0) > 0]
    return {"sesiones_con_ia": len(filas), "estado": dict(Counter(r["estado_v32"] for r in filas)),
            "pertinencia": dict(Counter(r["pertinencia"] for r in filas)), "sin_efecto_evitable": evitables,
            "por_ciclo": {f"{r['id']} c{r['ciclo']} s{r['sesion']}": f"{r['intervencion']}|{r['estado_v32']}|{r['pertinencia']}"
                          for r in filas}}


def coste(dirs):
    n = u = 0.0
    for d in {separa(x)[0] for x in dirs}:
        ruta = os.path.join(d, "99_resumen.txt")
        if os.path.exists(ruta):
            t = open(ruta).read()
            m = re.search(r"llamadas_proveedor=(\d+)", t)
            if m:
                n += int(m.group(1))
                u += float(re.search(r"usd_medido=([\d.]+)", t).group(1))
    return {"llamadas": int(n), "usd": round(u, 4)}


if __name__ == "__main__":
    salida = sys.argv[1]
    os.makedirs(salida, exist_ok=True)
    res = {}
    for arg in sys.argv[2:]:
        et, rutas = arg.split("=", 1)
        corr, _, orc = rutas.partition("+")
        dirs, orac = corr.split(","), ([o for o in orc.split(",") if o] or None)
        res[et] = {"generacion": generacion(dirs), "adaptacion": adaptacion(dirs, orac), "coste": coste(dirs)}
    json.dump(res, open(os.path.join(salida, "metricas_mej.json"), "w"), ensure_ascii=False, indent=1)
    for et, r in res.items():
        g, a = r["generacion"], r["adaptacion"]
        print(f"== {et} coste={r['coste']}")
        if g:
            print(f"   GEN primera valida {g['primera_valida']}/{g['perfiles']} · propuestas {g['propuestas']} · rechazadas "
                  f"{g['rechazadas']} {g['codigos_rechazo']} · IA {g['publicados_ia']} respaldo {g['respaldo']} · "
                  f"DOSIS_LIMITADA {g['planes_dosis_limitada']}/{g['perfiles']} {g['grupos_bajo_objetivo']}")
        if a:
            print(f"   ADA sesiones {a['sesiones_con_ia']} {a['estado']} {a['pertinencia']} evitables {a['sin_efecto_evitable']}")
