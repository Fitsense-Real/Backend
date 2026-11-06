#!/usr/bin/env python3
"""Tablas academicas y libro Excel de la evaluacion integral O (a partir de la salida del oraculo; SOLO LECTURA).

Uso: python3 -I tablas_O.py <O_PERFILES.csv> <salida_oraculo> <salida_tablas> <corrida_G> <corrida_P> [<etiqueta> <preambulo.md>]
  <etiqueta> y <preambulo.md> (opcionales) solo cambian el titulo y el texto inicial de TABLAS_SHORT_PAPER.md.
Los valores salen de la ejecucion (tablas exportadas + oraculo); este script no recalcula reglas, solo las presenta.
"""
import csv
import json
import os
import sys
from collections import Counter, defaultdict

sys.dont_write_bytecode = True
csv.field_size_limit(sys.maxsize)
AP, FA, NA, RR = "APROBADO", "FALLO", "NO APLICA", "REQUIERE REVISION"
ASP = ["A1_generacion", "A2_personalizacion_restricciones", "A3_registro_ejecucion", "A4_adherencia",
       "A5_adaptacion_ia", "A6_mantenimiento", "A7_progresion", "A8_reincorporacion", "A9_continuidad",
       "A10_seguridad_integridad"]
NOMBRE_ASP = {"A1_generacion": "1. Generación inicial", "A2_personalizacion_restricciones": "2. Personalización y restricciones",
              "A3_registro_ejecucion": "3. Registro de ejecución", "A4_adherencia": "4. Cálculo de adherencia",
              "A5_adaptacion_ia": "5. Adaptación con IA simulada", "A6_mantenimiento": "6. Mantenimiento sin adaptar",
              "A7_progresion": "7. Progresión gradual", "A8_reincorporacion": "8. Reincorporación condicionada",
              "A9_continuidad": "9. Continuidad multisemana", "A10_seguridad_integridad": "10. Seguridad e integridad"}
OBJ = {"GAIN_MUSCLE": "Ganar masa muscular", "LOSE_WEIGHT": "Perder peso", "INCREASE_STRENGTH": "Aumentar fuerza",
       "GENERAL_FITNESS": "Condición general"}
NIV = {"BEGINNER": "Principiante", "INTERMEDIATE": "Intermedio", "ADVANCED": "Avanzado"}


SLOT4C = {"HORIZONTAL_PUSH": "PUSH", "VERTICAL_PUSH": "PUSH", "HORIZONTAL_PULL": "PULL", "VERTICAL_PULL": "PULL",
          "KNEE_DOMINANT": "KNEE", "HIP_HINGE": "HIP", "HIP_EXTENSION": "HIP"}


def slots4c(patrones):
    s = sorted({SLOT4C[p] for p in patrones if p in SLOT4C})
    return f"{len(s)}/4 ({'+'.join(s)})"


