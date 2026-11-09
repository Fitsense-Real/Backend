#!/usr/bin/env python3
"""MEJ ronda 2 · metricas de generacion y adaptacion por perfil y por condicion (solo lectura).

Uso:
  analisis_mej_r2.py <salida_dir> --det <dir_determinista> GEN:ETIQUETA=dir[@ids],dir... ADA:ETIQUETA=dir,...+oraculo,...

Generacion (19 perfiles factibles; O11 aparte), con los datos que el propio backend registra:
  - generacion_ia.csv: primera propuesta valida, propuestas, rechazadas y codigos, reintentos, IA o respaldo;
  - predicciones_metricas.csv: latencia (ms) y coste por prediccion;
  - weekly_training_plans.planning_policy_trace (traza del backend, CalidadDosis): estado DOSIS_OBJETIVO/LIMITADA,
    series por grupo, grupos bajo objetivo, minutos aerobicos, clase de cada sesion (INSUFICIENTE / CORTA_RAZONABLE /
    COMPLETA), segundos estimados y % de M de cada sesion.
  - DOSIS_LIMITADA mejorable/inevitable (protocolo §4): un grupo bajo objetivo es MEJORABLE si el plan determinista del
    mismo perfil (--det) no lo tiene bajo objetivo o le da mas series; INEVITABLE si el determinista tampoco llega.
Adaptacion: oraculo v1.4 (adaptaciones.csv) + vigilancia independiente de V8C desde las tablas, por IDENTIDAD
  (no por posicion): RETIRADA_NO_AUTORIZADA (H-A1) y DESCANSO_CAMBIADO (H-A2).
Estadistica: McNemar exacta (pares por perfil) y Wilcoxon de rangos con signo (aproximacion normal con correccion).
"""
import csv
import json
import math
import os
import re
import sys
from collections import Counter

csv.field_size_limit(sys.maxsize)
FACT = [f"O{i:02d}" for i in range(1, 21) if i != 11]
RETIRADA_PERMITIDA = {"SEGURIDAD_ANTE_DOLOR"}           # retirar sin reemplazo vale (TipoIntervencion)
UNA_RETIRADA = {"ACORTAR_O_REORGANIZAR"}                # una sola colocacion de relleno, sin anadir


def leer(r):
    return list(csv.DictReader(open(r, newline="", encoding="utf-8"))) if os.path.exists(r) else []


def separa(x):
    d, _, ids = x.partition("@")
    return d, (set(ids.split(":")) if ids else None)


# ----------------------------------------------------------------------------------------------- generacion

def trazas_iniciales(d):
    """id de perfil -> (plan, traza) del PRIMER plan de cada usuario (la generacion inicial)."""
    usuarios = {r["id"]: r["user_id"] for r in leer(os.path.join(d, "usuarios.csv"))}
    planes = leer(os.path.join(d, "tablas", "weekly_training_plans.csv"))
    por_usuario = {}
    for p in planes:
        u = p["user_id"]
        if u not in por_usuario or int(p["plan_id"]) < int(por_usuario[u]["plan_id"]):
            por_usuario[u] = p
    out = {}
    for pid, u in usuarios.items():
        p = por_usuario.get(str(u))
        if p:
            try:
                traza = json.loads(p.get("planning_policy_trace") or "{}")
            except json.JSONDecodeError:
                traza = {}
            out[pid] = (p, traza)
    return out


def ejercicios_por_sesion(d):
    pwe = leer(os.path.join(d, "tablas", "planned_workout_exercises.csv"))
    por = {}
    for e in pwe:
        por.setdefault(e["planned_workout_id"], []).append(e)
    for v in por.values():
        v.sort(key=lambda e: int(e["exercise_order"]))
    return por


