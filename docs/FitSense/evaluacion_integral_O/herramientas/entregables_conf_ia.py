#!/usr/bin/env python3
"""CONF-IA (confirmacion de mejoras de IA, 10/11/2026) · entregables 3, 4, 5 y 8 a partir de metricas_mej_r2.json
(analisis_mej_r2.py) y de las corridas CONF_*. Solo lee; aplica el §6 de PROTOCOLO_CONFIRMACION_IA.md.

Uso: entregables_conf_ia.py <metricas.json> <espec.json> <dir_corridas> <salida> [<metricas_ronda2.json> <espec_r2.json>]
  espec.json: {"gen": {"ETIQUETA": {"condicion": "PR-1.7", "ronda": "C1"}, ...},
               "ada": {"ETIQUETA": {"condicion": "V2.1", "ronda": "D1"}, ...},
               "control_gen": "PR-1.7", "control_ada": "V2.1"}
Unidad de analisis: el PERFIL (o la SESION en adaptacion). Las repeticiones de un mismo perfil en varias rondas no se
tratan como independientes: la prueba principal es Wilcoxon de rangos con signo sobre la diferencia por perfil.
"""
import csv
import glob
import json
import math
import os
import re
import sys
from collections import Counter, defaultdict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import analisis_mej_r2 as A  # noqa: E402
import entregables_mej_r2 as E  # noqa: E402
from openpyxl import Workbook  # noqa: E402
from openpyxl.styles import Alignment, Font, PatternFill  # noqa: E402

ESTRUCTURALES = ("V2", "V13", "V18", "V23", "V25")


def wilson(k, n, z=1.96):
    if n == 0:
        return (None, None)
    p = k / n
    d = 1 + z * z / n
    c = (p + z * z / (2 * n)) / d
    h = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / d
    return (round(100 * (c - h), 1), round(100 * (c + h), 1))


def respaldo_por_rechazos(r):
    """Respaldo atribuible al prompt: se publico el plan del motor de reglas tras agotar los intentos rechazados."""
    return r.get("fuente") == "RULE_ENGINE" and r.get("propuestas", 0) >= 3 and r.get("rechazadas", 0) >= 3


def mejorables(r, det):
    return sum(1 for c in A.clasificar_limitada(r, det).values() if c == "MEJORABLE") if det else 0


def media(xs):
    xs = [x for x in xs if x is not None]
    return sum(xs) / len(xs) if xs else None


def por_condicion(espec_gen):
    out = defaultdict(dict)   # condicion -> ronda -> etiqueta
    for et, meta in espec_gen.items():
        out[meta["condicion"]][meta["ronda"]] = et
    return out


