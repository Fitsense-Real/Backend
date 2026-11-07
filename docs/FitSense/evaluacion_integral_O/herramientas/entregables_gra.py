#!/usr/bin/env python3
"""Entregables de la evaluacion GRA (generacion inicial + adaptacion con Replicate real). SOLO LECTURA: presenta lo
que produjeron las corridas y el oraculo; no evalua reglas.

Uso:
  python3 -I entregables_gra.py <salida> <O_PERFILES.csv> <O11F35.csv> \
      R1=<G>,<P>,<F35>,<oraculo_GP>,<oraculo_F35>  R2=<G>,<P>,<F35>,<oraculo_GP>,<oraculo_F35>
Escribe en <salida>: RESULTADOS_GENERACION_REPLICATE.csv, RESULTADOS_ADAPTACION_REPLICATE.csv, RESULTADOS_ADHERENCIA.csv,
RESULTADOS_PROGRESION_REINCORPORACION.csv, MATRIZ_10_ASPECTOS.csv, REPRODUCIBILIDAD_GENERACION_ADAPTACION.csv,
PREDICCIONES_Y_COSTOS_REPLICATE.csv, SOLICITUDES_Y_RESPUESTAS_REPLICATE.csv, TABLAS_GRA.md y
EVALUACION_20_PERFILES_IA_REAL.xlsx (una hoja por usuario y hojas de detalle).
"""
import csv
import importlib.util
import json
import os
import re
import sys
from collections import Counter, defaultdict
from datetime import date

csv.field_size_limit(sys.maxsize)
AQUI = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("ar", os.path.join(AQUI, "analisis_replicate_O.py"))
AR = importlib.util.module_from_spec(spec)
spec.loader.exec_module(AR)
ASP = ["A1_generacion", "A2_personalizacion_restricciones", "A3_registro_ejecucion", "A4_adherencia",
       "A5_adaptacion_ia", "A6_mantenimiento", "A7_progresion", "A8_reincorporacion", "A9_continuidad",
       "A10_seguridad_integridad"]
S1 = "2026-11-02"


