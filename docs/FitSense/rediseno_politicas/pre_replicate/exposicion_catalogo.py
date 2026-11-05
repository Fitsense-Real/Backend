#!/usr/bin/env python3
"""Descriptivo (sin umbral; pedido en la autorizacion del 05/11/2026): planes semanales PUBLICADOS de cada corrida que
contienen ejercicios con error de catalogo (21), los haya tocado o no la IA. La aceptacion tecnica de esos planes NO es
una validacion deportiva. Solo lectura.
Uso: python3 -I exposicion_catalogo.py <salida.csv> <corrida> [<corrida> ...]"""
import csv, collections, os, sys
sys.dont_write_bytecode = True
GRUPOS = {"775": [775], "973": [973], "1320": [1320], "580": [580],
          "aislados_rodilla": [327, 637, 915, 916, 1007, 1008, 1009, 1010, 1046, 1195, 1196, 1197, 1266],
          "otros_21": [1261, 1308, 1263, 1093, 1262, 1115, 853]}
TODOS = {i for v in GRUPOS.values() for i in v}


def leer(p):
    return list(csv.DictReader(open(p, encoding="utf-8")))


filas = []
for c in sys.argv[2:]:
    t = os.path.join(c, "tablas")
    planes = {r["plan_id"]: r for r in leer(f"{t}/weekly_training_plans.csv") if r["status"] in ("ACTIVE", "COMPLETED", "REPLACED")}
    wk = {r["planned_workout_id"]: r["plan_id"] for r in leer(f"{t}/planned_workouts.csv")}
    ej = collections.defaultdict(set)
    for r in leer(f"{t}/planned_workout_exercises.csv"):
        p = wk.get(r["planned_workout_id"])
        if p in planes: ej[p].add(int(r["exercise_id"]))
    total = len(planes)
    usuarios = {planes[p]["user_id"] for p in planes}
    fila = [os.path.basename(c.rstrip("/")), total, len(usuarios)]
    for g, ids in list(GRUPOS.items()) + [("cualquiera", sorted(TODOS))]:
        con = [p for p in planes if ej[p] & set(ids)]
        fila += [len(con), f"{len(con) / total:.1%}" if total else "", len({planes[p]['user_id'] for p in con})]
    filas.append(fila)
cab = ["corrida", "planes", "usuarios"]
for g in list(GRUPOS) + ["cualquiera"]:
    cab += [f"{g}_planes", f"{g}_pct", f"{g}_usuarios"]
with open(sys.argv[1], "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f, lineterminator="\n"); w.writerow(cab); w.writerows(filas)
for r in filas:
    print(" | ".join(f"{a}={b}" for a, b in zip(cab, r)))
