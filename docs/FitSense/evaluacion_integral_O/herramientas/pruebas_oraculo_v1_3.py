#!/usr/bin/env python3
"""Pruebas del oraculo integral O v1.3 (D-OR-3, rama de dolor).

1. Las 19 pruebas negativas de v1.1 y las R-1 de v1.2, ejecutadas con v1.3 en su lugar.
2. D-OR-3, por mutacion de COPIAS de la corrida real R'_O (O12, ciclo 1: dolor de sesion, la IA real cambio los 8
   ejercicios por 6). v1.3 debe seguir rechazando ante dolor:
   D1  mas anadidos que retirados (se anaden 3 ejercicios mas: 9 > 8)            -> FALLO A5
   D2  trabajo que sube (un anadido pasa a 3000 s)                              -> FALLO A5
   D3  un anadido mas dificil que lo retirado (dificultad 3 > max 2)            -> FALLO A5
   D4  el ejercicio que dolio vuelve a la sesion                                -> FALLO (A5 o A10)
   y fuera del dolor nada cambia:
   D5  O04 (VARIANTE_MAS_SENCILLA_O_CARGA): el sustituto pasa a otro patron      -> FALLO A5 "otro patron"
   Control: la salida real sin mutar -> O12 A5 APROBADO con v1.3 (FALLO con v1.2).
Nunca modifica las carpetas de entrada.

Uso: python3 -I pruebas_oraculo_v1_3.py <O_PERFILES.csv> <salida> <S_O_G> <S_O_P> <Rp_O_G> <Rp_O_P>
"""
import csv
import importlib.util
import json
import os
import shutil
import subprocess
import sys
import tempfile

csv.field_size_limit(sys.maxsize)
AQUI = os.path.dirname(os.path.abspath(__file__))
V12 = os.path.join(AQUI, "oraculo_integral_O_v1_2.py")
V13 = os.path.join(AQUI, "oraculo_integral_O_v1_3.py")


def modulo(nombre):
    spec = importlib.util.spec_from_file_location(nombre[:-3], os.path.join(AQUI, nombre))
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    return m


PN = modulo("pruebas_negativas_oraculo_v1_1.py")
P12 = modulo("pruebas_oraculo_v1_2.py")


def correr(oraculo, perfiles, g, p, salida):
    os.makedirs(salida, exist_ok=True)
    cp = subprocess.run([sys.executable, "-I", oraculo, perfiles, salida, g, p], capture_output=True, text=True)
    if cp.returncode != 0:
        raise RuntimeError(cp.stderr[-2000:])
    with open(os.path.join(salida, "detalle.json"), encoding="utf-8") as f:
        return {d["id"]: d for d in json.load(f)}


def sesion_ia_O12(T):
    """(sesion medida de O12 con dolor de sesion, su derivada adaptada por la IA)."""
    w1 = T.sesiones(T.planes_vigentes("O12")[0]["plan_id"])
    for w in w1:
        n = T.sucesora(w["planned_workout_id"])
        a = {e["exercise_id"] for e in T.ejercicios(w["planned_workout_id"])}
        b = {e["exercise_id"] for e in T.ejercicios(n["planned_workout_id"])}
        if a and not (a & b):
            return w, n
    raise KeyError("O12 sin sesion rehecha por dolor")


def otro(T, condicion):
    ids = {e["exercise_id"] for e in T.t["planned_workout_exercises"]}
    for r in csv.DictReader(open(os.path.join(T.carpeta, "tablas", "exercises.csv"), newline="", encoding="utf-8")):
        if r["is_active"] == "true" and r["exercise_id"] not in ids and condicion(r):
            return r
    raise KeyError("sin ejercicio")


def d1(T):
    _, n = sesion_ia_O12(T)
    ej = T.ejercicios(n["planned_workout_id"])
    for i in range(3):
        nuevo = otro(T, lambda r: r["difficulty_level"] == "1" and r["exercise_id"] not in
                     {e["exercise_id"] for e in T.t["planned_workout_exercises"]})
        f = T.nuevo_ejercicio(ej[0], n["planned_workout_id"], len(ej) + 1 + i)
        f["exercise_id"] = nuevo["exercise_id"]


def d2(T):
    _, n = sesion_ia_O12(T)
    e = next(x for x in T.ejercicios(n["planned_workout_id"]) if x["prescription_type"] == "DURATION")
    e["planned_duration_seconds"] = "3000"


def d3(T):
    _, n = sesion_ia_O12(T)
    e = T.ejercicios(n["planned_workout_id"])[0]
    e["exercise_id"] = otro(T, lambda r: r["difficulty_level"] == "3")["exercise_id"]


def d4(T):
    w, n = sesion_ia_O12(T)
    viejo = T.ejercicios(w["planned_workout_id"])[0]
    T.nuevo_ejercicio(viejo, n["planned_workout_id"], len(T.ejercicios(n["planned_workout_id"])) + 1)


