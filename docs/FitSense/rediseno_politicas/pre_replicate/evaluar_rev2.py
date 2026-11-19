#!/usr/bin/env python3
"""Evaluacion PRERREGISTRADA de R y R' con PLAN-REV-2.0 (opcion A: U + N). Fijada ANTES de cualquier ejecucion con
IA real; aplica los criterios de 23_PROTOCOLO_EVALUACION.md §5 sin modificarlos. Solo lectura.

Uso: python3 -I evaluar_rev2.py <salida_dir> U=<R_U> U=<Rp_U> N=<R_N> N=<Rp_N> [--perfiles-N=<csv>] [--perfiles-U=<csv>]
     (cada carpeta ya pasada por oraculo_k20_politica.py PLAN-REV-2.0+DIAS-1; acepta tambien corridas SIMULADAS para
     probar el script, nunca para evaluar)

Cuatro dimensiones SEPARADAS (ninguna compensa a otra):
  A FACTIBILIDAD        clasificar_resultados.py: PASS / ESPERADO_SIN_PLAN / DEFECTO por perfil; incumplimientos de
                        seguridad del oraculo (todo lo que no es «0 planes vigentes»); continuidad S2/S3.
  B ACEPTACION          por ciclo con IA (intentos_ia.csv): aceptado por los validadores en el intento 1, aceptado
                        al final (<= 3 intentos) o no aceptado (respaldo); fallos del proveedor aparte.
  C PERTINENCIA         por sesion senalada con cambio de origen IA: categoria del oraculo (ACC/ALT/INC) y
                        comprobacion REV2 por script contra el catalogo de la corrida (dificultad e impacto no
                        superiores, mismo patron y grupo, dosis no mayor, causa coherente, desmotivacion <= 2 cambios).
                        Un cambio que TOCA un ejercicio con error de catalogo (21) va a REQUIERE_REVISION: ni pertinente
                        ni fallo.
  D SIN PLAN ESPERADO   perfiles ESPERADO_SIN_PLAN: se informan aparte; nunca cuentan como PASS ni como exito de la IA.
"""
import csv
import collections
import importlib.util
import json
import math
import os
import re
import sys

sys.dont_write_bytecode = True
AQUI = os.path.dirname(os.path.abspath(__file__))
ORACULO = "oraculo_PLAN-REV-2.0+DIAS-1"
CATALOGO_REVISION = {775, 973, 1320, 580, 1261, 1308, 1195, 1196, 327, 637, 915, 916, 1007, 1008, 1009, 1010, 1046,
                     1197, 1266, 1263, 1093, 1262, 1115, 853}
CAUSA = {"REVISAR_CARGA_Y_RECUPERACION": "FATIGA", "VARIANTE_MAS_SENCILLA_O_CARGA": "DIFICIL",
         "INVESTIGAR_CAMBIO_MINIMO": "DESMOTIVACION"}


def cargar(nombre, fichero):
    spec = importlib.util.spec_from_file_location(nombre, os.path.join(AQUI, fichero))
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    return m


def leer(p):
    return list(csv.DictReader(open(p, encoding="utf-8"))) if os.path.exists(p) else []


def wilson_inferior(e, n, z=1.959964):
    if n == 0:
        return None
    p = e / n
    den = 1 + z * z / n
    return (p + z * z / (2 * n) - z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n))) / den


def catalogo(carpeta):
    t = os.path.join(carpeta, "tablas")
    bp = {r["body_part_id"]: r["code"] for r in leer(os.path.join(t, "body_parts.csv"))}
    return {int(r["exercise_id"]): {"grupo": bp.get(r["body_part_id"]), "patron": r["movement_pattern"] or "",
                                    "impacto": r["high_impact"] in ("t", "true"), "activo": r["is_active"] in ("t", "true"),
                                    "dif": int(r["difficulty_level"] or 0)}
            for r in leer(os.path.join(t, "exercises.csv"))}


def num(x):
    try:
        return float(x)
    except (TypeError, ValueError):
        return None