def leer(r):
    if not os.path.exists(r) or os.path.getsize(r) == 0:
        return []
    with open(r, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def escribir(r, filas, campos=None):
    if not filas:
        open(r, "w").close()
        return
    campos = campos or list(dict.fromkeys(k for f in filas for k in f))
    with open(r, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=campos, lineterminator="\n", extrasaction="ignore")
        w.writeheader()
        w.writerows(filas)


def md(filas, campos=None):
    if not filas:
        return "(sin filas)"
    c = campos or list(filas[0])
    return "\n".join(["| " + " | ".join(c) + " |", "|" + "---|" * len(c)] +
                     ["| " + " | ".join(str(f.get(k, "")) for k in c) + " |" for f in filas])


def motivos_agregados(filas):
    c = Counter()
    for x in filas:
        for part in x["motivos_rechazo_codigos"].split("; "):
            if "×" in part:
                k, v = part.split("×")
                c[k] += int(v)
    return "; ".join(f"{k}×{v}" for k, v in sorted(c.items())) or "-"


def codigos(motivo):
    return sorted(set(re.findall(r"\b(V\d+[A-Z]?)\b", motivo or "")))


class Run:
    def __init__(self, etiq, dirs):
        self.etiq = etiq
        self.g, self.p, self.f, self.ora, self.ora_f = dirs
        self.dirs = [d for d in (self.g, self.p, self.f) if d and os.path.isdir(d)]
        self.mat, self.det = {}, {}
        for o in (self.ora, self.ora_f):
            for r in leer(os.path.join(o, "matriz_10_aspectos.csv")):
                self.mat[r["id"]] = r
            if os.path.exists(os.path.join(o, "detalle.json")):
                for d in json.load(open(os.path.join(o, "detalle.json"), encoding="utf-8")):
                    self.det[d["id"]] = d
        self.tab = {}
        self.usuarios = {}
        for d in self.dirs:
            us = {r["id"]: r["user_id"] for r in leer(os.path.join(d, "usuarios.csv"))}
            for pid, u in us.items():
                self.usuarios[pid] = (d, u)
        self.preds = []
        for d in self.dirs:
            for r in leer(os.path.join(d, "predicciones_metricas.csv")):
                self.preds.append({"corrida": etiq, "bloque": os.path.basename(d.rstrip("/")), **r})
        self.gen = []
        for d in self.dirs:
            self.gen.extend(leer(os.path.join(d, "generacion_ia.csv")))
        self.trazas = []
        for d in self.dirs:
            ruta = os.path.join(d, "trazas_ia.jsonl")
            if os.path.exists(ruta):
                for l in open(ruta, encoding="utf-8"):
                    if l.strip():
                        self.trazas.append((d, json.loads(l)))

    def tabla(self, d, n):
        k = (d, n)
        if k not in self.tab:
            self.tab[k] = leer(os.path.join(d, "tablas", n + ".csv"))
        return self.tab[k]

    def plan_inicial(self, pid):
        if pid not in self.usuarios:
            return None, []
        d, u = self.usuarios[pid]
        planes = [p for p in self.tabla(d, "weekly_training_plans") if p["user_id"] == u and p["week_start_date"] == S1]
        if not planes:
            return None, []
        plan = sorted(planes, key=lambda p: int(p["plan_version"]))[0]
        ex = {r["exercise_id"]: r for r in self.tabla(d, "exercises")}
        pw = sorted([w for w in self.tabla(d, "planned_workouts") if w["plan_id"] == plan["plan_id"]],
                    key=lambda w: (w["scheduled_date"], int(w["display_order"] or 0)))
        pwe = self.tabla(d, "planned_workout_exercises")
        filas = []
        for w in pw:
            for e in sorted([x for x in pwe if x["planned_workout_id"] == w["planned_workout_id"]],
                            key=lambda x: int(x["exercise_order"])):
                c = ex.get(e["exercise_id"], {})
                filas.append({"corrida": self.etiq, "id": pid, "fecha": w["scheduled_date"], "enfoque": w["focus_code"],
                              "minutos_declarados": w["expected_duration_minutes"], "orden": e["exercise_order"],
                              "exercise_id": e["exercise_id"], "ejercicio": c.get("name_es") or c.get("name", ""),
                              "patron": c.get("movement_pattern", ""), "dificultad": c.get("difficulty_level", ""),
                              "tipo": e["prescription_type"], "series": e["planned_sets"], "reps": e["planned_reps"],
                              "segundos": e["planned_duration_seconds"], "descanso_s": e["rest_seconds"]})
        return plan, filas


def generacion(runs, perfiles):
    filas = []
    for R in runs:
        por = defaultdict(list)
        for g in R.gen:
            por[g["id"]].append(g)
        pred = {p["prediction_id"]: p for p in R.preds}
        for pid in perfiles:
            if pid not in R.usuarios:   # perfil no ejecutado en esta corrida (p. ej. bloque ausente)
                continue
            gs = por.get(pid, [])
            llam = [g for g in gs if g["resultado_proveedor"] not in ("SIN_LLAMADA", "")]
            plan, ej = R.plan_inicial(pid)
            d = R.det.get(pid, {})
            s1 = d.get("s1", {})
            ps = [pred[g["prediction_id"]] for g in llam if g["prediction_id"] in pred]
            mot = Counter(c for g in llam if g["rechazada_por_el_backend"] == "SI" for c in codigos(g["motivos"]))
            fuente = plan["generation_source"] if plan else "SIN_PLAN"
            filas.append({
                "corrida": R.etiq, "id": pid, "llamadas_ia": len(llam),
                "valida_al_primer_intento": "SI" if llam and llam[0]["rechazada_por_el_backend"] == "NO" else "NO",
                "propuestas_rechazadas": sum(1 for g in llam if g["rechazada_por_el_backend"] == "SI"),
                "reintentos": max(0, len(llam) - 1),
                "motivos_rechazo_codigos": "; ".join(f"{k}×{v}" for k, v in sorted(mot.items())) or "-",
                "errores_proveedor": sum(1 for g in llam if g["resultado_proveedor"] != "RESPUESTA"),
                "plan_publicado_desde": {"AI": "IA (Replicate)", "RULE_ENGINE": "RESPALDO determinista"}.get(fuente, fuente),
                "generation_attempts": plan["generation_attempts"] if plan else "-",
                "sin_plan_motivo": (gs[0]["error_generacion"][:140] if gs and not plan else "-"),
                "sesiones": s1.get("sesiones", "-"), "minutos_declarados": "/".join(map(str, s1.get("minutos_declarados", []))) or "-",
                "minutos_estimados": "/".join(map(str, s1.get("minutos_estimados", []))) or "-",
                "ejercicios_por_sesion": "/".join(map(str, s1.get("ejercicios_por_sesion", []))) or "-",
                "patrones_4C": ",".join(s1.get("patrones", [])) or "-", "estado_dosis": s1.get("estado_dosis") or "-",
                "A1": R.mat.get(pid, {}).get("A1_generacion", "-"),
                "A2_restricciones": R.mat.get(pid, {}).get("A2_personalizacion_restricciones", "-"),
                "latencia_total_s": round(sum(int(p["ms"]) for p in ps if p["ms"] not in ("", "-1")) / 1000, 1) if ps else 0,
                "tokens_entrada": sum(int(p["tokens_entrada"]) for p in ps if p["tokens_entrada"] not in ("", "-1")),
                "tokens_salida": sum(int(p["tokens_salida"]) for p in ps if p["tokens_salida"] not in ("", "-1")),
                "usd": round(sum(float(p["usd"] or 0) for p in ps), 6),
                "predicciones": ";".join(g["prediction_id"] for g in llam)})
    return filas


def main():
    salida, perf_csv, f35_csv = sys.argv[1:4]
    os.makedirs(salida, exist_ok=True)
    runs = []
    for a in sys.argv[4:]:
        e, d = a.split("=", 1)
        runs.append(Run(e, d.split(",")))
    perfiles = [r["id"] for r in leer(perf_csv)] + [r["id"] for r in leer(f35_csv)]
    prof = {r["id"]: r for r in leer(perf_csv) + leer(f35_csv)}
    gen = generacion(runs, perfiles)
    escribir(os.path.join(salida, "RESULTADOS_GENERACION_REPLICATE.csv"), gen)
    # Adaptacion: ciclos con orden de IA (analisis_replicate_O.corrida), solo predicciones de ADAPTACION
    ada = []
    cic = {}
    for R in runs:
        c = AR.corrida(R.etiq, [R.g, R.p, R.f, R.ora, R.ora_f])
        cic[R.etiq] = c
        for x in c["ciclos"]:
            ada.append(x)
    escribir(os.path.join(salida, "RESULTADOS_ADAPTACION_REPLICATE.csv"), ada)
    # Adherencia, progresion, matriz
    adh, prog, mat = [], [], []
    for R in runs:
        for o in (R.ora, R.ora_f):
            for r in leer(os.path.join(o, "semanas.csv")):
                adh.append({"corrida": R.etiq, **r})
            for r in leer(os.path.join(o, "pasos_progresion.csv")):
                prog.append({"corrida": R.etiq, **r})
        for pid in perfiles:
            if pid in R.mat:
                mat.append({"corrida": R.etiq, **R.mat[pid]})
    escribir(os.path.join(salida, "RESULTADOS_ADHERENCIA.csv"), adh)
    escribir(os.path.join(salida, "RESULTADOS_PROGRESION_REINCORPORACION.csv"), prog)
    escribir(os.path.join(salida, "MATRIZ_10_ASPECTOS.csv"), mat)
    # Predicciones y costes
    pc = []
    for R in runs:
        for p in R.preds:
            pc.append({"corrida": R.etiq, "etapa": "GENERACION" if p["tipo"] == "GENERACION" else "ADAPTACION", **p})
    escribir(os.path.join(salida, "PREDICCIONES_Y_COSTOS_REPLICATE.csv"), pc)
    # Solicitudes y respuestas (sin credenciales: el prompt se referencia por fichero y huella; la respuesta completa)
    sr = []
    for R in runs:
        for d, t in R.trazas:
            sr.append({"corrida": R.etiq, "bloque": os.path.basename(d.rstrip("/")), "etiqueta": t.get("etiqueta"),
                       "etapa": t.get("tipo"), "intento": t.get("intento"), "prediction_id": t.get("prediction_id", ""),
                       "prompt_fichero": f"prompts/{str(t.get('etiqueta', '')).replace('/', '_')}_i{t.get('intento')}.txt",
                       "prompt_caracteres": t.get("prompt_chars", ""), "prompt_sha256_16": t.get("prompt_sha256_16", ""),
                       "resultado": t.get("resultado"), "error": t.get("error", ""),
                       "respuesta": str(t.get("salida", ""))})
    escribir(os.path.join(salida, "SOLICITUDES_Y_RESPUESTAS_REPLICATE.csv"), sr)
    # Reproducibilidad R1 vs R2
    rep = []
    if len(runs) >= 2:
        A, B = runs[0], runs[1]
        ga = {g["id"]: g for g in gen if g["corrida"] == A.etiq}
        gb = {g["id"]: g for g in gen if g["corrida"] == B.etiq}
        for pid in perfiles:
            _, ea = A.plan_inicial(pid)
            _, eb = B.plan_inicial(pid)
            sa, sb = {e["exercise_id"] for e in ea}, {e["exercise_id"] for e in eb}
            jac = round(len(sa & sb) / len(sa | sb), 2) if sa | sb else "-"
            exacto = [(e["fecha"], e["exercise_id"], e["series"], e["reps"], e["segundos"]) for e in ea] == \
                     [(e["fecha"], e["exercise_id"], e["series"], e["reps"], e["segundos"]) for e in eb] and bool(ea)
            rep.append({"etapa": "GENERACION", "id": pid, "ciclo": "-",
                        f"{A.etiq}": f"{ga[pid]['plan_publicado_desde']} · {ga[pid]['llamadas_ia']} llamada(s) · A1 {ga[pid]['A1']}",
                        f"{B.etiq}": f"{gb[pid]['plan_publicado_desde']} · {gb[pid]['llamadas_ia']} llamada(s) · A1 {gb[pid]['A1']}",
                        "misma_fuente": ga[pid]["plan_publicado_desde"] == gb[pid]["plan_publicado_desde"],
                        "mismas_sesiones": ga[pid]["sesiones"] == gb[pid]["sesiones"],
                        "jaccard_ejercicios": jac, "plan_identico": exacto})
        ca = {(x["id"], x["ciclo"]): x for x in cic[A.etiq]["ciclos"]}
        cb = {(x["id"], x["ciclo"]): x for x in cic[B.etiq]["ciclos"]}
        for k in sorted(set(ca) | set(cb)):
            x, y = ca.get(k), cb.get(k)
            rep.append({"etapa": "ADAPTACION", "id": k[0], "ciclo": k[1],
                        f"{A.etiq}": f"{x['intervenciones']} · {x['clasificacion']} · {x['pertinencia']}" if x else "(sin orden de IA)",
                        f"{B.etiq}": f"{y['intervenciones']} · {y['clasificacion']} · {y['pertinencia']}" if y else "(sin orden de IA)",
                        "misma_fuente": bool(x and y and x["clasificacion"] == y["clasificacion"]),
                        "mismas_sesiones": bool(x and y and x["intervenciones"] == y["intervenciones"]),
                        "jaccard_ejercicios": "-", "plan_identico": bool(x and y and x["cambios"] == y["cambios"])})
    escribir(os.path.join(salida, "REPRODUCIBILIDAD_GENERACION_ADAPTACION.csv"), rep)
    # Tablas resumen
    t_gen, t_ada, t_cost = [], [], []
    for R in runs:
        g = [x for x in gen if x["corrida"] == R.etiq]
        fact = [x for x in g if x["llamadas_ia"] > 0]
        t_gen.append({"Corrida": R.etiq, "Perfiles": len(g), "Factibles (con llamada)": len(fact),
                      "Sin plan (esperado)": sum(1 for x in g if x["plan_publicado_desde"] == "SIN_PLAN"),
                      "Llamadas de generación": sum(x["llamadas_ia"] for x in g),
                      "Válidas al 1.er intento": sum(1 for x in fact if x["valida_al_primer_intento"] == "SI"),
                      "Propuestas rechazadas": sum(x["propuestas_rechazadas"] for x in g),
                      "Errores del proveedor": sum(x["errores_proveedor"] for x in g),
                      "Planes publicados desde la IA": sum(1 for x in g if x["plan_publicado_desde"] == "IA (Replicate)"),
                      "Planes por respaldo": sum(1 for x in g if x["plan_publicado_desde"].startswith("RESPALDO")),
                      "Motivos de rechazo": motivos_agregados(g)})
        c = [x for x in cic[R.etiq]["ciclos"]]
        cl = Counter(x["clasificacion"] for x in c)
        per = Counter(p.split(":")[-1] for x in c for p in x["pertinencia"].split(";") if p != "-")
        t_ada.append({"Corrida": R.etiq, "Ciclos con orden de IA": len(c),
                      "Aceptadas al 1.er intento": cl["ACEPTADA_INTENTO_1"], "Aceptadas tras reintento": cl["ACEPTADA_TRAS_REINTENTO"],
                      "Intentos rechazados": sum(int(x["rechazos"]) for x in c),
                      "SIN_EFECTO propuesto por la IA": cl["SIN_EFECTO_PROPUESTO_POR_IA"],
                      "Respaldo": cl["RESPALDO_TRAS_RECHAZOS"] + cl["RESPALDO_TRAS_ERROR_PROVEEDOR"],
                      "Publicadas desde la IA": f"{cl['ACEPTADA_INTENTO_1'] + cl['ACEPTADA_TRAS_REINTENTO']}/{len(c)}",
                      "ACC/ALT/CONS-J/INC": f"{per['ACC']}/{per['ALT']}/{per['CONS-J']}/{per['INC']}"})
        for etapa in ("GENERACION", "ADAPTACION"):
            ps = [p for p in pc if p["corrida"] == R.etiq and p["etapa"] == etapa]
            lat = sorted(int(p["ms"]) for p in ps if p["ms"] not in ("", "-1"))
            t_cost.append({"Corrida": R.etiq, "Etapa": etapa, "Predicciones": len(ps),
                           "Tokens entrada": sum(int(p["tokens_entrada"]) for p in ps if p["tokens_entrada"] not in ("", "-1")),
                           "Tokens salida": sum(int(p["tokens_salida"]) for p in ps if p["tokens_salida"] not in ("", "-1")),
                           "Latencia mediana (s)": round(lat[len(lat) // 2] / 1000, 1) if lat else "-",
                           "Latencia máx. (s)": round(lat[-1] / 1000, 1) if lat else "-",
                           "USD": round(sum(float(p["usd"] or 0) for p in ps), 4)})
    t_mat = []
    for R in runs:
        cc = Counter(r[a] for pid, r in R.mat.items() if pid in prof and pid != "O11F35" for a in ASP)
        ap, fa = cc["APROBADO"], cc["FALLO"]
        t_mat.append({"Corrida": R.etiq, "APROBADO": ap, "FALLO": fa, "NO APLICA": cc["NO APLICA"],
                      "REQUIERE REVISIÓN": cc["REQUIERE REVISION"],
                      "Aprobación técnica": f"{ap}/{ap + fa} = {100 * ap / (ap + fa):.1f} %" if ap + fa else "-"})
    escribir(os.path.join(salida, "_tabla_generacion.csv"), t_gen)
    escribir(os.path.join(salida, "_tabla_adaptacion.csv"), t_ada)
    escribir(os.path.join(salida, "_tabla_costes.csv"), t_cost)
    escribir(os.path.join(salida, "_tabla_matriz.csv"), t_mat)
    with open(os.path.join(salida, "TABLAS_GRA.md"), "w", encoding="utf-8") as f:
        f.write("# Tablas GRA (generación inicial + adaptación con Replicate real)\n\n")
        for t, filas in (("Generación inicial con Replicate (agregado)", t_gen), ("Adaptación con Replicate (agregado)", t_ada),
                         ("Diez aspectos (20 perfiles; O11-F35 aparte)", t_mat), ("Rendimiento y costes por etapa", t_cost)):
            f.write(f"## {t}\n\n{md(filas)}\n\n")
    # Excel
    from openpyxl import Workbook
    from openpyxl.styles import Font, Alignment
    wb = Workbook()
    wb.remove(wb.active)

    def hoja(nombre, filas, campos=None):
        ws = wb.create_sheet(nombre[:31])
        if not filas:
            ws.append(["(sin filas)"])
            return ws
        c = campos or list(dict.fromkeys(k for x in filas for k in x))
        ws.append(c)
        for cel in ws[1]:
            cel.font = Font(bold=True)
        for x in filas:
            ws.append([str(x.get(k, ""))[:32000] for k in c])
        return ws

    hoja("Resumen generacion", t_gen)
    hoja("Resumen adaptacion", t_ada)
    hoja("Resumen 10 aspectos", t_mat)
    hoja("Costes por etapa", t_cost)
    for pid in perfiles:
        ws = wb.create_sheet(pid[:31])
        p = prof[pid]
        ws.append([f"{pid} · {p['goal_type']} · {p['nivel']} · {p.get('tipo_equipamiento') or p['equipamiento']} · "
                   f"{p['dias_por_semana']} días × {p['minutos_sesion']} min · escenario {p['escenario']}"])
        ws["A1"].font = Font(bold=True)
        ws.append([f"Restricciones: {p['restricciones']} · bloqueados: {p.get('ejercicios_bloqueados') or '-'} · "
                   f"guion S1–S4: {p['semana1']} | {p['semana2']} | {p['semana3']} | {p['semana4']}"])
        for R in runs:
            ws.append([])
            ws.append([f"=== {R.etiq} ==="])
            g = next((x for x in gen if x["corrida"] == R.etiq and x["id"] == pid), {})
            ws.append(["Generación", f"desde {g.get('plan_publicado_desde')}", f"llamadas {g.get('llamadas_ia')}",
                       f"válida 1.er intento {g.get('valida_al_primer_intento')}", f"rechazos {g.get('motivos_rechazo_codigos')}",
                       f"USD {g.get('usd')}", f"latencia {g.get('latencia_total_s')} s"])
            _, ej = R.plan_inicial(pid)
            ws.append(["Plan inicial: fecha", "enfoque", "min", "ej.", "ejercicio", "patrón", "dif.", "tipo", "series",
                       "reps", "seg.", "desc."])
            for e in ej:
                ws.append([e["fecha"], e["enfoque"], e["minutos_declarados"], e["exercise_id"], e["ejercicio"], e["patron"],
                           e["dificultad"], e["tipo"], e["series"], e["reps"], e["segundos"], e["descanso_s"]])
            ws.append(["Semanas: semana", "programadas", "completas", "parciales", "omitidas", "sin registro",
                       "adherencia obs.", "señal", "decisión backend", "decisión oráculo", "coinciden", "estado V32"])
            for s in [x for x in adh if x["corrida"] == R.etiq and x["id"] == pid]:
                ws.append([s["semana"], s["programadas"], s["completas"], s["parciales"], s["omitidas"], s["sin_registro"],
                           s["adherencia_observada"], s["senal"], s["decision_backend"], s["decision_oraculo"],
                           s["coinciden"], s["estado_v32"]])
            for x in [y for y in cic[R.etiq]["ciclos"] if y["id"] == pid]:
                ws.append(["Adaptación IA", f"C{x['ciclo']}", x["intervenciones"], x["clasificacion"], x["intentos"][:300],
                           x["cambios"][:300], x["pertinencia"]])
            for s in [x for x in prog if x["corrida"] == R.etiq and x["id"] == pid]:
                ws.append(["Paso", f"C{s['ciclo']}", s["tipo"], s["exercise_id"], f"vol {s['volumen_antes']}→{s['volumen_despues']}",
                           f"techo {s['techo_oraculo']}"])
            m = R.mat.get(pid, {})
            ws.append(["Resultado 10 aspectos"] + [f"{a.split('_')[0]} {m.get(a, '-')}" for a in ASP])
            ws.append(["Revisión de catálogo", m.get("revision_catalogo", "-")[:500]])
        ws.column_dimensions["A"].width = 22
        ws.column_dimensions["E"].width = 40
    planes = []
    for R in runs:
        for pid in perfiles:
            planes.extend(R.plan_inicial(pid)[1])
    hoja("Planes iniciales", planes)
    hoja("Generacion", gen)
    hoja("Adaptacion", ada)
    hoja("Adherencia", adh)
    hoja("Progresion", prog)
    hoja("Matriz 10 aspectos", mat)
    hoja("Reproducibilidad", rep)
    hoja("Predicciones", pc)
    hoja("Solicitudes y respuestas", sr)
    wb.save(os.path.join(salida, "EVALUACION_20_PERFILES_IA_REAL.xlsx"))
    for t in (t_gen, t_ada, t_mat, t_cost):
        print(md(t), "\n")


if __name__ == "__main__":
    main()