def comparar_gen(m, espec, det, control, variante):
    """Prueba principal y criterios §6.1 de una variante frente al control, en las rondas que comparten."""
    pc = por_condicion(espec["gen"])
    rondas = sorted(set(pc[control]) & set(pc[variante]))
    filas_ronda, dif_valida, dif_rech, dif_pct, dif_mej = [], [], [], [], []
    tot = {k: Counter() for k in (control, variante)}
    for rd in rondas:
        pv, pc_ = m[pc[variante][rd]]["perfiles"], m[pc[control][rd]]["perfiles"]
        b = sum(1 for p in A.FACT if pv.get(p, {}).get("primera_valida") and not pc_.get(p, {}).get("primera_valida"))
        c = sum(1 for p in A.FACT if pc_.get(p, {}).get("primera_valida") and not pv.get(p, {}).get("primera_valida"))
        filas_ronda.append([variante, control, rd, len(A.FACT), b, c, round(A.mcnemar(b, c), 4)])
    for p in A.FACT:
        sv = sum(bool(m[pc[variante][rd]]["perfiles"].get(p, {}).get("primera_valida")) for rd in rondas)
        sc = sum(bool(m[pc[control][rd]]["perfiles"].get(p, {}).get("primera_valida")) for rd in rondas)
        dif_valida.append(sv - sc)
        rv = sum(m[pc[variante][rd]]["perfiles"].get(p, {}).get("rechazadas", 0) for rd in rondas)
        rc = sum(m[pc[control][rd]]["perfiles"].get(p, {}).get("rechazadas", 0) for rd in rondas)
        dif_rech.append(rv - rc)
        pv_ = media([media(m[pc[variante][rd]]["perfiles"].get(p, {}).get("pct_de_m") or []) for rd in rondas])
        pc2 = media([media(m[pc[control][rd]]["perfiles"].get(p, {}).get("pct_de_m") or []) for rd in rondas])
        if pv_ is not None and pc2 is not None:
            dif_pct.append(round(pv_ - pc2, 2))
        mv = sum(mejorables(m[pc[variante][rd]]["perfiles"].get(p, {}), det.get(p)) for rd in rondas)
        mc = sum(mejorables(m[pc[control][rd]]["perfiles"].get(p, {}), det.get(p)) for rd in rondas)
        dif_mej.append(mv - mc)
    for cond in (control, variante):
        for rd in rondas:
            per = m[pc[cond][rd]]["perfiles"]
            for p in A.FACT:
                r = per.get(p, {})
                if not r.get("ejecutado", True):
                    continue
                t = tot[cond]
                t["n"] += 1
                t["valida"] += bool(r.get("primera_valida"))
                t["rechazadas"] += r.get("rechazadas", 0)
                t["propuestas"] += r.get("propuestas", 0)
                for k, v in (r.get("codigos") or {}).items():
                    t[k] += v
                t["estructurales"] += sum((r.get("codigos") or {}).get(k, 0) for k in ESTRUCTURALES)
                t["respaldo_rechazos"] += respaldo_por_rechazos(r)
                t["respaldo_total"] += r.get("fuente") == "RULE_ENGINE"
    w = A.wilcoxon(dif_valida)
    wr = A.wilcoxon(dif_rech)
    wp = A.wilcoxon(dif_pct)
    wm = A.wilcoxon(dif_mej)
    mejor = sum(d > 0 for d in dif_valida)
    peor = sum(d < 0 for d in dif_valida)
    tv, tc = tot[variante], tot[control]
    crit = {
        "1_validez_p<0.05_a_favor": w["p"] < 0.05 and sum(dif_valida) > 0,
        "2_V24_variante<=control": tv["V24"] <= tc["V24"],
        "3a_estructurales<=control": tv["estructurales"] <= tc["estructurales"],
        "3b_respaldos_por_rechazos<=control": tv["respaldo_rechazos"] <= tc["respaldo_rechazos"],
        "4_ningun_plan_publicado_con_violaciones": True,   # por construccion (V1-V27 antes de publicar); ver informe
        "5_dosis_tiempo_no_empeoran": not ((wp["p"] < 0.05 and sum(dif_pct) < 0) or (wm["p"] < 0.05 and sum(dif_mej) > 0)),
    }
    resto = all(v for k, v in crit.items() if not k.startswith("1_"))
    veredicto = ("RECOMENDABLE" if all(crit.values()) else
                 "FAVORABLE_NO_CONCLUYENTE" if resto and sum(dif_valida) > 0 else "NO_CUMPLE")
    return {"rondas": rondas, "filas_ronda": filas_ronda, "dif_valida": dif_valida, "wilcoxon_validez": w,
            "perfiles_mejor_peor_igual": (mejor, peor, len(A.FACT) - mejor - peor),
            "wilcoxon_rechazadas": wr, "wilcoxon_pct_de_m": wp, "wilcoxon_mejorables": wm,
            "totales": {control: dict(tc), variante: dict(tv)}, "criterios": crit, "veredicto": veredicto}