def pertinencia(r, cat):
    """(clase, motivos) de una sesion con cambio de origen IA."""
    su = json.loads(r["sustituciones"] or "[]")
    re_ = json.loads(r["reducciones"] or "[]")
    tocados = {s["de"] for s in su} | {s["a"] for s in su} | {x[0] for x in re_ if isinstance(x, list) and x}
    mal = []
    for s in su:
        de, a = cat.get(s["de"]), cat.get(s["a"])
        if de is None or a is None:
            mal.append(f"sustitucion {s['de']}->{s['a']} sin ficha"); continue
        if not a["activo"]: mal.append(f"{s['a']} inactivo")
        if a["dif"] > de["dif"]: mal.append(f"{s['de']}->{s['a']} sube dificultad")
        if a["impacto"] and not de["impacto"]: mal.append(f"{s['de']}->{s['a']} sube impacto")
        if a["patron"] != de["patron"]: mal.append(f"{s['de']}->{s['a']} cambia de patron")
        if a["grupo"] != de["grupo"]: mal.append(f"{s['de']}->{s['a']} cambia de grupo")
        if s.get("dosis_no_mayor") is False: mal.append(f"{s['de']}->{s['a']} dosis mayor")
    for x in re_:
        if isinstance(x, list) and len(x) >= 4 and num(x[3]) is not None and num(x[2]) is not None and num(x[3]) > num(x[2]):
            mal.append(f"{x[0]} {x[1]} sube {x[2]}->{x[3]}")
    causa = CAUSA.get(r["intervencion"])
    # «Demasiado dificil»: debe bajar la dificultad o la dosis CUANDO es posible. Si el ejercicio sustituido ya tiene la
    # dificultad minima (1), una sustitucion de la misma dificultad es lo mas sencillo que existe.
    if causa == "DIFICIL" and not re_ and not any(
            cat.get(s["a"], {}).get("dif", 9) < cat.get(s["de"], {}).get("dif", 0)
            or cat.get(s["de"], {}).get("dif", 0) <= 1 for s in su):
        mal.append("demasiado dificil sin variante mas sencilla ni menos dosis, pudiendo")
    if causa == "DESMOTIVACION" and len(su) + len(re_) > 2:
        mal.append(f"desmotivacion con {len(su) + len(re_)} cambios (> 2)")
    if r["categoria"] not in ("ACC", "ALT"):
        mal.append(f"categoria del oraculo {r['categoria']}")
    if mal:
        return "NO_PERTINENTE", mal
    if tocados & CATALOGO_REVISION:
        return "REQUIERE_REVISION", [f"toca {sorted(tocados & CATALOGO_REVISION)} (21)"]
    return "PERTINENTE", []


def analizar(carpeta, perfiles, clasif):
    run = os.path.basename(carpeta.rstrip("/"))
    cat = catalogo(carpeta)
    ses = leer(os.path.join(carpeta, ORACULO, "sesiones.csv"))
    det = json.load(open(os.path.join(carpeta, ORACULO, "detalle.json"), encoding="utf-8"))
    out = os.path.join(os.environ.get("TMPDIR_EVAL", "/tmp"), f"clas_{run}.csv")
    import subprocess
    res = subprocess.run([sys.executable, "-I", os.path.join(AQUI, "clasificar_resultados.py"), carpeta, ORACULO,
                          perfiles, out], capture_output=True, text=True)
    clases = json.loads(res.stdout.strip().splitlines()[-1])["clases"]
    por_perfil = {r["id"]: r for r in leer(out)}
    m = {"corrida": run, "perfiles": len(por_perfil)}
    for k in ("PASS", "ESPERADO_SIN_PLAN", "DEFECTO"):
        m["A_" + k] = clases.get(k, 0)
    m["A_DEFECTO_ids"] = " ".join(i for i, r in por_perfil.items() if r["clase"] == "DEFECTO")
    m["D_ESPERADO_SIN_PLAN_ids"] = " ".join(i for i, r in por_perfil.items() if r["clase"] == "ESPERADO_SIN_PLAN")
    seg = [(d["id"], e, x) for d in det for e, l in d["comprobaciones"].items() for x in l
           if not (e == "A_generacion" and x.startswith("0 planes vigentes"))]
    m["A_incumplimientos_seguridad"] = len(seg)
    m["A_incumplimientos_detalle"] = "; ".join(f"{a}/{b}: {c}" for a, b, c in seg)[:2000]
    # B aceptacion por ciclo con IA
    # Ciclos con IA = etiquetas con alguna traza de prediccion (real o simulada). Aceptado = una fila de
    # intentos_ia.csv con intento >= 1, publicable y source AI (lo publicado es la propuesta de la IA).
    trazas = collections.defaultdict(list)
    p_tr = os.path.join(carpeta, "trazas_ia.jsonl")
    for l in (open(p_tr, encoding="utf-8") if os.path.exists(p_tr) else []):
        d = json.loads(l)
        trazas[d.get("etiqueta")].append(d)
    intentos = leer(os.path.join(carpeta, "intentos_ia.csv"))
    por_et = collections.defaultdict(list)
    for x in intentos:
        if int(x["intento"] or 0) >= 1:
            por_et[x["etiqueta"]].append(x)
    pub = lambda x: x["publicable"] in ("true", "si", "SI", "1") and x["source"] == "AI"
    acept1 = aceptf = prov = sin_llamada = 0
    for et, tr in trazas.items():
        l = sorted(por_et.get(et, []), key=lambda x: int(x["intento"]))
        if l and l[0]["intento"] == "1" and pub(l[0]): acept1 += 1
        if any(pub(x) for x in l): aceptf += 1
        if any(t.get("resultado") in ("ERROR_PROVEEDOR", "FALLO_SIMULADO_DEL_PROVEEDOR") for t in tr): prov += 1
        if any(t.get("resultado") == "PRESUPUESTO_AGOTADO_SIN_LLAMADA" for t in tr): sin_llamada += 1
    m["B_ciclos_IA"] = len(trazas)
    m["B_aceptados_intento1"] = acept1
    m["B_aceptados_final"] = aceptf
    m["B_ciclos_con_fallo_proveedor"] = prov
    m["B_ciclos_sin_llamada_por_tope"] = sin_llamada
    m["B_intentos_total"] = sum(len(v) for v in trazas.values())
    # C pertinencia
    fact = [r for r in ses if r["factible_sonda"] == "si"]
    m["C_sesiones_senaladas"] = len(ses)
    m["C_sesiones_factibles"] = len(fact)
    pc = collections.Counter()
    filas = []
    for r in ses:
        if r["origen"] == "IA" and r["cambio"] == "si":
            clase, mot = pertinencia(r, cat)
        else:
            clase, mot = ("SIN_CAMBIO_IA:" + r["origen"]), []
        if r["factible_sonda"] == "si": pc[clase] += 1
        filas.append([run, r["id"], r["sesion"], r["intervencion"], r["factible_sonda"], r["origen"], r["categoria"],
                      clase, "; ".join(mot)])
    m["C_pertinentes_en_factibles"] = pc["PERTINENTE"]
    m["C_requiere_revision_en_factibles"] = pc["REQUIERE_REVISION"]
    m["C_no_pertinentes_en_factibles"] = pc["NO_PERTINENTE"]
    m["C_sin_cambio_IA_en_factibles"] = sum(v for k, v in pc.items() if k.startswith("SIN_CAMBIO_IA"))
    m["C_no_pertinentes_total"] = sum(f[7] == "NO_PERTINENTE" for f in filas)
    m["C_ACC_IA"] = sum(r["origen"] == "IA" and r["categoria"] == "ACC" for r in ses)
    resumen = leer(os.path.join(carpeta, "predicciones_metricas.csv"))
    m["predicciones"] = len(resumen)
    txt = open(os.path.join(carpeta, "99_resumen.txt"), encoding="utf-8").read() if os.path.exists(os.path.join(carpeta, "99_resumen.txt")) else ""
    g = re.search(r"usd_medido=([\d.]+)", txt)
    m["usd_medido"] = g.group(1) if g else ""
    b = re.search(r"bloqueo_presupuesto=(\S+)", txt)
    m["bloqueo_presupuesto"] = b.group(1) if b else ""
    return m, filas