def d5(T):
    """O04 (S_O): el sustituto 128 de la variante mas sencilla pasa a un ejercicio de OTRO patron y misma dificultad."""
    pat = {r["exercise_id"]: (r["movement_pattern"], r["difficulty_level"]) for r in
           csv.DictReader(open(os.path.join(T.carpeta, "tablas", "exercises.csv"), newline="", encoding="utf-8"))}
    e = next(x for x in T.t["planned_workout_exercises"] if x["exercise_id"] == "128"
             and T.usuarios["O04"] == next(p["user_id"] for p in T.t["weekly_training_plans"]
                                           if p["plan_id"] == next(w["plan_id"] for w in T.t["planned_workouts"]
                                                                  if w["planned_workout_id"] == x["planned_workout_id"])))
    pa, di = pat["128"]
    e["exercise_id"] = otro(T, lambda r: r["movement_pattern"] not in (pa, "", None) and r["difficulty_level"] <= di
                           and r["movement_pattern"] != "LOCOMOTION_CARDIO")["exercise_id"]


CASOS = [("D1", "mas anadidos que retirados ante dolor", "Rp", "O12", ["A5_adaptacion_ia"], "ante dolor anade", d1),
         ("D2", "trabajo que sube ante dolor", "Rp", "O12", ["A5_adaptacion_ia"], "mas trabajo ante dolor", d2),
         ("D3", "anadido mas dificil que lo retirado", "Rp", "O12", ["A5_adaptacion_ia"], "mas dificil", d3),
         ("D4", "el ejercicio que dolio vuelve", "Rp", "O12", ["A5_adaptacion_ia", "A10_seguridad_integridad"], "", d4),
         ("D5", "fuera del dolor: sustituto de otro patron (O04)", "S", "O04", ["A5_adaptacion_ia"], "otro patron", d5)]


def main():
    perfiles, salida, sg, sp, rg, rp = sys.argv[1:7]
    os.makedirs(salida, exist_ok=True)
    filas, ok = [], True
    with tempfile.TemporaryDirectory() as tmp:
        # 1. pruebas anteriores con v1.3
        for etiqueta, mod, args in (("19 negativas de v1.1", PN, [perfiles, sg, sp, os.path.join(tmp, "pn")]),
                                    ("identidad + R-1 de v1.2", P12, [perfiles, os.path.join(tmp, "p12"), sg, sp])):
            if mod is PN:
                mod.V11 = V13
            else:
                mod.V12 = V13
            sys.argv = ["x"] + args
            try:
                mod.main()
                pasa = True
            except SystemExit as e:
                pasa = e.code == 0
            ok &= pasa
            filas.append({"prueba": etiqueta + " ejecutadas con v1.3", "esperado": "todas PASA",
                          "obtenido": "todas PASA" if pasa else "ALGUNA NO PASA", "resultado": "PASA" if pasa else "NO PASA"})
        # control
        c12 = correr(V12, perfiles, rg, rp, os.path.join(tmp, "c12"))
        c13 = correr(V13, perfiles, rg, rp, os.path.join(tmp, "c13"))
        e12, e13 = c12["O12"]["estados"]["A5_adaptacion_ia"], c13["O12"]["estados"]["A5_adaptacion_ia"]
        bien = e13 == "APROBADO" and e12 == "FALLO"
        ok &= bien
        filas.append({"prueba": "CONTROL O12 R'_O sin mutar", "esperado": "A5 APROBADO (v1.3), FALLO (v1.2)",
                      "obtenido": f"v1.3 {e13} · v1.2 {e12}", "resultado": "PASA" if bien else "NO PASA"})
        # 2. D-OR-3
        for cod, desc, base, pid, aspectos, frag, fn in CASOS:
            g, p = (rg, rp) if base == "Rp" else (sg, sp)
            mg, mp = os.path.join(tmp, cod, "G"), os.path.join(tmp, cod, "P")
            shutil.copytree(g, mg)
            shutil.copytree(p, mp)
            T = PN.Tablas(mg)
            fn(T)
            T.guardar()
            r13 = correr(V13, perfiles, mg, mp, os.path.join(tmp, cod, "v13"))
            ests = {a: r13[pid]["estados"][a] for a in aspectos}
            mot = " | ".join(m for a in aspectos for m in r13[pid]["checks"][a])
            bien = any(v == "FALLO" for v in ests.values()) and (not frag or frag in mot)
            ok &= bien
            filas.append({"prueba": f"{cod} {desc}", "esperado": f"{pid} FALLO en {'/'.join(aspectos)}",
                          "obtenido": f"{ests} [{mot[:200]}]", "resultado": "PASA" if bien else "NO PASA"})
    with open(os.path.join(salida, "pruebas_oraculo_v1_3.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(filas[0]), lineterminator="\n")
        w.writeheader()
        w.writerows(filas)
    for x in filas:
        print(f"{x['resultado']:8} {x['prueba'][:60]:60} {x['obtenido'][:150]}")
    print("TODAS PASAN" if ok else "HAY PRUEBAS QUE NO PASAN")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
