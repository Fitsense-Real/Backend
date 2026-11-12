"""Caso 39 · compara el plan 78 REAL (JSON de la API) con plan78_respaldo_reproducido.csv, ejercicio por ejercicio.

Uso (desde esta carpeta):
    python3 -I -B comparar_plan78.py plan78_real.json [plan78_respaldo_reproducido.csv]

plan78_real.json: la respuesta de GET /api/v1/users/me/plan/current (o el JSON del plan 78 que tengas), con
workouts[].scheduledDate/focusCode/status/expectedDurationMinutes y workouts[].exercises[].exerciseId/
exerciseOrder/prescriptionType/plannedSets/plannedReps/plannedDurationSeconds/restSeconds.
Acepta tambien snake_case (scheduled_date, exercise_id...). No modifica nada.
Sale con codigo 0 si todo coincide y 1 si hay diferencias; imprime una tabla de comparacion.
"""
import csv
import json
import sys


def g(d, *claves):
    for c in claves:
        if c in d and d[c] is not None:
            return d[c]
    return None


def real(ruta):
    plan = json.load(open(ruta, encoding="utf-8"))
    if "plan" in plan and isinstance(plan["plan"], dict):
        plan = plan["plan"]
    filas = []
    for w in g(plan, "workouts") or []:
        fecha = str(g(w, "scheduledDate", "scheduled_date"))[:10]
        for e in g(w, "exercises") or []:
            filas.append({
                "fecha": fecha, "enfoque": g(w, "focusCode", "focus_code"), "estado": g(w, "status"),
                "minutos": g(w, "expectedDurationMinutes", "expected_duration_minutes"),
                "orden": g(e, "exerciseOrder", "exercise_order"), "id": g(e, "exerciseId", "exercise_id"),
                "tipo": g(e, "prescriptionType", "prescription_type"), "series": g(e, "plannedSets", "planned_sets"),
                "reps": g(e, "plannedReps", "planned_reps"),
                "segundos": g(e, "plannedDurationSeconds", "planned_duration_seconds"),
                "descanso": g(e, "restSeconds", "rest_seconds")})
    return plan, filas


def reproducido(ruta):
    filas = []
    for x in csv.DictReader(open(ruta, encoding="utf-8"), delimiter=";"):
        n = lambda v: int(v) if v not in ("", None) else None
        filas.append({"fecha": x["scheduled_date"], "enfoque": x["focus_code"], "estado": x["workout_status"],
                      "minutos": n(x["expected_duration_minutes"]), "orden": n(x["exercise_order"]),
                      "id": n(x["exercise_id"]), "tipo": x["prescription_type"], "series": n(x["planned_sets"]),
                      "reps": n(x["planned_reps"]), "segundos": n(x["planned_duration_seconds"]),
                      "descanso": n(x["rest_seconds"])})
    return filas


def main():
    plan, r = real(sys.argv[1])
    p = reproducido(sys.argv[2] if len(sys.argv) > 2 else "plan78_respaldo_reproducido.csv")
    clave = lambda f: (f["fecha"], f["orden"])
    R, P = {clave(f): f for f in r}, {clave(f): f for f in p}
    difs = 0
    print(f"plan real: id={g(plan, 'planId', 'plan_id')} origen={g(plan, 'generationSource', 'generation_source')} "
          f"estado={g(plan, 'status')} · {len(r)} ejercicios reales, {len(p)} reproducidos")
    print("fecha;orden;campo;real;reproducido")
    for k in sorted(set(R) | set(P), key=lambda x: (x[0], x[1] or 0)):
        a, b = R.get(k), P.get(k)
        if a is None or b is None:
            difs += 1
            print(f"{k[0]};{k[1]};FALTA;{'-' if a is None else a['id']};{'-' if b is None else b['id']}")
            continue
        for campo in ("enfoque", "minutos", "id", "tipo", "series", "reps", "segundos", "descanso"):
            if a[campo] != b[campo]:
                difs += 1
                print(f"{k[0]};{k[1]};{campo};{a[campo]};{b[campo]}")
    estados = {(f["fecha"], f["estado"]) for f in r}
    print("estados reales por sesion:", sorted(estados), "(la reproduccion los crea todos SCHEDULED)")
    print("RESULTADO:", "COINCIDE ejercicio por ejercicio" if difs == 0 else f"{difs} diferencias")
    sys.exit(0 if difs == 0 else 1)


if __name__ == "__main__":
    main()