def leer(r):
    if not os.path.exists(r) or os.path.getsize(r) == 0:
        return []
    with open(r, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def escribir(ruta, filas, campos=None):
    campos = campos or (list(filas[0].keys()) if filas else ["vacio"])
    with open(ruta, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=campos, lineterminator="\n")
        w.writeheader()
        for r in filas:
            w.writerow({k: r.get(k, "") for k in campos})


def md(filas, campos):
    out = ["| " + " | ".join(campos) + " |", "|" + "---|" * len(campos)]
    for r in filas:
        out.append("| " + " | ".join(str(r.get(c, "")).replace("|", "/") for c in campos) + " |")
    return "\n".join(out)


def main():
    perfiles_csv, oraculo, salida = sys.argv[1], sys.argv[2], sys.argv[3]
    corridas = sys.argv[4:]
    os.makedirs(salida, exist_ok=True)
    perfiles = {r["id"]: r for r in leer(perfiles_csv)}
    det = {d["id"]: d for d in json.load(open(os.path.join(oraculo, "detalle.json"), encoding="utf-8"))}
    matriz = {r["id"]: r for r in leer(os.path.join(oraculo, "matriz_10_aspectos.csv"))}
    semanas = leer(os.path.join(oraculo, "semanas.csv"))
    adapt = leer(os.path.join(oraculo, "adaptaciones.csv"))
    pasos = leer(os.path.join(oraculo, "pasos_progresion.csv"))
    res = {}
    for c in corridas:
        for r in leer(os.path.join(c, "resultados_20.csv")):
            res[r["id"]] = r
    sem_por = defaultdict(list)
    for s in semanas:
        sem_por[s["id"]].append(s)
    ad_por = defaultdict(list)
    for a in adapt:
        ad_por[a["id"]].append(a)
    pa_por = defaultdict(list)
    for p in pasos:
        pa_por[p["id"]].append(p)

    t1, t2, t3, t4, t5 = [], [], [], [], []
    for pid in sorted(perfiles):
        p, d = perfiles[pid], det.get(pid, {})
        s1 = d.get("s1", {})
        t1.append({"ID": pid, "Objetivo": OBJ[p["goal_type"]], "Nivel": NIV[p["nivel"]],
                   "Equipamiento": p["tipo_equipamiento"], "Días solicitados": p["dias_por_semana"],
                   "Días programados": s1.get("sesiones", 0) if d.get("estado_generacion") == "PLAN_PUBLICADO" else 0,
                   "Duración (min)": p["minutos_sesion"], "Edad": d.get("edad_en_la_ejecucion", ""),
                   "Restricciones": p["restricciones"].replace("permite_", "sin ").replace("_", " "),
                   "Bloque": "P (progresión activada)" if p["bloque"] == "P" else "G",
                   "Escenario": p["escenario"]})
        est = d.get("estados", {})
        if d.get("estado_generacion") == "PLAN_PUBLICADO":
            mins = s1.get("minutos_declarados", [])
            t2.append({"ID": pid, "Resultado": "Plan válido" if est.get("A1_generacion") in (AP, RR) else "Plan inválido",
                       "Sesiones (esperadas)": f"{s1.get('sesiones')} ({d.get('sesiones_esperadas')})",
                       "Duración declarada por sesión (min)": "/".join(map(str, mins)),
                       "Máx. del perfil (min)": p["minutos_sesion"],
                       "Duración estimada (min)": "/".join(map(str, s1.get("minutos_estimados", []))),
                       "Ejercicios por sesión": "/".join(map(str, s1.get("ejercicios_por_sesion", []))),
                       "Slots 4C cubiertos": slots4c(s1.get("patrones", [])),
                       "Estado de dosis": s1.get("estado_dosis") or "-",
                       "Incumplimientos de validación (oráculo)": len(d.get("checks", {}).get("A1_generacion", [])),
                       "Limitaciones (/plan/limitations)": ", ".join(d.get("limitaciones_tras_generacion", [])) or "ninguna",
                       "A1": est.get("A1_generacion"), "A2": est.get("A2_personalizacion_restricciones")})
        else:
            t2.append({"ID": pid, "Resultado": "ESPERADO_SIN_PLAN" if p["escenario"] == "ESPERADO_SIN_PLAN" else "Sin plan",
                       "Sesiones (esperadas)": f"0 ({d.get('sesiones_esperadas')})",
                       "Duración declarada por sesión (min)": "-", "Máx. del perfil (min)": p["minutos_sesion"],
                       "Duración estimada (min)": "-", "Ejercicios por sesión": "-", "Slots 4C cubiertos": "-",
                       "Estado de dosis": "-", "Incumplimientos de validación (oráculo)": "-",
                       "Limitaciones (/plan/limitations)": ", ".join(d.get("limitaciones_tras_generacion", [])) or "ninguna",
                       "A1": est.get("A1_generacion"), "A2": est.get("A2_personalizacion_restricciones")})
        ss = [s for s in sem_por.get(pid, []) if int(s["semana"]) <= 4]
        if ss:
            prog = sum(int(s["programadas"]) for s in ss)
            t3.append({"ID": pid, "Sesiones programadas (S1-S4)": prog,
                       "Completas": sum(int(s["completas"]) for s in ss),
                       "Parciales": sum(int(s["parciales"]) for s in ss),
                       "Omitidas": sum(int(s["omitidas"]) for s in ss),
                       "Sin registro": sum(int(s["sin_registro"]) for s in ss),
                       "Adherencia observada S1/S2/S3/S4 (%)": " / ".join(s["adherencia_observada"] for s in ss),
                       "Cobertura S1 (%)": ss[0]["cobertura"],
                       "Señal detectada": " | ".join(f"S{s['semana']}: {s['senal']}" for s in ss if s["senal"] != "-") or "ninguna",
                       "Decisión esperada (oráculo) C1-C4": " / ".join(s["decision_oraculo"] for s in ss),
                       "Decisión obtenida (backend) C1-C4": " / ".join(s["decision_backend"] for s in ss),
                       "Coinciden": "sí" if all(s["coinciden"] == "True" for s in ss) else "NO",
                       "A3": est.get("A3_registro_ejecucion"), "A4": est.get("A4_adherencia")})
        if ss:
            mods = []
            for a in ad_por.get(pid, []):
                cambio = []
                if a["sustituciones"] != "-":
                    cambio.append("sustitución " + a["sustituciones"])
                if a["reducciones"] != "-":
                    cambio.append("reducción " + a["reducciones"])
                if a["retirados"] != "-":
                    cambio.append("retirado " + a["retirados"])
                if not cambio:
                    cambio.append("sin cambios")
                mods.append(f"C{a['ciclo']} {a['intervencion']} ({a['estado_v32']}, {a['pertinencia']}): {', '.join(cambio)}")
            pp = pa_por.get(pid, [])
            tipos = Counter(x["tipo"] for x in pp)
            reinc = d.get("reincorporaciones", [])
            todas = sem_por.get(pid, [])
            t4.append({"ID": pid, "Decisiones C1-C4": " / ".join(f"{s['decision_backend']}·{s['intervenciones']}" for s in ss),
                       "Modificaciones realizadas": " | ".join(mods) or "ninguna",
                       "Restricciones respetadas (A2)": "sí" if est.get("A2_personalizacion_restricciones") == AP else "NO",
                       "Pasos de progresión": ", ".join(f"{k}×{v}" for k, v in sorted(tipos.items())) or "0",
                       "Ciclos con paso": ", ".join(f"C{x['ciclo']}:{x['tipo']}:{x['exercise_id']}" for x in pp) or "-",
                       "Reincorporación": ", ".join(f"ej. {r['exercise_id']} en C{r['ciclo']} (retirado en C{r['retirado_en']})"
                                                    for r in reinc) or ("pendiente: " + ",".join(map(str, d.get("retirados_sin_reincorporar", [])))
                                                                         if d.get("retirados_sin_reincorporar") else "-"),
                       "Excluidos por dolor": ",".join(map(str, d.get("excluidos_por_dolor", []))) or "-",
                       "Semanas medidas": len(todas),
                       "A5": est.get("A5_adaptacion_ia"), "A6": est.get("A6_mantenimiento"),
                       "A7": est.get("A7_progresion"), "A8": est.get("A8_reincorporacion")})
        fila = {"ID": pid}
        for a in ASP:
            fila[NOMBRE_ASP[a]] = est.get(a, NA)
        t5.append(fila)
    # totales de la Tabla 5
    tot = Counter(e for r in t5 for k, e in r.items() if k != "ID")
    aplic = tot[AP] + tot[FA]
    por_asp = []
    for a in ASP:
        c = Counter(r[NOMBRE_ASP[a]] for r in t5)
        ap = c[AP] + c[FA]
        por_asp.append({"Aspecto": NOMBRE_ASP[a], "APROBADO": c[AP], "FALLO": c[FA], "NO APLICA": c[NA],
                        "REQUIERE REVISIÓN": c[RR], "Aplicables (AP+FA)": ap,
                        "Aprobación técnica": f"{100 * c[AP] / ap:.1f} %" if ap else "-"})
    por_asp.append({"Aspecto": "TOTAL (20 × 10 = 200)", "APROBADO": tot[AP], "FALLO": tot[FA], "NO APLICA": tot[NA],
                    "REQUIERE REVISIÓN": tot[RR], "Aplicables (AP+FA)": aplic,
                    "Aprobación técnica": f"{100 * tot[AP] / aplic:.1f} %" if aplic else "-"})
    escribir(os.path.join(salida, "Tabla1_perfiles.csv"), t1)
    escribir(os.path.join(salida, "Tabla2_generacion_personalizacion.csv"), t2)
    escribir(os.path.join(salida, "Tabla3_ejecucion_adherencia.csv"), t3)
    escribir(os.path.join(salida, "Tabla4_adaptacion_mantenimiento_progresion.csv"), t4)
    escribir(os.path.join(salida, "Tabla5_diez_aspectos.csv"), t5)
    escribir(os.path.join(salida, "Tabla5b_totales_por_aspecto.csv"), por_asp)
    # CSV largos
    escribir(os.path.join(salida, "generacion_por_sesion.csv"), generacion_larga(corridas, perfiles))
    escribir(os.path.join(salida, "adherencia_semanal.csv"), semanas)
    escribir(os.path.join(salida, "adaptaciones.csv"), adapt)
    escribir(os.path.join(salida, "progresion_y_reincorporacion.csv"), pasos)
    escribir(os.path.join(salida, "matriz_10_aspectos.csv"), list(matriz.values()))
    lim = []
    for c in corridas:
        ruta = os.path.join(c, "limitaciones.jsonl")
        if os.path.exists(ruta):
            for l in open(ruta, encoding="utf-8"):
                if l.strip():
                    m = json.loads(l)
                    for x in (m.get("cuerpo") or {}).get("limitations", []) or [{"code": "(ninguna)", "message": ""}]:
                        lim.append({"id": m["id"], "momento": m["momento"], "http": m.get("http"), "tipo": x.get("type"),
                                    "codigo": x.get("code"), "mensaje": x.get("message")})
    lim.sort(key=lambda r: (r["id"], r["momento"]))
    escribir(os.path.join(salida, "limitaciones.csv"), lim)
    cat = []
    for pid in sorted(det):
        rc = det[pid].get("revision_catalogo", {})
        cat.append({"ID": pid, "A1 (plan inicial)": "; ".join(rc.get("A1", [])) or "sin hallazgos",
                    "A5 (sustitutos)": "; ".join(rc.get("A5", [])) or "-", "A7 (pasos)": "; ".join(rc.get("A7", [])) or "-",
                    "Aviso E2-a tras generación": (det[pid].get("aviso_e2a") or {}).get("TRAS_GENERACION", "-")})
    escribir(os.path.join(salida, "calidad_catalogo.csv"), cat)
    # Markdown para el articulo
    with open(os.path.join(salida, "TABLAS_SHORT_PAPER.md"), "w", encoding="utf-8") as f:
        etiqueta = sys.argv[6] if len(sys.argv) > 6 else "S_O"
        f.write(f"# Tablas para el short paper (valores de la corrida {etiqueta})\n\n")
        if len(sys.argv) > 7:
            f.write(open(sys.argv[7], encoding="utf-8").read().rstrip("\n") + "\n\n")
        for titulo, filas, campos in (
                ("Tabla 1. Características de los 20 perfiles sintéticos", t1,
                 ["ID", "Objetivo", "Nivel", "Equipamiento", "Días solicitados", "Días programados", "Duración (min)",
                  "Edad", "Restricciones"]),
                ("Tabla 2. Generación y personalización", t2,
                 ["ID", "Resultado", "Sesiones (esperadas)", "Duración declarada por sesión (min)", "Máx. del perfil (min)",
                  "Slots 4C cubiertos", "Estado de dosis", "Incumplimientos de validación (oráculo)",
                  "Limitaciones (/plan/limitations)"]),
                ("Tabla 3. Ejecución y adherencia simulada (S1–S4)", t3,
                 ["ID", "Sesiones programadas (S1-S4)", "Completas", "Parciales", "Omitidas", "Sin registro",
                  "Adherencia observada S1/S2/S3/S4 (%)", "Señal detectada", "Decisión esperada (oráculo) C1-C4",
                  "Decisión obtenida (backend) C1-C4", "Coinciden"]),
                ("Tabla 4. Adaptación, mantenimiento y progresión", t4,
                 ["ID", "Decisiones C1-C4", "Modificaciones realizadas", "Restricciones respetadas (A2)",
                  "Pasos de progresión", "Reincorporación", "Semanas medidas"]),
                ("Tabla 5. Evaluación integral de los diez aspectos", t5, ["ID"] + [NOMBRE_ASP[a] for a in ASP]),
                ("Tabla 5b. Totales por aspecto", por_asp, list(por_asp[0].keys()))):
            f.write(f"## {titulo}\n\n{md(filas, campos)}\n\n")
    excel(salida, [("Tabla1_perfiles", t1), ("Tabla2_generacion", t2), ("Tabla3_adherencia", t3),
                   ("Tabla4_adaptacion", t4), ("Tabla5_aspectos", t5), ("Tabla5b_totales", por_asp),
                   ("Semanas", semanas), ("Adaptaciones", adapt), ("Progresion", pasos), ("Limitaciones", lim),
                   ("Calidad_catalogo", cat), ("Matriz_oraculo", list(matriz.values()))])
    print(md(por_asp, list(por_asp[0].keys())))


def generacion_larga(corridas, perfiles):
    """Una fila por sesion del plan inicial (S1), leida de las tablas exportadas."""
    filas = []
    for c in corridas:
        t = os.path.join(c, "tablas")
        us = {r["id"]: int(r["user_id"]) for r in leer(os.path.join(c, "usuarios.csv"))}
        planes = [p for p in leer(os.path.join(t, "weekly_training_plans.csv")) if p["week_start_date"] == "2026-11-02"]
        pw = leer(os.path.join(t, "planned_workouts.csv"))
        pwe = defaultdict(list)
        for e in leer(os.path.join(t, "planned_workout_exercises.csv")):
            pwe[e["planned_workout_id"]].append(e)
        ej = {e["exercise_id"]: e for e in leer(os.path.join(t, "exercises.csv"))}
        for pid, u in sorted(us.items()):
            pl = [p for p in planes if int(p["user_id"]) == u and not p["replaced_at"]]
            if not pl:
                continue
            for w in sorted([w for w in pw if w["plan_id"] == pl[0]["plan_id"]], key=lambda w: w["scheduled_date"]):
                lst = sorted(pwe[w["planned_workout_id"]], key=lambda e: int(e["exercise_order"]))
                filas.append({"id": pid, "fecha": w["scheduled_date"], "enfoque": w["focus_code"],
                              "minutos_declarados": w["expected_duration_minutes"], "ejercicios": len(lst),
                              "detalle": " ; ".join(f"{e['exercise_id']} {ej.get(e['exercise_id'], {}).get('name_es', '')} "
                                                    f"{e['planned_sets']}x{e['planned_reps'] or (e['planned_duration_seconds'] + 's')}"
                                                    for e in lst)})
    return filas


def excel(salida, hojas):
    from openpyxl import Workbook
    from openpyxl.styles import Font, PatternFill, Alignment
    wb = Workbook()
    wb.remove(wb.active)
    colores = {AP: "C6EFCE", FA: "FFC7CE", NA: "EDEDED", RR: "FFEB9C"}
    for nombre, filas in hojas:
        ws = wb.create_sheet(nombre[:31])
        if not filas:
            ws.append(["(vacío)"])
            continue
        campos = list(filas[0].keys())
        ws.append(campos)
        for c in ws[1]:
            c.font = Font(bold=True)
            c.alignment = Alignment(wrap_text=True, vertical="top")
        for r in filas:
            ws.append([r.get(k, "") for k in campos])
        for fila in ws.iter_rows(min_row=2):
            for c in fila:
                if isinstance(c.value, str) and c.value in colores:
                    c.fill = PatternFill("solid", fgColor=colores[c.value])
                c.alignment = Alignment(wrap_text=True, vertical="top")
        for i, k in enumerate(campos, start=1):
            ws.column_dimensions[ws.cell(row=1, column=i).column_letter].width = min(60, max(10, len(k) + 2))
        ws.freeze_panes = "B2"
    wb.save(os.path.join(salida, "EVALUACION_INTEGRAL_O_20_PERFILES.xlsx"))


if __name__ == "__main__":
    main()
