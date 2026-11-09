#!/usr/bin/env python3
"""MEJ ronda 2 · entregables 5, 6, 7 y 10 a partir de metricas_mej_r2.json (analisis_mej_r2.py) y las corridas.

Uso: entregables_mej_r2.py <metricas_mej_r2.json> <espec.json> <dir_corridas> <salida_dir>
  espec.json: {"gen": {"ETIQUETA_RONDA": {"condicion": "PR-1.7", "ronda": "R1"}, ...},
               "ada": {"ETIQUETA_RONDA": {"condicion": "V2.1", "ronda": "R1"}, ...}}
Escribe RESULTADOS_GENERACION_MEJ_R2.csv, RESULTADOS_ADAPTACION_MEJ_R2.csv, PREDICCIONES_Y_COSTOS.csv y
COMPARACION_PROMPTS_MEJ_R2.xlsx. No modifica nada de entrada.
"""
import csv
import glob
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import analisis_mej_r2 as A  # noqa: E402

from openpyxl import Workbook  # noqa: E402
from openpyxl.styles import Alignment, Font, PatternFill  # noqa: E402

OBJ = {}


def objetivos(perfiles_csv):
    for r in csv.DictReader(open(perfiles_csv, encoding="utf-8")):
        OBJ[r["id"]] = r["goal_type"]


def pertinencia_identidad(p):
    """Recalcula SOLO cuando el oraculo dice INC, con la comparacion por identidad (artefacto posicional, O04/O12)."""
    iv, ora, ide = p["intervencion"], p["pertinencia"], p.get("identidad")
    if ora != "INC" or not ide:
        return ora
    antes, despues = (int(x) for x in p["trabajo"].split("->"))
    mas_trabajo = despues > antes or len(ide["anadidos"]) > len(ide["quitados"])
    mas_dificil = bool(ide["mas_dificil_identidad"])
    if iv == "SEGURIDAD_ANTE_DOLOR":
        return "INC" if (mas_trabajo or mas_dificil) else "NO_INC (identidad)"
    if iv == "VARIANTE_MAS_SENCILLA_O_CARGA":
        return "INC" if (mas_trabajo or mas_dificil) else "NO_INC (identidad)"
    return ora