def main():
    out = sys.argv[1]
    args = sys.argv[2:]
    perf = {"U": os.path.join(AQUI, "../../short_paper_ieee/evaluacion_final_k20/entrada/03_PERFILES_U01_U20.csv"),
            "N": os.path.join(AQUI, "../cohorte_N/N_PERFILES_INDEPENDIENTES.csv")}
    corr = []
    for a in args:
        if a.startswith("--perfiles-"):
            k, v = a[len("--perfiles-"):].split("=", 1); perf[k] = v
        else:
            k, v = a.split("=", 1); corr.append((k, v))
    os.makedirs(out, exist_ok=True)
    os.environ["TMPDIR_EVAL"] = out
    met, filas = {}, []
    for coh, d in corr:
        m, f = analizar(d, perf[coh], None)
        m["cohorte"] = coh
        met[f"{coh}:{m['corrida']}"] = m
        filas += f
    crit = []

    def add(cid, dim, coh, texto, valor, cumple):
        crit.append([cid, dim, coh, texto, valor, "INFORMATIVO" if cumple is None else
                     ("NO EVALUABLE" if cumple == "NE" else ("CUMPLE" if cumple else "NO CUMPLE"))])

    for coh in sorted({c for c, _ in corr}):
        ms = [m for m in met.values() if m["cohorte"] == coh]
        for m in ms:
            k = m["corrida"]
            add("A-1", "FACTIBILIDAD", coh, f"{k}: 0 DEFECTO (PASS + ESPERADO_SIN_PLAN = todos)",
                f"PASS {m['A_PASS']} · ESPERADO_SIN_PLAN {m['A_ESPERADO_SIN_PLAN']} · DEFECTO {m['A_DEFECTO']} {m['A_DEFECTO_ids']}",
                m["A_DEFECTO"] == 0)
            add("A-2", "FACTIBILIDAD", coh, f"{k}: 0 incumplimientos de seguridad del oraculo", m["A_incumplimientos_seguridad"],
                m["A_incumplimientos_seguridad"] == 0)
            add("B-0", "ACEPTACION", coh, f"{k}: ciclos con IA / aceptados intento 1 / aceptados al final / con fallo del proveedor",
                f"{m['B_ciclos_IA']} / {m['B_aceptados_intento1']} / {m['B_aceptados_final']} / {m['B_ciclos_con_fallo_proveedor']}", None)
            fp = m["B_ciclos_con_fallo_proveedor"] / m["B_ciclos_IA"] if m["B_ciclos_IA"] else 0
            add("B-2", "ACEPTACION", coh, f"{k}: ciclos con fallo del proveedor <= 10 % (si no: INCIDENCIA_PROVEEDOR, la corrida"
                " no vale para B ni C y se evalua aparte)", f"{m['B_ciclos_con_fallo_proveedor']}/{m['B_ciclos_IA']}", fp <= 0.10)
            add("C-1", "PERTINENCIA", coh, f"{k}: 0 cambios de la IA NO pertinentes (todas las sesiones)",
                m["C_no_pertinentes_total"], m["C_no_pertinentes_total"] == 0)
            add("D-1", "SIN_PLAN_ESPERADO", coh, f"{k}: perfiles ESPERADO_SIN_PLAN (aparte; no cuentan como PASS ni como exito)",
                m["D_ESPERADO_SIN_PLAN_ids"] or "ninguno", None)
            add("G-1", "GASTO", coh, f"{k}: predicciones / USD medido / bloqueo", f"{m['predicciones']} / {m['usd_medido']} / {m['bloqueo_presupuesto']}", None)
        ciclos = sum(m["B_ciclos_IA"] for m in ms)
        acep = sum(m["B_aceptados_final"] for m in ms)
        prov = sum(m["B_ciclos_con_fallo_proveedor"] for m in ms)
        den = ciclos - prov
        umbral = 0.90 if coh == "U" else 0.80
        add("B-1", "ACEPTACION", coh, f"R + R': aceptados por los validadores al final >= {umbral:.0%} de los ciclos con IA (sin fallos del proveedor)",
            f"{acep}/{den}" + (f" = {acep / den:.1%}" if den else ""), den > 0 and acep / den >= umbral)
        if coh != "U":
            wl = wilson_inferior(acep, den)
            add("B-1", "ACEPTACION", coh, "R + R': limite inferior de Wilson al 95 % >= 63 %", "n/d" if wl is None else f"{wl:.1%}",
                wl is not None and wl >= 0.63)
        efec = sum(m["C_pertinentes_en_factibles"] for m in ms)
        rev = sum(m["C_requiere_revision_en_factibles"] for m in ms)
        fac = sum(m["C_sesiones_factibles"] for m in ms) - rev
        add("C-2", "PERTINENCIA", coh, f"R + R': efectivas y pertinentes >= {umbral:.0%} de las factibles evaluables (sin REQUIERE_REVISION: {rev})",
            f"{efec}/{fac}" + (f" = {efec / fac:.1%}" if fac else ""), fac > 0 and efec / fac >= umbral)
        if coh != "U":
            wl = wilson_inferior(efec, fac)
            add("C-2", "PERTINENCIA", coh, "R + R': limite inferior de Wilson al 95 % >= 63 %", "n/d" if wl is None else f"{wl:.1%}",
                wl is not None and wl >= 0.63)
        minimo = 0.80 if coh == "U" else 0.75
        for m in ms:
            fm = m["C_sesiones_factibles"] - m["C_requiere_revision_en_factibles"]
            add("C-3", "PERTINENCIA", coh, f"{m['corrida']}: ninguna corrida por debajo del {minimo:.0%} de sus factibles evaluables",
                f"{m['C_pertinentes_en_factibles']}/{fm}", "NE" if fm == 0 else m["C_pertinentes_en_factibles"] / fm >= minimo)
        add("C-4", "PERTINENCIA", coh, "R + R': cambios que tocan ejercicios con error de catalogo (REQUIERE_REVISION)", rev, None)
    with open(os.path.join(out, "CRITERIOS_REV2.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n"); w.writerow(["criterio", "dimension", "cohorte", "comprobacion", "valor", "resultado"]); w.writerows(crit)
    with open(os.path.join(out, "METRICAS_REV2.csv"), "w", newline="", encoding="utf-8") as f:
        claves = list(dict.fromkeys(k for m in met.values() for k in m))
        w = csv.writer(f, lineterminator="\n"); w.writerow(["metrica"] + list(met)); [w.writerow([k] + [met[c].get(k, "") for c in met]) for k in claves]
    with open(os.path.join(out, "SESIONES_REV2.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["corrida", "id", "sesion", "intervencion", "factible_sonda", "origen", "categoria", "pertinencia", "motivos"]); w.writerows(filas)
    for c in crit:
        print(" | ".join(str(x) for x in c))


if __name__ == "__main__":
    main()
