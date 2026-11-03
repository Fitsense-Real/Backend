"""Analisis del experimento C1/C2 (solo lectura de la evidencia). Genera 08-13 y analisis_resumen.json.

Uso: python3 -I analisis.py <carpeta experimento_prompt_v2>
Duracion: replica exacta de SessionDurationEstimator con MVP-1.2 (3 s/rep, 60 s transicion, 5 min, descanso
(series-1) x rest). Minimo V20: ceil(min x 60 x 70 / 100).
"""
import csv, json, os, re, sys

BASE = sys.argv[1]
EV = os.path.join(BASE, "evidencia")
CORRIDAS = [("C1_r1", "C1"), ("C2_r1", "C2"), ("C2_r2", "C2"), ("C1_r2", "C1")]
ELEGIBLES = ["U03", "U04", "U08", "U09", "U13", "U14", "U18", "U19"]
PRECIO_IN, PRECIO_OUT = 0.25e-6, 2.00e-6


def leer(p):
    return list(csv.DictReader(open(p, encoding="utf-8")))


def sesion(firma):
    """'2026-11-06 FULL_BODY 43min [id:TIPO:3x10:r90, ...]' -> (fecha, [(id, tipo, series, valor, rest)])."""
    fecha = firma.split()[0]
    ej = []
    for id_, tipo, dosis, rest in re.findall(r"(\d+):(\w+):([^:]+):r(\d+|null)", firma):
        s, v = dosis.split("x")
        ej.append((int(id_), tipo, int(s) if s != "null" else None, v, int(rest) if rest != "null" else None))
    return fecha, ej


def segundos(ej):
    if not ej:
        return 0
    t = 300 + 60 * (len(ej) - 1)
    for _, tipo, s, v, r in ej:
        s1 = s or 1
        if tipo == "DURATION":
            t += s1 * int(v.rstrip("s"))
        else:
            t += s1 * int(v) * 3
        t += max(0, s1 - 1) * (r if r is not None else 60)
    return t


def catalogo(carpeta, u):
    p = os.path.join(carpeta, "prompts", f"{u}_adaptacion_i1.txt")
    if not os.path.exists(p):
        return {}
    t = open(p, encoding="utf-8").read()
    snap = json.loads(t[t.find("Datos de entrada:") + 17:].strip().split("\n")[0])
    return {e["exercise_id"]: e for e in snap["available_exercises"]}


def nombres(carpeta):
    out = {}
    for r in leer(os.path.join(carpeta, "plan_inicial.csv")):
        out[int(r["exercise_id"])] = r["ejercicio"]
    return out