def main(metricas, espec, corridas, salida):
    m = json.load(open(metricas))
    e = json.load(open(espec))
    objetivos(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "cohorte", "O_PERFILES.csv"))
    os.makedirs(salida, exist_ok=True)
    det = m.get("_determinista", {}).get("perfiles", {})

    # ---------------------------------------------------------------- 5 generacion
    cab = ["condicion", "ronda", "id", "objetivo", "ejecutado", "primera_valida", "propuestas", "rechazadas",
           "codigos_rechazo", "fuente_plan", "latencia_ms", "usd", "estado_dosis", "series_por_grupo",
           "grupos_bajo_objetivo", "grupos_bajo_clasificacion", "aerobico_min_trabajo/objetivo", "clases_sesion",
           "pct_de_m_por_sesion", "sesiones", "colocaciones_por_sesion", "nota"]
    filas_g = []
    for et, meta in e["gen"].items():
        for pid, r in m[et]["perfiles"].items():
            cls = A.clasificar_limitada(r, det[pid]) if det.get(pid) and r["estado_dosis"] else {}
            filas_g.append([meta["condicion"], meta["ronda"], pid, OBJ.get(pid, ""), r["ejecutado"],
                            r["primera_valida"], r["propuestas"], r["rechazadas"],
                            ";".join(f"{k}x{v}" for k, v in sorted(r["codigos"].items())), r["fuente"],
                            ";".join(map(str, r["latencia_ms"])), r["usd"], r["estado_dosis"],
                            json.dumps(r["series_por_grupo"], ensure_ascii=False),
                            ";".join(map(str, r["grupos_bajo_objetivo"])),
                            ";".join(f"{k}:{v}" for k, v in cls.items()),
                            f"{r['aerobico'][0]}/{r['aerobico'][1]}", ";".join(map(str, r["clases_sesion"])),
                            ";".join(map(str, r["pct_de_m"])), r["sesiones"],
                            ";".join(map(str, r["colocaciones"])), r.get("nota", "")])
    for pid, r in det.items():
        filas_g.append(["DETERMINISTA (referencia, sin IA)", "-", pid, OBJ.get(pid, ""), "", "", "", "", "",
                        r["fuente"], "", 0, r["estado_dosis"], json.dumps(r["series_por_grupo"], ensure_ascii=False),
                        ";".join(map(str, r["grupos_bajo_objetivo"])), "", f"{r['aerobico'][0]}/{r['aerobico'][1]}",
                        ";".join(map(str, r["clases_sesion"])), ";".join(map(str, r["pct_de_m"])), r["sesiones"],
                        ";".join(map(str, r["colocaciones"])), "referencia de DOSIS_LIMITADA mejorable/inevitable"])
    with open(os.path.join(salida, "RESULTADOS_GENERACION_MEJ_R2.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(cab)
        w.writerows(filas_g)

    # ---------------------------------------------------------------- 6 adaptacion
    cab_a = ["condicion", "ronda", "id", "objetivo", "ciclo", "sesion", "intervencion", "estado_v32", "fuente",
             "pertinencia_oraculo", "pertinencia_identidad", "sustituciones_oraculo(posicional)", "reducciones",
             "retirados_oraculo", "quitados_identidad", "anadidos_identidad", "se_quedan", "dosis_cambiadas",
             "dif_max_retirados", "dif_max_anadidos", "trabajo_s_antes->despues", "no_autorizado", "problemas"]
    filas_a = []
    for et, meta in e["ada"].items():
        res = m[et]["resumen"]
        noaut = {}
        for h in res["no_autorizados"]:
            noaut.setdefault(f"{h[0]} c{h[1]} s{h[2]}", []).append(f"{h[4]}: {h[5]}")
        for k, p in res["por_sesion"].items():
            pid, ciclo, ses = k.split(" ")
            ide = p.get("identidad") or {}
            filas_a.append([meta["condicion"], meta["ronda"], pid, OBJ.get(pid, ""), ciclo[1:], ses[1:],
                            p["intervencion"], p["estado"], p["fuente"], p["pertinencia"], pertinencia_identidad(p),
                            p["sustituciones"], p["reducciones"], p["retirados"],
                            ";".join(ide.get("quitados", [])), ";".join(ide.get("anadidos", [])),
                            ";".join(ide.get("se_quedan", [])), ";".join(ide.get("dosis_cambiadas", [])),
                            ide.get("dif_max_retirados"), ide.get("dif_max_anadidos"), p["trabajo"],
                            " | ".join(noaut.get(k, [])) or "ninguno", p["problemas"]])
    with open(os.path.join(salida, "RESULTADOS_ADAPTACION_MEJ_R2.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(cab_a)
        w.writerows(filas_a)

    # ---------------------------------------------------------------- 10 predicciones y costes
    cab_p = ["corrida", "etiqueta", "tipo", "intento", "prediction_id", "tokens_entrada", "tokens_salida",
             "predict_time_s", "ms", "usd", "usd_acumulado_corrida", "estado", "version_modelo_antes"]
    filas_p = []
    vers = {}
    rv = os.path.join(corridas, "versiones.txt")
    if os.path.exists(rv):
        for l in open(rv):
            mm = re.match(r"VERSION antes de (\S+): (\S+)", l)
            if mm:
                vers[mm.group(1)] = mm.group(2)
    for d in sorted(glob.glob(os.path.join(corridas, "MEJR2_*"))):
        if not os.path.isdir(d):
            continue
        n = os.path.basename(d)
        for r in A.leer(os.path.join(d, "predicciones_metricas.csv")):
            filas_p.append([n, r["etiqueta"], r["tipo"], r["intento"], r["prediction_id"], r["tokens_entrada"],
                            r["tokens_salida"], r["predict_time_s"], r["ms"], r["usd"], r["usd_acumulado"],
                            r["estado"], vers.get(n, "")])
    with open(os.path.join(salida, "PREDICCIONES_Y_COSTOS.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(cab_p)
        w.writerows(filas_p)

    # ---------------------------------------------------------------- 7 comparacion (xlsx)
    wb = Workbook()
    ws = wb.active
    ws.title = "Generacion_resumen"
    claves = ["perfiles", "primera_valida", "propuestas", "rechazadas", "V15", "V26", "V24", "publicados_ia",
              "respaldo", "con_plan_medible", "dosis_limitada", "dosis_objetivo", "grupos_bajo_mejorables",
              "grupos_bajo_inevitables", "sesiones_insuficientes", "sesiones_cortas", "sesiones_completas",
              "pct_de_m_medio", "latencia_mediana_ms", "usd"]
    ws.append(["condicion", "ronda"] + claves + ["codigos_rechazo", "no_ejecutados"])
    for et, meta in e["gen"].items():
        r = m[et]["resumen"]
        ws.append([meta["condicion"], meta["ronda"]] + [r.get(k) for k in claves]
                  + [json.dumps(r["codigos_rechazo"]), ";".join(r["no_ejecutados"])])
    # pareado por ronda y agregado
    wp = wb.create_sheet("Generacion_pareado")
    wp.append(["variante", "control", "ronda", "perfiles_pareados", "solo_variante_valida", "solo_control_valida",
               "McNemar_p", "dif_rechazadas_Wilcoxon_n", "W+", "p", "dif_pct_de_m_n", "W+", "p"])
    por_ronda = {}
    for et, meta in e["gen"].items():
        por_ronda.setdefault(meta["ronda"], {})[meta["condicion"]] = m[et]["perfiles"]
    agregado = {}
    for ronda, conds in sorted(por_ronda.items()):
        ctl = conds.get("PR-1.7")
        if not ctl:
            continue
        for cond, var in conds.items():
            if cond == "PR-1.7":
                continue
            pr = A.pareado(var, ctl)
            wp.append([cond, "PR-1.7", ronda, pr["perfiles"], pr["solo_variante"], pr["solo_control"],
                       pr["mcnemar_p"], pr["rechazadas_dif"]["n"], pr["rechazadas_dif"]["W+"],
                       pr["rechazadas_dif"]["p"], pr["pct_de_m_dif"]["n"], pr["pct_de_m_dif"]["W+"],
                       pr["pct_de_m_dif"]["p"]])
            claves_ag = [(cond, "AGREGADO (pares perfil x ronda, todas)")]
            if ronda != "R1":  # R1 sirvio para elegir las variantes: la confirmatoria se agrega aparte
                claves_ag.append((cond, "AGREGADO CONFIRMATORIO (R2+R3)"))
            for k in claves_ag:
                a = agregado.setdefault(k, [0, 0, 0])
                a[0] += pr["perfiles"]
                a[1] += pr["solo_variante"]
                a[2] += pr["solo_control"]
    for (cond, etq), (n, b, c) in agregado.items():
        wp.append([cond, "PR-1.7", etq, n, b, c, round(A.mcnemar(b, c), 4)])
    # por perfil
    wf = wb.create_sheet("Generacion_por_perfil")
    conds = list(dict.fromkeys(f"{v['condicion']} {v['ronda']}" for v in e["gen"].values()))
    wf.append(["id", "objetivo"] + [f"{c} primera_valida" for c in conds] + [f"{c} estado_dosis" for c in conds]
              + ["DETERMINISTA estado_dosis"])
    for pid in A.FACT:
        fila = [pid, OBJ.get(pid, "")]
        for campo in ("primera_valida", "estado_dosis"):
            for et, meta in e["gen"].items():
                r = m[et]["perfiles"].get(pid, {})
                fila.append(r.get(campo) if r.get("ejecutado", True) else "NO_EJECUTADO")
        fila.append(det.get(pid, {}).get("estado_dosis", ""))
        wf.append(fila)
    # adaptacion
    wa = wb.create_sheet("Adaptacion_resumen")
    wa.append(["condicion", "ronda", "sesiones_con_ia", "APLICADA", "SIN_EFECTO", "RECHAZADA/otras", "ACC", "ALT",
               "CONS-J", "INC_oraculo", "INC_por_identidad", "SIN_EFECTO_evitables", "no_autorizados_identidad"])
    for et, meta in e["ada"].items():
        r = m[et]["resumen"]
        est, per = r["estado"], r["pertinencia"]
        inc_id = sum(pertinencia_identidad(p) == "INC" for p in r["por_sesion"].values())
        wa.append([meta["condicion"], meta["ronda"], r["sesiones_con_ia"], est.get("APLICADA", 0),
                   est.get("SIN_EFECTO", 0), r["sesiones_con_ia"] - est.get("APLICADA", 0) - est.get("SIN_EFECTO", 0),
                   per.get("ACC", 0), per.get("ALT", 0), per.get("CONS-J", 0), per.get("INC", 0), inc_id,
                   len(r["sin_efecto_evitable"]), len(r["no_autorizados"])])
    # costes
    wc = wb.create_sheet("Costes")
    wc.append(["corrida", "predicciones", "usd_medido"])
    tot_n = tot_u = 0
    for d in sorted(glob.glob(os.path.join(corridas, "MEJR2_*"))):
        if os.path.isdir(d):
            t = open(os.path.join(d, "99_resumen.txt")).read() if os.path.exists(os.path.join(d, "99_resumen.txt")) else ""
            n = int((re.search(r"llamadas_proveedor=(\d+)", t) or [0, 0])[1])
            u = float((re.search(r"usd_medido=([\d.]+)", t) or [0, 0])[1])
            tot_n += n
            tot_u += u
            wc.append([os.path.basename(d), n, u])
    wc.append(["TOTAL", tot_n, round(tot_u, 4)])
    for hoja in wb.worksheets:
        for c in hoja[1]:
            c.font = Font(bold=True, color="FFFFFF")
            c.fill = PatternFill("solid", fgColor="305496")
            c.alignment = Alignment(wrap_text=True, vertical="top")
        for col in hoja.columns:
            hoja.column_dimensions[col[0].column_letter].width = 16
        hoja.freeze_panes = "A2"
    wb.save(os.path.join(salida, "COMPARACION_PROMPTS_MEJ_R2.xlsx"))
    print(f"generacion {len(filas_g)} filas · adaptacion {len(filas_a)} · predicciones {len(filas_p)} "
          f"· total {tot_n} predicciones USD {round(tot_u, 4)}")


if __name__ == "__main__":
    main(*sys.argv[1:5])
