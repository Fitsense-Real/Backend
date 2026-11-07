#!/usr/bin/env python3
"""Pruebas negativas del oraculo de adaptacion (contar_baseline.py), sin Replicate. A partir de las adaptaciones
PUBLICADAS por GRA-R1 (0 cambios no autorizados), se fabrican propuestas con UN incumplimiento conocido y se comprueba
que el oraculo cuenta exactamente esa categoria. Uso: pruebas_contar_baseline.py <B-R1> <GRA_G> <GRA_P> <tablas> <disp> <perfiles> <tmp>"""
import csv, json, os, subprocess, sys
bdir, gg, gp, tablas, disp, perf, tmp = sys.argv[1:8]
csv.field_size_limit(sys.maxsize)
L = lambda d, n: list(csv.DictReader(open(os.path.join(d, "tablas", n + ".csv"), encoding="utf-8")))
ctx = json.load(open(os.path.join(bdir, "contexto_adaptacion.json")))
dirs = {"GRA_R1_G": gg, "GRA_R1_P": gp}


def plan_publicado(c):
    d = dirs[c["gra_dir"]]
    pw, pwe = L(d, "planned_workouts"), L(d, "planned_workout_exercises")
    ws = sorted([w for w in pw if w["plan_id"] == c["resulting_plan_id"]], key=lambda w: w["scheduled_date"])
    out = []
    for w in ws:
        es = sorted([e for e in pwe if e["planned_workout_id"] == w["planned_workout_id"]], key=lambda e: int(e["exercise_order"]))
        out.append({"scheduled_date": w["scheduled_date"], "focus_code": w["focus_code"], "workout_name": "x",
                    "expected_duration_minutes": 30, "exercises": [
                        {"exercise_id": int(e["exercise_id"]), "exercise_order": int(e["exercise_order"]),
                         "prescription_type": e["prescription_type"], "planned_sets": int(e["planned_sets"] or 0) or None,
                         "planned_reps": int(e["planned_reps"]) if e["planned_reps"] else None,
                         "planned_duration_seconds": int(e["planned_duration_seconds"]) if e["planned_duration_seconds"] else None,
                         "target_load_kg": None, "rest_seconds": int(e["rest_seconds"]) if e["rest_seconds"] else None,
                         "notes": None} for e in es]})
    return out


def caso(nombre, etiqueta, mutar, esperado):
    c = next(x for x in ctx if x["etiqueta"] == etiqueta)
    ws = plan_publicado(c)
    mutar(ws, c)
    d = os.path.join(tmp, nombre); os.makedirs(d, exist_ok=True)
    for x in ctx:   # solo el ciclo del caso tiene respuesta
        f = os.path.join(d, x["etiqueta"].replace("/", "_") + ".json")
        if os.path.exists(f): os.remove(f)
    json.dump({"experimento": "B_ADAPTACION", "id": c["id"], "prediction_id": "prueba", "estado": "succeeded",
               "salida": json.dumps({"schema_version": "x", "plan_name": "x", "total_volume": 0, "rationale": "x", "workouts": ws})},
              open(os.path.join(d, etiqueta.replace("/", "_") + ".json"), "w"))
    out = os.path.join(d, "out")
    subprocess.run([sys.executable, "-I", os.path.join(os.path.dirname(os.path.abspath(__file__)), "contar_baseline.py"),
                    "adaptacion", bdir, gg, gp, tablas, disp, perf, "P", out], check=True, capture_output=True,
                   env={**os.environ, "RESPUESTAS_BASE": d})
    filas = [r for r in csv.DictReader(open(os.path.join(out, "cambios_P.csv"))) if r["origen"] == "BASE"]
    cats = sorted({r["categoria"] for r in filas})
    ok = cats == esperado
    print(("OK  " if ok else "FALLA"), nombre, "esperado", esperado, "obtenido", cats)
    return ok


def no_senalada(ws, c):   # cambia una sesion no senalada (O04: senalada 2026-11-03 -> semana sig. 11-10)
    w = next(w for w in ws if w["scheduled_date"] != "2026-11-10"); w["exercises"][0]["planned_reps"] = (w["exercises"][0]["planned_reps"] or 10) + 2


def sube_dosis(ws, c):    # O17 REVISAR_CARGA: sube repeticiones en la sesion senalada (11-06 -> 11-13)
    w = next(w for w in ws if w["scheduled_date"] == "2026-11-13"); w["exercises"][0]["planned_reps"] = (w["exercises"][0]["planned_reps"] or 10) + 40


def fuera_catalogo(ws, c):  # O06 dolor: sustituye por un id inexistente
    w = next(w for w in ws if w["scheduled_date"] == "2026-11-09"); w["exercises"][0]["exercise_id"] = 999999


def sesion_extra(ws, c):
    ws.append(dict(ws[0], scheduled_date="2026-11-15"))


r = [caso("no_senalada", "O04/ciclo1", no_senalada, ["C1"]),
     caso("sube_dosis", "O17/ciclo1", sube_dosis, ["C2", "C3"]),
     caso("fuera_catalogo", "O06/ciclo1", fuera_catalogo, ["C7"]),
     caso("identica", "O04/ciclo1", lambda ws, c: None, [])]
print("PRUEBAS", sum(r), "de", len(r))
sys.exit(0 if all(r) else 1)