filas, rech, aplic, cambios = {"C1": [], "C2": []}, [], [], []
seguridad = {}
for corrida, cond in CORRIDAS:
    c = os.path.join(EV, corrida)
    res = {r["id"]: r for r in leer(os.path.join(c, "resultados_20.csv"))}
    dec = {r["id"]: r for r in leer(os.path.join(c, "decisiones.csv"))}
    sig = leer(os.path.join(c, "plan_siguiente.csv"))
    uid = {r["user_id"]: r["id"] for r in res.values()}
    log = open(os.path.join(c, "ejecucion_maven.log"), encoding="utf-8", errors="replace").read()
    tim = {}
    for m in re.finditer(r"REPLICATE_TIMING user=(\d+) week=\d+ attempt=(\d+) prediction_id=(\w+) .*?total_ms=(\d+) resultado=(\w+)", log):
        tim[(uid[m.group(1)], int(m.group(2)))] = (m.group(3), int(m.group(4)), m.group(5))
    motivos = {}
    for m in re.finditer(r"Intento (\d) para el usuario (\d+): la semana propuesta no pasa \S* \[(.*)\]\n", log):
        motivos[(uid[m.group(2)], int(m.group(1)))] = m.group(3)
    idx = json.load(open(os.path.join(c, "predicciones_replicate", "indice.json")))["predicciones"]
    tok = {p["id"]: p for p in idx}
    nom = nombres(c)
    sec = dict(planes_invalidos=0, cambios_no_senaladas=0, dosis_no_autorizada=0, bajo_minimo_v20=0,
               reincorporaciones=0, excluidos=0, violaciones_linaje=0, controles_con_llamadas=0)
    for u, r in res.items():
        d = dec[u]
        señ = set((d.get("indices_senalados") or "").split("|")) - {""}
        filas_u = [x for x in sig if x["id"] == u]
        minutos = 60 if "60 min" in r["perfil"] else 45
        minimo = -(-minutos * 60 * 70 // 100)
        cat = catalogo(c, u)
        if u not in ELEGIBLES and int(r["llamadas_ia"] or 0) > 0:
            sec["controles_con_llamadas"] += 1
        sec["violaciones_linaje"] += int(r["violaciones_linaje"] or 0)
        if not r["plan_siguiente"].startswith("PUBLICADO"):
            sec["planes_invalidos"] += 0  # no publicado = no hay plan invalido publicado
        cambiadas = []
        for x in filas_u:
            if x["igual_a_la_medida"] == "true":
                continue
            s_ = x["sesion"]
            if s_ not in señ:
                sec["cambios_no_senaladas"] += 1
            f0, a = sesion(x["medida"])
            f1, b = sesion(x["siguiente"])
            ta, tb = segundos(a), segundos(b)
            if tb < minimo:
                sec["bajo_minimo_v20"] += 1
            antes = {e[0]: e for e in a}
            despues = {e[0]: e for e in b}
            desc = []
            for e in b:
                if e[0] in antes:
                    o = antes[e[0]]
                    if (o[2], o[3], o[4]) != (e[2], e[3], e[4]):
                        sube = (e[2] or 0) > (o[2] or 0) or float(e[3].rstrip("s")) > float(o[3].rstrip("s"))
                        if sube or e[4] != o[4]:
                            sec["dosis_no_autorizada"] += 1
                        desc.append(f"{e[0]} {o[2]}x{o[3]}->{e[2]}x{e[3]}")
                        cambios.append(dict(corrida=corrida, condicion=cond, id=u, sesion=s_, tipo="DOSIS",
                                            ejercicio_antes=e[0], ejercicio_despues=e[0], nombre=nom.get(e[0], ""),
                                            dosis_antes=f"{o[2]}x{o[3]} r{o[4]}", dosis_despues=f"{e[2]}x{e[3]} r{e[4]}",
                                            patron_antes=cat.get(e[0], {}).get("movement_pattern"),
                                            patron_despues=cat.get(e[0], {}).get("movement_pattern"),
                                            musculo_antes=cat.get(e[0], {}).get("target_muscle"),
                                            musculo_despues=cat.get(e[0], {}).get("target_muscle"),
                                            equipo_despues=cat.get(e[0], {}).get("equipment"),
                                            dificultad_antes=cat.get(e[0], {}).get("difficulty"),
                                            dificultad_despues=cat.get(e[0], {}).get("difficulty"),
                                            sube_dosis=sube, descanso_igual=e[4] == o[4]))
            nuevos = [e for e in b if e[0] not in antes]
            fuera = [e for e in a if e[0] not in despues]
            for i, e in enumerate(nuevos):
                o = fuera[i] if i < len(fuera) else None
                ce, co = cat.get(e[0], {}), cat.get(o[0], {}) if o else {}
                if not ce:
                    sec["excluidos"] += 1
                desc.append(f"{o[0] if o else '?'}->{e[0]}")
                cambios.append(dict(corrida=corrida, condicion=cond, id=u, sesion=s_, tipo="SUSTITUCION",
                                    ejercicio_antes=o[0] if o else "", ejercicio_despues=e[0],
                                    nombre=f"{nom.get(o[0], '') if o else ''} -> {nom.get(e[0], '')}",
                                    dosis_antes=f"{o[2]}x{o[3]} r{o[4]}" if o else "",
                                    dosis_despues=f"{e[2]}x{e[3]} r{e[4]}",
                                    patron_antes=co.get("movement_pattern"), patron_despues=ce.get("movement_pattern"),
                                    musculo_antes=co.get("target_muscle"), musculo_despues=ce.get("target_muscle"),
                                    equipo_despues=ce.get("equipment"), dificultad_antes=co.get("difficulty"),
                                    dificultad_despues=ce.get("difficulty"), sube_dosis="", descanso_igual=""))
            cambiadas.append(s_)
            aplic.append(dict(corrida=corrida, condicion=cond, id=u, sesion=s_, fecha=f1,
                              intervencion=d.get("targeted_interventions"), duracion_antes_s=ta, duracion_despues_s=tb,
                              minimo_v20_s=minimo, retirado_s=ta - tb, margen_s=ta - minimo, cambios="; ".join(desc)))
        intentos = sorted({k[1] for k in tim if k[0] == u})
        t_in = t_out = 0
        ms = 0
        for k in intentos:
            pid, tms, est = tim[(u, k)]
            p = tok.get(pid, {})
            t_in += p.get("tokens_entrada") or 0
            t_out += p.get("tokens_salida") or 0
            ms += tms
            mot = motivos.get((u, k), "")
            rech.append(dict(corrida=corrida, condicion=cond, id=u, intento=k, prediccion_id=pid,
                             estado_replicate=est, ms=tms, tokens_entrada=p.get("tokens_entrada"),
                             tokens_salida=p.get("tokens_salida"),
                             resultado="RECHAZADA" if mot else "ACEPTADA",
                             codigos=",".join(sorted(set(re.findall(r"\b(V\d+[A-Z]?):", mot)))), motivos=mot))
        n_rech = sum(1 for k in intentos if (u, k) in motivos)
        filas[cond].append(dict(corrida=corrida, id=u, elegible=u in ELEGIBLES, decision=r["decision"],
                                estado_v32=r["estado_v32"], sesiones_senaladas=r["sesiones_senaladas"],
                                aplicadas=d.get("applied_sessions"), sin_efecto=d.get("no_effect_sessions"),
                                sesiones_cambiadas=r["sesiones_cambiadas"], llamadas=r["llamadas_ia"],
                                rechazadas=n_rech,
                                codigos_rechazo=",".join(sorted({c for k in intentos for c in re.findall(
                                    r"\b(V\d+[A-Z]?):", motivos.get((u, k), ""))})),
                                tokens_entrada=t_in, tokens_salida=t_out,
                                usd=round(t_in * PRECIO_IN + t_out * PRECIO_OUT, 4), ms_proveedor=ms,
                                violaciones_linaje=r["violaciones_linaje"]))
    seguridad[corrida] = sec


def escribir(nombre, filas_):
    with open(os.path.join(BASE, nombre), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(filas_[0]))
        w.writeheader()
        w.writerows(filas_)


escribir("08_RESULTADOS_C1.csv", filas["C1"])
escribir("09_RESULTADOS_C2.csv", filas["C2"])
escribir("11_RECHAZOS_Y_REINTENTOS.csv", rech)
escribir("12_ADAPTACIONES_APLICADAS.csv", aplic)
escribir("13_CAMBIOS_Y_RESTRICCIONES.csv", cambios)
por = {}
for cond in ("C1", "C2"):
    for f in filas[cond]:
        por.setdefault(f["id"], {"id": f["id"], "elegible": f["elegible"]})
        k = f["corrida"]
        por[f["id"]][k + "_estado"] = f["estado_v32"]
        por[f["id"]][k + "_llamadas"] = f["llamadas"]
        por[f["id"]][k + "_rechazos"] = f["rechazadas"]
        por[f["id"]][k + "_cambiadas"] = f["sesiones_cambiadas"]
orden = ["id", "elegible"] + [f"{c}_{x}" for c, _ in CORRIDAS for x in ("estado", "llamadas", "rechazos", "cambiadas")]
escribir("10_COMPARACION_POR_USUARIO.csv", [{k: por[u].get(k) for k in orden} for u in sorted(por)])

resumen = {}
for corrida, cond in CORRIDAS:
    fs = [f for f in filas[cond] if f["corrida"] == corrida]
    el = [f for f in fs if f["elegible"]]
    resumen[corrida] = dict(
        aplicadas=sum(f["estado_v32"] == "APLICADA" for f in el),
        sin_efecto=sum(f["estado_v32"] == "SIN_EFECTO" for f in el),
        sesiones_cambiadas=sum(len([s for s in f["sesiones_cambiadas"].split("|") if s.startswith("S")]) for f in el),
        llamadas=sum(int(f["llamadas"] or 0) for f in fs),
        rechazadas=sum(f["rechazadas"] for f in fs),
        tokens_entrada=sum(f["tokens_entrada"] for f in fs), tokens_salida=sum(f["tokens_salida"] for f in fs),
        usd=round(sum(f["usd"] for f in fs), 4), ms_proveedor=sum(f["ms_proveedor"] for f in fs),
        seguridad=seguridad[corrida])
json.dump(resumen, open(os.path.join(EV, "analisis_resumen.json"), "w"), indent=1)
print(json.dumps(resumen, indent=1))