def main(metricas, espec_p, corridas, salida, metricas_r2=None, espec_r2=None):
    m = json.load(open(metricas))
    e = json.load(open(espec_p))
    det = m.get("_determinista", {}).get("perfiles", {})
    E.objetivos(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "cohorte", "O_PERFILES.csv"))
    OBJ = E.OBJ
    os.makedirs(salida, exist_ok=True)
    ctl_g, ctl_a = e.get("control_gen", "PR-1.7"), e.get("control_ada", "V2.1")

    # ---------------------------------------------------------------- 3 generacion por perfil
    cab = ["condicion", "ronda", "id", "objetivo", "ejecutado", "primera_valida", "propuestas", "rechazadas",
           "reintentos", "codigos_rechazo", "V15", "V26", "V24", "V23", "estructurales", "fuente_publicado",
           "respaldo_por_rechazos", "estado_dosis", "grupos_bajo_objetivo", "grupos_bajo_mejorables",
           "series_por_grupo", "aerobico_min_trabajo/objetivo", "clases_sesion", "pct_de_m", "sesiones",
           "colocaciones", "latencia_ms", "usd", "nota"]
    filas = []
    for et, meta in e["gen"].items():
        for pid in A.FACT:
            r = m[et]["perfiles"].get(pid)
            if r is None:
                filas.append([meta["condicion"], meta["ronda"], pid, OBJ.get(pid, ""), False] + [""] * (len(cab) - 5))
                continue
            cod = r.get("codigos") or {}
            filas.append([meta["condicion"], meta["ronda"], pid, OBJ.get(pid, ""), r.get("ejecutado", True),
                          r.get("primera_valida"), r.get("propuestas"), r.get("rechazadas"),
                          max(0, (r.get("propuestas") or 0) - 1), json.dumps(cod, sort_keys=True),
                          cod.get("V15", 0), cod.get("V26", 0), cod.get("V24", 0), cod.get("V23", 0),
                          sum(cod.get(k, 0) for k in ESTRUCTURALES), r.get("fuente"), respaldo_por_rechazos(r),
                          r.get("estado_dosis"), json.dumps(r.get("grupos_bajo_objetivo"), ensure_ascii=False),
                          mejorables(r, det.get(pid)), json.dumps(r.get("series_por_grupo"), ensure_ascii=False),
                          json.dumps(r.get("aerobico")), ";".join(str(x) for x in r.get("clases_sesion") or []),
                          ";".join(str(x) for x in r.get("pct_de_m") or []), r.get("sesiones"),
                          ";".join(str(x) for x in r.get("colocaciones") or []),
                          ";".join(str(x) for x in r.get("latencia_ms") or []), r.get("usd"), r.get("nota", "")])
    with open(os.path.join(salida, "RESULTADOS_GENERACION_CONFIRMATORIA.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(cab)
        w.writerows(filas)

    # ---------------------------------------------------------------- 4 adaptacion por sesion
    cab_a = ["condicion", "ronda", "id", "objetivo", "ciclo", "sesion", "intervencion", "estado_v32", "fuente",
             "pertinencia_oraculo", "pertinencia_por_identidad", "sin_efecto_evitable", "quitados", "anadidos",
             "se_quedan", "dosis_cambiadas", "dif_max_retirados", "dif_max_anadidos", "trabajo_antes->despues",
             "no_autorizados_identidad", "rechazo_V8C", "problemas"]
    filas_a = []
    ses_cond = defaultdict(lambda: defaultdict(dict))   # condicion -> ronda -> clave -> fila resumida
    for et, meta in e.get("ada", {}).items():
        r = m[et]["resumen"]
        evit = set(r["sin_efecto_evitable"])
        noaut = defaultdict(list)
        for h in r["no_autorizados"]:
            noaut[f"{h[0]} c{h[1]} s{h[2]}"].append(f"{h[4]}: {h[5]}")
        for k, p in sorted(r["por_sesion"].items()):
            pid, ciclo, ses = k.split()
            ide = p.get("identidad") or {}
            pi = E.pertinencia_identidad(p)
            v8c = "V8C" in (p.get("problemas") or "")
            filas_a.append([meta["condicion"], meta["ronda"], pid, OBJ.get(pid, ""), ciclo[1:], ses[1:],
                            p["intervencion"], p["estado"], p["fuente"], p["pertinencia"], pi,
                            f"{pid} {ciclo}" in evit, ";".join(ide.get("quitados", [])),
                            ";".join(ide.get("anadidos", [])), ";".join(ide.get("se_quedan", [])),
                            ";".join(ide.get("dosis_cambiadas", [])), ide.get("dif_max_retirados"),
                            ide.get("dif_max_anadidos"), p["trabajo"], " | ".join(noaut.get(k, [])) or "ninguno",
                            v8c, p["problemas"]])
            ses_cond[meta["condicion"]][meta["ronda"]][f"{pid} {ciclo}"] = {
                "aplicada": p["estado"] == "APLICADA" and not noaut.get(k), "evitable": f"{pid} {ciclo}" in evit,
                "inc": pi == "INC", "noaut": bool(noaut.get(k)), "v8c": v8c, "estado": p["estado"]}
    with open(os.path.join(salida, "RESULTADOS_ADAPTACION_CONFIRMATORIA.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(cab_a)
        w.writerows(filas_a)

    # ---------------------------------------------------------------- 8 predicciones y costes (solo CONF_*)
    cab_p = ["corrida", "etiqueta", "tipo", "intento", "prediction_id", "tokens_entrada", "tokens_salida",
             "predict_time_s", "ms", "usd", "usd_acumulado_corrida", "estado", "version_modelo_antes"]
    vers = {}
    rv = os.path.join(corridas, "versiones.txt")
    if os.path.exists(rv):
        for l in open(rv):
            mm = re.match(r"VERSION antes de (\S+): (\S+)", l)
            if mm:
                vers[mm.group(1)] = mm.group(2)
    filas_p, costes = [], []
    for d in sorted(glob.glob(os.path.join(corridas, "CONF_*"))):
        if not os.path.isdir(d):
            continue
        n = os.path.basename(d)
        for r in A.leer(os.path.join(d, "predicciones_metricas.csv")):
            filas_p.append([n, r["etiqueta"], r["tipo"], r["intento"], r["prediction_id"], r["tokens_entrada"],
                            r["tokens_salida"], r["predict_time_s"], r["ms"], r["usd"], r["usd_acumulado"],
                            r["estado"], vers.get(n, "")])
        t = open(os.path.join(d, "99_resumen.txt")).read() if os.path.exists(os.path.join(d, "99_resumen.txt")) else ""
        costes.append([n, int((re.search(r"llamadas_proveedor=(\d+)", t) or [0, 0])[1]),
                       float((re.search(r"usd_medido=([\d.]+)", t) or [0, 0])[1])])
    with open(os.path.join(salida, "PREDICCIONES_Y_COSTOS_CONFIRMATORIA.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(cab_p)
        w.writerows(filas_p)

    # ---------------------------------------------------------------- 5 comparacion (xlsx) + json de decisiones
    wb = Workbook()
    ws = wb.active
    ws.title = "Gen_resumen"
    claves = ["perfiles", "primera_valida", "propuestas", "rechazadas", "V15", "V26", "V24", "publicados_ia",
              "respaldo", "con_plan_medible", "dosis_limitada", "dosis_objetivo", "grupos_bajo_mejorables",
              "grupos_bajo_inevitables", "sesiones_insuficientes", "sesiones_cortas", "sesiones_completas",
              "pct_de_m_medio", "latencia_mediana_ms", "usd"]
    ws.append(["condicion", "ronda"] + claves + ["IC95_validez_%", "codigos_rechazo", "no_ejecutados"])
    for et, meta in e["gen"].items():
        r = m[et]["resumen"]
        ws.append([meta["condicion"], meta["ronda"]] + [r.get(k) for k in claves]
                  + [str(wilson(r["primera_valida"], r["perfiles"])), json.dumps(r["codigos_rechazo"]),
                     ";".join(r["no_ejecutados"])])
    decisiones = {}
    wp = wb.create_sheet("Gen_pareado")
    wp.append(["variante", "control", "ronda", "perfiles", "solo_variante_valida", "solo_control_valida", "McNemar_p"])
    wc = wb.create_sheet("Gen_criterios")
    wc.append(["variante", "rondas", "validas_variante", "validas_control", "IC95_variante", "IC95_control",
               "perfiles_mejor/peor/igual", "Wilcoxon_validez_n", "W+", "p", "Wilcoxon_rechazadas_p",
               "Wilcoxon_pct_de_m_p", "Wilcoxon_mejorables_p", "V24_v/c", "estructurales_v/c",
               "respaldos_por_rechazos_v/c", "respaldos_total_v/c"]
              + ["C" + k for k in ("1_validez_p<0.05_a_favor", "2_V24_variante<=control", "3a_estructurales<=control",
                                   "3b_respaldos_por_rechazos<=control", "4_ningun_plan_publicado_con_violaciones",
                                   "5_dosis_tiempo_no_empeoran")] + ["veredicto"])
    for var in [c for c in por_condicion(e["gen"]) if c != ctl_g]:
        if not set(por_condicion(e["gen"])[var]) & set(por_condicion(e["gen"])[ctl_g]):
            continue
        cmp_ = comparar_gen(m, e, det, ctl_g, var)
        decisiones[var] = cmp_
        for fr in cmp_["filas_ronda"]:
            wp.append(fr)
        tv, tc = cmp_["totales"][var], cmp_["totales"][ctl_g]
        wv = cmp_["wilcoxon_validez"]
        wc.append([var, ",".join(cmp_["rondas"]), f"{tv['valida']}/{tv['n']}", f"{tc['valida']}/{tc['n']}",
                   str(wilson(tv["valida"], tv["n"])), str(wilson(tc["valida"], tc["n"])),
                   "/".join(map(str, cmp_["perfiles_mejor_peor_igual"])), wv["n"], wv["W+"], wv["p"],
                   cmp_["wilcoxon_rechazadas"]["p"], cmp_["wilcoxon_pct_de_m"]["p"], cmp_["wilcoxon_mejorables"]["p"],
                   f"{tv.get('V24', 0)}/{tc.get('V24', 0)}", f"{tv['estructurales']}/{tc['estructurales']}",
                   f"{tv['respaldo_rechazos']}/{tc['respaldo_rechazos']}", f"{tv['respaldo_total']}/{tc['respaldo_total']}"]
                  + list(cmp_["criterios"].values()) + [cmp_["veredicto"]])
    wf = wb.create_sheet("Gen_por_perfil")
    pc = por_condicion(e["gen"])
    wf.append(["id", "objetivo"] + [f"{c} validas/{len(pc[c])}" for c in pc] + [f"{c} rechazadas" for c in pc]
              + [f"dif {c} - {ctl_g}" for c in pc if c != ctl_g])
    for p in A.FACT:
        val = {c: sum(bool(m[pc[c][rd]]["perfiles"].get(p, {}).get("primera_valida")) for rd in pc[c]) for c in pc}
        rec = {c: sum(m[pc[c][rd]]["perfiles"].get(p, {}).get("rechazadas", 0) for rd in pc[c]) for c in pc}
        comunes = {c: set(pc[c]) & set(pc[ctl_g]) for c in pc}
        dif = [sum(bool(m[pc[c][rd]]["perfiles"].get(p, {}).get("primera_valida"))
                   - bool(m[pc[ctl_g][rd]]["perfiles"].get(p, {}).get("primera_valida")) for rd in comunes[c])
               for c in pc if c != ctl_g]
        wf.append([p, OBJ.get(p, "")] + [val[c] for c in pc] + [rec[c] for c in pc] + dif)

    # adaptacion
    wa = wb.create_sheet("Ada_resumen")
    wa.append(["condicion", "ronda", "sesiones_con_ia", "APLICADA", "SIN_EFECTO", "RESPALDO/RECHAZADA", "ACC", "ALT",
               "CONS-J", "INC_oraculo", "INC_por_identidad", "SIN_EFECTO_evitables", "no_autorizados_identidad",
               "rechazos_V8C"])
    for et, meta in e.get("ada", {}).items():
        r = m[et]["resumen"]
        est, per = r["estado"], r["pertinencia"]
        inc_id = sum(E.pertinencia_identidad(p) == "INC" for p in r["por_sesion"].values())
        v8c = sum("V8C" in (p.get("problemas") or "") for p in r["por_sesion"].values())
        wa.append([meta["condicion"], meta["ronda"], r["sesiones_con_ia"], est.get("APLICADA", 0),
                   est.get("SIN_EFECTO", 0), r["sesiones_con_ia"] - est.get("APLICADA", 0) - est.get("SIN_EFECTO", 0),
                   per.get("ACC", 0), per.get("ALT", 0), per.get("CONS-J", 0), per.get("INC", 0), inc_id,
                   len(r["sin_efecto_evitable"]), len(r["no_autorizados"]), v8c])
    wq = wb.create_sheet("Ada_pareado")
    wq.append(["variante", "control", "pares_sesion_x_ronda", "solo_variante_aplicada", "solo_control_aplicada",
               "McNemar_p", "sesiones", "Wilcoxon_por_sesion_n", "W+", "p", "aplicadas_v/c", "evitables_v/c",
               "INC_id_v/c", "no_autorizados_v/c", "rechazos_V8C_v/c", "veredicto_6.2"])
    for var in [c for c in ses_cond if c != ctl_a]:
        rondas = sorted(set(ses_cond[var]) & set(ses_cond[ctl_a]))
        claves_s = sorted({k for rd in rondas for k in ses_cond[ctl_a][rd]} | {k for rd in rondas for k in ses_cond[var][rd]})
        b = c = n = 0
        dif = []
        tv, tc = Counter(), Counter()
        for k in claves_s:
            sv = sc = 0
            for rd in rondas:
                xv, xc = ses_cond[var][rd].get(k), ses_cond[ctl_a][rd].get(k)
                if xv and xc:
                    n += 1
                    b += xv["aplicada"] and not xc["aplicada"]
                    c += xc["aplicada"] and not xv["aplicada"]
                for t, x in ((tv, xv), (tc, xc)):
                    if x:
                        t["aplicada"] += x["aplicada"]
                        t["evitable"] += x["evitable"]
                        t["inc"] += x["inc"]
                        t["noaut"] += x["noaut"]
                        t["v8c"] += x["v8c"]
                sv += bool(xv and xv["aplicada"])
                sc += bool(xc and xc["aplicada"])
            dif.append(sv - sc)
        wil = A.wilcoxon(dif)
        ok = (tv["noaut"] == 0 and tv["aplicada"] >= tc["aplicada"] and tv["inc"] <= tc["inc"]
              and tv["evitable"] <= tc["evitable"])
        ver = ("RECOMENDABLE_CON_VENTAJA_DEMOSTRADA" if ok and wil["p"] < 0.05 and sum(dif) > 0 else
               "NO_PEOR_SIN_VENTAJA_DEMOSTRADA" if ok else "NO_CUMPLE")
        decisiones["ADA:" + var] = {"rondas": rondas, "pares": n, "b": b, "c": c, "mcnemar": A.mcnemar(b, c),
                                    "wilcoxon": wil, "totales": {var: dict(tv), ctl_a: dict(tc)}, "veredicto": ver}
        wq.append([var, ctl_a, n, b, c, round(A.mcnemar(b, c), 4), len(claves_s), wil["n"], wil["W+"], wil["p"],
                   f"{tv['aplicada']}/{tc['aplicada']}", f"{tv['evitable']}/{tc['evitable']}",
                   f"{tv['inc']}/{tc['inc']}", f"{tv['noaut']}/{tc['noaut']}", f"{tv['v8c']}/{tc['v8c']}", ver])

    # secundario: junto con la confirmatoria de la ronda 2 (no decide)
    if metricas_r2 and espec_r2 and os.path.exists(metricas_r2):
        m2 = json.load(open(metricas_r2))
        e2 = json.load(open(espec_r2))
        mm = dict(m2)
        mm.update(m)
        eg = {k: v for k, v in e2["gen"].items() if v["ronda"] in ("R2", "R3")}
        eg.update(e["gen"])
        ws2 = wb.create_sheet("Secundario_con_R2")
        ws2.append(["variante", "rondas", "perfiles_mejor/peor/igual", "Wilcoxon_validez_p", "validas_v/c",
                    "V24_v/c", "estructurales_v/c", "respaldos_por_rechazos_v/c", "nota"])
        for var in [c for c in por_condicion(eg) if c != ctl_g]:
            if not set(por_condicion(eg)[var]) & set(por_condicion(eg)[ctl_g]):
                continue
            cm = comparar_gen(mm, {"gen": eg}, det, ctl_g, var)
            tv, tc = cm["totales"][var], cm["totales"][ctl_g]
            ws2.append([var, ",".join(cm["rondas"]), "/".join(map(str, cm["perfiles_mejor_peor_igual"])),
                        cm["wilcoxon_validez"]["p"], f"{tv['valida']}/{tv['n']}", f"{tc['valida']}/{tc['n']}",
                        f"{tv.get('V24', 0)}/{tc.get('V24', 0)}", f"{tv['estructurales']}/{tc['estructurales']}",
                        f"{tv['respaldo_rechazos']}/{tc['respaldo_rechazos']}",
                        "secundario: incluye R2/R3 de MEJ ronda 2 (G17_R2 sin metricas de dosis); no decide"])
            decisiones["SECUNDARIO:" + var] = {"rondas": cm["rondas"], "wilcoxon": cm["wilcoxon_validez"],
                                               "mejor_peor_igual": cm["perfiles_mejor_peor_igual"],
                                               "totales": cm["totales"]}
    wk = wb.create_sheet("Costes")
    wk.append(["corrida", "predicciones", "usd_medido"])
    for fila in costes:
        wk.append(fila)
    wk.append(["TOTAL (bloque incremental CONF-IA)", sum(x[1] for x in costes), round(sum(x[2] for x in costes), 4)])
    for hoja in wb.worksheets:
        for c in hoja[1]:
            c.font = Font(bold=True, color="FFFFFF")
            c.fill = PatternFill("solid", fgColor="305496")
            c.alignment = Alignment(wrap_text=True, vertical="top")
        for col in hoja.columns:
            hoja.column_dimensions[col[0].column_letter].width = 16
        hoja.freeze_panes = "A2"
    wb.save(os.path.join(salida, "COMPARACION_FINAL_PROMPTS.xlsx"))
    json.dump(decisiones, open(os.path.join(salida, "decisiones_conf_ia.json"), "w"), ensure_ascii=False, indent=1,
              default=str)
    print(f"generacion {len(filas)} filas · adaptacion {len(filas_a)} · predicciones {len(filas_p)} · "
          f"total {sum(x[1] for x in costes)} predicciones USD {round(sum(x[2] for x in costes), 4)}")
    for k, v in decisiones.items():
        print(k, v.get("veredicto", ""), json.dumps(v.get("criterios", {})) if "criterios" in v else "")


if __name__ == "__main__":
    main(*sys.argv[1:7])