def gen_corrida(d, ids=None):
    filas = [r for r in leer(os.path.join(d, "generacion_ia.csv")) if ids is None or r["id"] in ids]
    met = [r for r in leer(os.path.join(d, "predicciones_metricas.csv")) if r.get("tipo") == "GENERACION"]
    trazas = trazas_iniciales(d)
    pw = leer(os.path.join(d, "tablas", "planned_workouts.csv"))
    ejs = ejercicios_por_sesion(d)
    por = {}
    for r in filas:
        if r["intento"] != "0":
            por.setdefault(r["id"], []).append(r)
    res = {}
    for pid in FACT:
        if ids is not None and pid not in ids:
            continue
        ints = sorted(por.get(pid, []), key=lambda r: int(r["intento"]))
        if not ints and pid not in trazas:
            continue
        rech = [r for r in ints if r["rechazada_por_el_backend"] == "SI"]
        codigos = Counter()
        for r in rech:
            codigos.update(re.findall(r"\b(V\d+[A-Z]?):", r["motivos"]))
        m = [x for x in met if x["etiqueta"].startswith(pid + "/")]
        plan, traza = trazas.get(pid, ({}, {}))
        dosis = traza.get("dosis", {}) if traza else {}
        ses = traza.get("sesiones", []) if traza else []
        sesiones_plan = [w for w in pw if w["plan_id"] == plan.get("plan_id")]
        coloc = [len(ejs.get(w["planned_workout_id"], [])) for w in sesiones_plan]
        res[pid] = {
            # Sin ninguna llamada a la IA (bloque detenido por el arnes antes de llegar al perfil, I-MEJ-1): no se
            # cuenta como no valido sino como NO_EJECUTADO, y se completa en otra corrida (@ids).
            "ejecutado": bool(ints),
            "primera_valida": bool(ints) and ints[0]["rechazada_por_el_backend"] == "NO",
            "propuestas": len(ints), "rechazadas": len(rech), "codigos": dict(codigos),
            "fuente": plan.get("generation_source") or (ints[-1]["generation_source"] if ints else ""),
            "latencia_ms": [int(x["ms"]) for x in m if x.get("ms")],
            "usd": round(sum(float(x["usd"] or 0) for x in m), 6),
            "estado_dosis": dosis.get("estado", ""),
            "series_por_grupo": dosis.get("series_por_grupo", {}),
            "grupos_bajo_objetivo": dosis.get("grupos_bajo_objetivo", []),
            "aerobico": (dosis.get("aerobico_trabajo_minutos"), dosis.get("aerobico_objetivo_minutos")),
            "clases_sesion": [s.get("clase") for s in dosis.get("sesiones", [])],
            "pct_de_m": [s.get("pct_de_m") for s in ses],
            "sesiones": len(sesiones_plan), "colocaciones": coloc,
        }
    return res


def grupos_bajo(x):
    out = {}
    for g in x.get("grupos_bajo_objetivo") or []:
        if isinstance(g, dict):
            out[g.get("grupo") or g.get("body_part") or json.dumps(g, sort_keys=True)] = g
        else:
            out[str(g)] = g
    return out


def clasificar_limitada(ia, det):
    """Por grupo bajo objetivo en el plan de la IA: MEJORABLE si el determinista no lo tiene bajo objetivo o le da
    mas series; INEVITABLE si tampoco llega (sin umbrales nuevos)."""
    out = {}
    for g in grupos_bajo(ia):
        bajo_det = g in grupos_bajo(det)
        s_ia = (ia.get("series_por_grupo") or {}).get(g, 0)
        s_det = (det.get("series_por_grupo") or {}).get(g, 0)
        out[g] = "MEJORABLE" if (not bajo_det or s_det > s_ia) else "INEVITABLE"
    return out


def resumen_gen(res_todos, det):
    no_ejecutados = sorted(p for p, r in res_todos.items() if not r["ejecutado"])
    res = {p: r for p, r in res_todos.items() if r["ejecutado"]}
    n = len(res)
    cod = Counter()
    for r in res.values():
        cod.update(r["codigos"])
    lat = sorted(x for r in res.values() for x in r["latencia_ms"])
    clases = Counter(c for r in res.values() for c in r["clases_sesion"])
    pct = [p for r in res.values() for p in r["pct_de_m"] if p is not None]
    lim = Counter()
    for pid, r in res.items():
        if det.get(pid):
            for g, c in clasificar_limitada(r, det[pid]).items():
                lim[c] += 1
    return {
        "perfiles": n, "no_ejecutados": no_ejecutados,
        "primera_valida": sum(r["primera_valida"] for r in res.values()),
        "propuestas": sum(r["propuestas"] for r in res.values()),
        "rechazadas": sum(r["rechazadas"] for r in res.values()), "codigos_rechazo": dict(cod),
        "V15": cod.get("V15", 0), "V26": cod.get("V26", 0), "V24": cod.get("V24", 0),
        "publicados_ia": sum(r["fuente"] == "AI" for r in res.values()),
        "respaldo": sum(r["fuente"] == "RULE_ENGINE" for r in res.values()),
        "con_plan_medible": sum(bool(r["estado_dosis"]) for r in res.values()),
        "dosis_limitada": sum(r["estado_dosis"] == "DOSIS_LIMITADA" for r in res.values()),
        "dosis_objetivo": sum(r["estado_dosis"] == "DOSIS_OBJETIVO" for r in res.values()),
        "grupos_bajo_mejorables": lim.get("MEJORABLE", 0), "grupos_bajo_inevitables": lim.get("INEVITABLE", 0),
        "sesiones_insuficientes": clases.get("INSUFICIENTE", 0), "sesiones_cortas": clases.get("CORTA_RAZONABLE", 0),
        "sesiones_completas": clases.get("COMPLETA", 0),
        "pct_de_m_medio": round(sum(pct) / len(pct), 1) if pct else None,
        "latencia_mediana_ms": lat[len(lat) // 2] if lat else None,
        "usd": round(sum(r["usd"] for r in res.values()), 4),
    }


# ----------------------------------------------------------------------------------------------- adaptacion

IDENTIDAD = {}   # (id, ciclo, sesion) -> detalle por identidad (lo rellena vigilancia)


def vigilancia(d, filas_oraculo):
    """H-A1/H-A2 por identidad desde las tablas: sesion de origen (semana medida) frente a su derivada. Deja ademas en
    IDENTIDAD el detalle por sesion (quitados, anadidos, dosis cambiadas, dificultad maxima antes/despues), que corrige
    el emparejamiento posicional del oraculo (artefacto de O04)."""
    pw = leer(os.path.join(d, "tablas", "planned_workouts.csv"))
    ejs = ejercicios_por_sesion(d)
    dif = {e["exercise_id"]: int(e["difficulty_level"] or 0)
           for e in leer(os.path.join(d, "tablas", "exercises.csv"))}
    hallazgos = []
    for r in filas_oraculo:
        if r.get("estado_v32") != "APLICADA":
            continue
        origen = r["sesion"]
        derivadas = sorted((w for w in pw if w.get("source_planned_workout_id") == origen),
                           key=lambda w: int(w["planned_workout_id"]))
        if not derivadas:
            continue
        antes = ejs.get(origen, [])
        despues = ejs.get(derivadas[0]["planned_workout_id"], [])
        ca, cd = Counter(e["exercise_id"] for e in antes), Counter(e["exercise_id"] for e in despues)
        quitados = list((ca - cd).elements())
        anadidos = list((cd - ca).elements())
        iv = r["intervencion"]
        dosis_a = {e["exercise_id"]: (e["planned_sets"], e["planned_reps"], e["planned_duration_seconds"]) for e in antes}
        cambiadas = [e["exercise_id"] for e in despues
                     if e["exercise_id"] in dosis_a and dosis_a[e["exercise_id"]] != (e["planned_sets"], e["planned_reps"],
                                                                                    e["planned_duration_seconds"])]
        max_q = max((dif.get(x, 0) for x in quitados), default=None)
        max_a = max((dif.get(x, 0) for x in anadidos), default=None)
        IDENTIDAD[(r["id"], r["ciclo"], origen)] = {
            "quitados": quitados, "anadidos": anadidos, "dosis_cambiadas": cambiadas,
            "se_quedan": sorted(set(ca) & set(cd)),
            "dif_max_retirados": max_q, "dif_max_anadidos": max_a,
            "mas_dificil_identidad": max_a is not None and max_q is not None and max_a > max_q}
        if iv in UNA_RETIRADA:
            if len(quitados) > 1 or anadidos:
                hallazgos.append((r["id"], r["ciclo"], origen, iv, "RETIRADA_NO_AUTORIZADA",
                                  f"retira {quitados}, anade {anadidos}"))
        elif iv not in RETIRADA_PERMITIDA and len(quitados) > len(anadidos):
            hallazgos.append((r["id"], r["ciclo"], origen, iv, "RETIRADA_NO_AUTORIZADA",
                              f"retira {quitados}, anade {anadidos}"))
        descanso_a = {e["exercise_id"]: e["rest_seconds"] for e in antes}
        for e in despues:
            if e["exercise_id"] in descanso_a and e["rest_seconds"] != descanso_a[e["exercise_id"]]:
                hallazgos.append((r["id"], r["ciclo"], origen, iv, "DESCANSO_CAMBIADO",
                                  f"{e['exercise_id']}: {descanso_a[e['exercise_id']]} -> {e['rest_seconds']} s"))
    return hallazgos


def ada_corrida(dirs, oraculos):
    IDENTIDAD.clear()   # por condicion: las claves (id, ciclo, sesion) se repiten entre condiciones
    filas, fact = [], {}
    for x in oraculos:
        o, ids = separa(x)
        filas += [r for r in leer(os.path.join(o, "adaptaciones.csv")) if int(r["llamadas_ia"] or 0) > 0
                  and (ids is None or r["id"] in ids)]
    vig = []
    for x in dirs:
        d, ids = separa(x)
        if ids is None:  # cada carpeta solo con SUS perfiles: los ids de sesion se repiten entre bases (G y P)
            ids = {r["id"] for r in leer(os.path.join(d, "usuarios.csv"))}
        for r in leer(os.path.join(d, "factibilidad.csv")):
            if ids is None or r["etiqueta"].split("/")[0] in ids:
                fact[(r["etiqueta"].split("/")[0], r["sesion"])] = int(r["validas"] or 0)
        sub = [r for r in filas if ids is None or r["id"] in ids]
        vig += vigilancia(d, sub)
    evitables = [f"{r['id']} c{r['ciclo']}" for r in filas if r["estado_v32"] == "SIN_EFECTO"
                 and fact.get((r["id"], r["sesion"]), 0) > 0]
    return {"sesiones_con_ia": len(filas), "estado": dict(Counter(r["estado_v32"] for r in filas)),
            "pertinencia": dict(Counter(r["pertinencia"] for r in filas)), "sin_efecto_evitable": evitables,
            "no_autorizados": [list(h) for h in vig],
            "por_sesion": {f"{r['id']} c{r['ciclo']} s{r['sesion']}": {
                "intervencion": r["intervencion"], "estado": r["estado_v32"], "pertinencia": r["pertinencia"],
                "fuente": r["fuente"], "sustituciones": r["sustituciones"], "reducciones": r["reducciones"],
                "retirados": r["retirados"], "trabajo": f"{r['trabajo_antes_s']}->{r['trabajo_despues_s']}",
                "problemas": r["problemas"], "identidad": IDENTIDAD.get((r["id"], r["ciclo"], r["sesion"]))}
                for r in filas}}


# ----------------------------------------------------------------------------------------------- estadistica

def mcnemar(b, c):
    """Exacta bilateral: b = solo la variante, c = solo el control."""
    n = b + c
    if n == 0:
        return 1.0
    k = min(b, c)
    p = sum(math.comb(n, i) for i in range(k + 1)) / 2 ** n
    return min(1.0, 2 * p)


def wilcoxon(dif):
    d = [x for x in dif if x != 0]
    n = len(d)
    if n == 0:
        return {"n": 0, "W+": 0, "p": 1.0}
    orden = sorted(range(n), key=lambda i: abs(d[i]))
    rangos = [0.0] * n
    i = 0
    while i < n:
        j = i
        while j + 1 < n and abs(d[orden[j + 1]]) == abs(d[orden[i]]):
            j += 1
        for k in range(i, j + 1):
            rangos[orden[k]] = (i + j) / 2 + 1
        i = j + 1
    wp = sum(r for r, x in zip(rangos, d) if x > 0)
    mu, sd = n * (n + 1) / 4, math.sqrt(n * (n + 1) * (2 * n + 1) / 24)
    z = (abs(wp - mu) - 0.5) / sd if sd else 0
    p = math.erfc(z / math.sqrt(2))
    return {"n": n, "W+": wp, "p": round(min(1.0, p), 4)}


def pareado(var, ctl):
    comunes = [p for p in FACT if p in var and p in ctl and var[p]["ejecutado"] and ctl[p]["ejecutado"]]
    b = sum(var[p]["primera_valida"] and not ctl[p]["primera_valida"] for p in comunes)
    c = sum(ctl[p]["primera_valida"] and not var[p]["primera_valida"] for p in comunes)
    rech = [var[p]["rechazadas"] - ctl[p]["rechazadas"] for p in comunes]
    pct = []
    for p in comunes:
        a, k = var[p]["pct_de_m"], ctl[p]["pct_de_m"]
        if a and k and None not in a and None not in k:
            pct.append(sum(a) / len(a) - sum(k) / len(k))
    return {"perfiles": len(comunes), "solo_variante": b, "solo_control": c, "mcnemar_p": round(mcnemar(b, c), 4),
            "rechazadas_dif": wilcoxon(rech), "pct_de_m_dif": wilcoxon(pct)}


# ----------------------------------------------------------------------------------------------- main

if __name__ == "__main__":
    salida = sys.argv[1]
    os.makedirs(salida, exist_ok=True)
    args = sys.argv[2:]
    det = {}
    if args and args[0] == "--det":
        det = gen_corrida(args[1])
        args = args[2:]
    # --rec ETIQUETA=baseline_validacion.csv: respuestas del proveedor RECUPERADAS (prediccion terminada que el backend
    # no pudo leer por un 502, I-R2-6) y validadas en solo lectura con el validador real (etapa NORMALIZADO). Cuentan
    # como primera propuesta; el plan publicado fue el respaldo por un fallo del proveedor, asi que sus metricas de
    # dosis/tiempo no se usan (no son de la IA).
    recuperadas = {}
    while args and args[0] == "--rec":
        et, ruta = args[1].split("=", 1)
        recuperadas.setdefault(et, {})
        for r in leer(ruta):
            if r["etapa"] == "NORMALIZADO":
                recuperadas[et][r["id"]] = r
        args = args[2:]
    res = {"_determinista": {"perfiles": {k: v for k, v in det.items()}}}
    for a in args:
        tipo, _, resto = a.partition(":")
        et, rutas = resto.split("=", 1)
        corr, _, orc = rutas.partition("+")
        dirs = corr.split(",")
        if tipo == "GEN":
            perf = {}
            for x in dirs:
                d, ids = separa(x)
                perf.update(gen_corrida(d, ids))
            for pid, r in recuperadas.get(et, {}).items():
                valida = r["resultado"] == "VALIDO"
                perf[pid] = {"ejecutado": True, "primera_valida": valida, "propuestas": 1,
                             "rechazadas": 0 if valida else 1,
                             "codigos": dict(Counter(re.findall(r"\b(V\d+[A-Z]?):", r["problemas"]))),
                             "fuente": "RECUPERADA_502", "latencia_ms": [], "usd": 0.0, "estado_dosis": "",
                             "series_por_grupo": {}, "grupos_bajo_objetivo": [], "aerobico": (None, None),
                             "clases_sesion": [], "pct_de_m": [], "sesiones": 0, "colocaciones": [],
                             "nota": f"respuesta recuperada ({r['archivo']}); plan publicado = respaldo por 502"}
            res[et] ={"tipo": "GEN", "resumen": resumen_gen(perf, det), "perfiles": perf}
        else:
            res[et] = {"tipo": "ADA", "resumen": ada_corrida(dirs, [o for o in orc.split(",") if o])}
    json.dump(res, open(os.path.join(salida, "metricas_mej_r2.json"), "w"), ensure_ascii=False, indent=1)
    for et, r in res.items():
        if et.startswith("_"):
            continue
        print(f"== {et}: {json.dumps(r['resumen'] if r['tipo'] == 'GEN' else {k: v for k, v in r['resumen'].items() if k != 'por_sesion'}, ensure_ascii=False)}")
