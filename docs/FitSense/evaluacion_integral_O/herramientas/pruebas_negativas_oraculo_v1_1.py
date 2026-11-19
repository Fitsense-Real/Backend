#!/usr/bin/env python3
"""Pruebas negativas (mutacion) del oraculo integral O v1.1 (correcciones D-OR-1 y D-OR-2 aprobadas).

Toma COPIAS de las salidas reales de una corrida (bloques G y P), introduce un defecto concreto en las tablas
exportadas y comprueba que el oraculo v1.1 lo RECHAZA (FALLO en el aspecto esperado y con el motivo esperado).
Tambien comprueba el control sin mutar (las mismas celdas APROBADO con v1.1) y que el oraculo v1.0 rechaza
igualmente cada mutacion (las correcciones no relajan esas validaciones).

Nunca modifica las carpetas de entrada: trabaja sobre copias en un directorio temporal.

Uso:
    python3 -I pruebas_negativas_oraculo_v1_1.py <O_PERFILES.csv> <corrida_G> <corrida_P> <salida_dir>
Sale con codigo 0 solo si todas las pruebas pasan.
"""
import csv
import json
import os
import shutil
import subprocess
import sys
import tempfile

csv.field_size_limit(sys.maxsize)
AQUI = os.path.dirname(os.path.abspath(__file__))
V10 = os.path.join(AQUI, "oraculo_integral_O.py")
V11 = os.path.join(AQUI, "oraculo_integral_O_v1_1.py")


# ------------------------------------------------------------------ utilidades sobre tablas exportadas
class Tablas:
    def __init__(self, carpeta):
        self.carpeta = carpeta
        self.t, self.cab = {}, {}
        for n in ("users", "weekly_training_plans", "planned_workouts", "planned_workout_exercises",
                  "plan_adaptation_attempts"):
            with open(os.path.join(carpeta, "tablas", n + ".csv"), newline="", encoding="utf-8") as f:
                r = csv.DictReader(f)
                self.cab[n] = r.fieldnames
                self.t[n] = list(r)
        with open(os.path.join(carpeta, "usuarios.csv"), newline="", encoding="utf-8") as f:
            self.usuarios = {r["id"]: r["user_id"] for r in csv.DictReader(f)}

    def guardar(self):
        for n, filas in self.t.items():
            with open(os.path.join(self.carpeta, "tablas", n + ".csv"), "w", newline="", encoding="utf-8") as f:
                w = csv.DictWriter(f, fieldnames=self.cab[n], lineterminator="\n")
                w.writeheader()
                w.writerows(filas)

    def planes_vigentes(self, pid):
        u = self.usuarios[pid]
        p = [x for x in self.t["weekly_training_plans"] if x["user_id"] == u and not x["replaced_at"]]
        return sorted(p, key=lambda x: x["week_start_date"])

    def sesiones(self, plan_id):
        s = [w for w in self.t["planned_workouts"] if w["plan_id"] == str(plan_id)]
        return sorted(s, key=lambda w: (w["scheduled_date"], int(w["display_order"] or 0), int(w["planned_workout_id"])))

    def ejercicios(self, wid):
        e = [x for x in self.t["planned_workout_exercises"] if x["planned_workout_id"] == str(wid)]
        return sorted(e, key=lambda x: int(x["exercise_order"]))

    def sucesora(self, wid):
        return next(w for w in self.t["planned_workouts"] if w["source_planned_workout_id"] == str(wid))

    def v32(self, pid, source_plan_id):
        u = self.usuarios[pid]
        return [r for r in self.t["plan_adaptation_attempts"]
                if r["user_id"] == u and r["source_plan_id"] == str(source_plan_id)][-1]

    def v32_con_origen(self, pid, origen):
        u = self.usuarios[pid]
        for r in self.t["plan_adaptation_attempts"]:
            if r["user_id"] != u or not r["conservation_report"]:
                continue
            inf = json.loads(r["conservation_report"])
            if any(int(p.get("origen") or -1) == origen for p in inf.get("pasos") or []):
                return r
        raise KeyError(origen)

    def nuevo_ejercicio(self, plantilla, wid, orden):
        fila = dict(plantilla)
        fila["planned_exercise_id"] = str(max(int(x["planned_exercise_id"]) for x in self.t["planned_workout_exercises"]) + 1)
        fila["planned_workout_id"] = str(wid)
        fila["exercise_order"] = str(orden)
        self.t["planned_workout_exercises"].append(fila)
        return fila


def editar_informe(r, fn):
    inf = json.loads(r["conservation_report"])
    fn(inf)
    r["conservation_report"] = json.dumps(inf)


def paso(pasos, origen):
    return next(p for p in pasos if int(p.get("origen") or -1) == origen)


# ------------------------------------------------------------------ mutaciones
# Cada mutacion: (codigo, descripcion, bloque a mutar, perfil, aspecto, fragmento esperado en el motivo, funcion)

def m_recuperacion_sobre_referencia(T):
    """O03: la RECUPERACION de 899 (C3, sesion 36) pasa a 4 series, por encima de su dosis de referencia (3)."""
    w = T.sucesora(36)
    e = next(x for x in T.ejercicios(w["planned_workout_id"]) if x["exercise_id"] == "899")
    e["planned_sets"] = "4"


def m_recuperacion_etiquetada_progresion(T):
    """O03: el paso de C3 se registra como PROGRESION con la progresion apagada (bloque G)."""
    r = T.v32_con_origen("O03", 36)
    editar_informe(r, lambda inf: paso(inf["pasos"], 36).__setitem__("tipo", "PROGRESION"))


def m_recuperacion_O14_sobre_referencia(T):
    """O14: la RECUPERACION de 253 (C4, sesion 218) pasa a 4 series (referencia 3)."""
    w = T.sucesora(218)
    e = next(x for x in T.ejercicios(w["planned_workout_id"]) if x["exercise_id"] == "253")
    e["planned_sets"] = "4"


def m_progresion_no_autorizada_G(T):
    """O01 (bloque G, progresion apagada, sin ningun paso real): se inyecta en C2 un paso PROGRESION (+1 rep)."""
    planes = T.planes_vigentes("O01")
    w2 = T.sesiones(planes[1]["plan_id"])[0]          # sesion de la semana 2 (origen del ciclo 2)
    w3 = T.sucesora(w2["planned_workout_id"])          # su copia en la semana 3
    e = T.ejercicios(w3["planned_workout_id"])[0]
    e["planned_reps"] = str(int(e["planned_reps"]) + 1)
    r = T.v32("O01", planes[1]["plan_id"])
    editar_informe(r, lambda inf: inf.setdefault("pasos", []).append(
        {"tipo": "PROGRESION", "origen": int(w2["planned_workout_id"]), "exercise_id": int(e["exercise_id"])}))


def m_progresion_sin_dos_semanas_P(T):
    """O15 (bloque P): paso PROGRESION en C1, sin dos semanas medidas."""
    planes = T.planes_vigentes("O15")
    w1 = T.sesiones(planes[0]["plan_id"])[0]
    w2 = T.sucesora(w1["planned_workout_id"])
    e = T.ejercicios(w2["planned_workout_id"])[0]
    e["planned_reps"] = str(int(e["planned_reps"]) + 1)
    r = T.v32("O15", planes[0]["plan_id"])
    editar_informe(r, lambda inf: inf.setdefault("pasos", []).append(
        {"tipo": "PROGRESION", "origen": int(w1["planned_workout_id"]), "exercise_id": int(e["exercise_id"])}))


def m_progresion_mas_dos_reps(T):
    """O15: la PROGRESION real de 342 en C2 (10 -> 11) pasa a 10 -> 12 (no es +1)."""
    for x in T.t["plan_adaptation_attempts"]:
        if x["user_id"] != T.usuarios["O15"] or not x["conservation_report"]:
            continue
        inf = json.loads(x["conservation_report"])
        p = next((p for p in inf.get("pasos") or [] if p.get("tipo") == "PROGRESION" and p.get("exercise_id") == 342), None)
        if p:
            w = T.sucesora(p["origen"])
            e = next(y for y in T.ejercicios(w["planned_workout_id"]) if y["exercise_id"] == "342")
            e["planned_reps"] = str(int(e["planned_reps"]) + 1)
            return
    raise KeyError("O15/342")


def _cadena(T, wid, n):
    """Sesion del mismo linaje n semanas despues de wid."""
    w = {"planned_workout_id": str(wid)}
    for _ in range(n):
        w = T.sucesora(w["planned_workout_id"])
    return w


def _retirada_O19(T):
    """(sesion de la semana 1 del linaje del retirado 853, dosis inicial de 853)."""
    w1 = T.sesiones(T.planes_vigentes("O19")[0]["plan_id"])
    for w in w1:
        if not any(e["exercise_id"] == "853" for e in T.ejercicios(w["planned_workout_id"])):
            continue
        s2 = T.sucesora(w["planned_workout_id"])
        if not any(e["exercise_id"] == "853" for e in T.ejercicios(s2["planned_workout_id"])):
            ini = next(e for e in T.ejercicios(w["planned_workout_id"]) if e["exercise_id"] == "853")
            return w, ini
    raise KeyError("O19 sin retirada de 853")


def m_reincorporacion_sin_progresion(T):
    """O19: las dos PROGRESION del linaje (1179) se re-etiquetan; la reincorporacion de C9 queda sin PROGRESION previa."""
    for x in T.t["plan_adaptation_attempts"]:
        if x["user_id"] != T.usuarios["O19"] or not x["conservation_report"]:
            continue
        inf = json.loads(x["conservation_report"])
        cambia = False
        for p in inf.get("pasos") or []:
            if p.get("tipo") == "PROGRESION" and p.get("exercise_id") == 1179:
                p["tipo"] = "RECUPERACION"
                cambia = True
        if cambia:
            x["conservation_report"] = json.dumps(inf)


def m_reaparicion_en_linaje(T):
    """O19: 853 reaparece en su linaje en la semana 3 (C2), al final y con su dosis, sin paso de REINCORPORACION."""
    w1, ini = _retirada_O19(T)
    w3 = _cadena(T, w1["planned_workout_id"], 2)
    T.nuevo_ejercicio(ini, w3["planned_workout_id"], len(T.ejercicios(w3["planned_workout_id"])) + 1)


def m_reincorporacion_dosis_distinta(T):
    """O19: en la reincorporacion de C9, 853 vuelve con 4 series en lugar de su dosis inicial (3)."""
    w1, ini = _retirada_O19(T)
    r = next(x for x in T.t["plan_adaptation_attempts"] if x["user_id"] == T.usuarios["O19"]
             and "REINCORPORACION" in (x["conservation_report"] or ""))
    origen = paso(json.loads(r["conservation_report"])["pasos"],
                  next(p["origen"] for p in json.loads(r["conservation_report"])["pasos"] if p["tipo"] == "REINCORPORACION"))["origen"]
    w = T.sucesora(origen)
    e = next(x for x in T.ejercicios(w["planned_workout_id"]) if x["exercise_id"] == "853")
    e["planned_sets"] = str(int(e["planned_sets"]) + 1)


def m_reincorporacion_no_al_final(T):
    """O19: en la reincorporacion de C9, 853 se coloca en la primera posicion de la sesion."""
    r = next(x for x in T.t["plan_adaptation_attempts"] if x["user_id"] == T.usuarios["O19"]
             and "REINCORPORACION" in (x["conservation_report"] or ""))
    origen = next(p["origen"] for p in json.loads(r["conservation_report"])["pasos"] if p["tipo"] == "REINCORPORACION")
    w = T.sucesora(origen)
    ej = T.ejercicios(w["planned_workout_id"])
    ultimo = ej[-1]
    assert ultimo["exercise_id"] == "853", ultimo
    for e in ej[:-1]:
        e["exercise_order"] = str(int(e["exercise_order"]) + 1)
    ultimo["exercise_order"] = "1"


def m_aparicion_en_otra_sesion(T):
    """O18: el retirado 1302 aparece en la semana 3 en una sesion de OTRO linaje que no lo tenia (sin orden ni paso)."""
    planes = T.planes_vigentes("O18")
    s3 = T.sesiones(planes[2]["plan_id"])
    ini = next(e for w in T.sesiones(planes[0]["plan_id"]) for e in T.ejercicios(w["planned_workout_id"])
               if e["exercise_id"] == "1302")
    destino = next(w for w in s3 if not any(e["exercise_id"] == "1302" for e in T.ejercicios(w["planned_workout_id"]))
                   and not any(e["exercise_id"] == "1302" for e in
                               T.ejercicios(next(x for x in T.t["planned_workouts"]
                                                 if x["planned_workout_id"] == w["source_planned_workout_id"])["planned_workout_id"])))
    T.nuevo_ejercicio(ini, destino["planned_workout_id"], len(T.ejercicios(destino["planned_workout_id"])) + 1)


def m_politica_no_rev2(T):
    """O03: con la RECUPERACION intacta pero la politica declarada PLAN-HIST-1, P-A1 no la ampara (D-OR-1 solo REV2)."""
    ruta = os.path.join(T.carpeta, "00_configuracion.txt")
    with open(ruta, encoding="utf-8") as f:
        txt = f.read()
    with open(ruta, "w", encoding="utf-8") as f:
        f.write(txt.replace("politica_por_defecto: PLAN-REV-2.0", "politica_por_defecto: PLAN-HIST-1"))


MUTACIONES = [
    ("N1", "D-OR-1", "RECUPERACION por encima de la dosis de referencia (O03)", "G", "O03", "A7_progresion",
     "RECUPERACION por encima de la referencia", m_recuperacion_sobre_referencia),
    ("N2", "D-OR-1", "RECUPERACION por encima de la dosis de referencia (O14)", "G", "O14", "A7_progresion",
     "RECUPERACION por encima de la referencia", m_recuperacion_O14_sobre_referencia),
    ("N3", "D-OR-1", "Paso PROGRESION con la progresion apagada (O03, bloque G)", "G", "O03", "A7_progresion",
     "paso de progresion con la progresion apagada", m_recuperacion_etiquetada_progresion),
    ("N4", "D-OR-1", "PROGRESION inyectada en un perfil sin pasos (O01, bloque G)", "G", "O01", "A7_progresion",
     "paso de progresion con la progresion apagada", m_progresion_no_autorizada_G),
    ("N5", "D-OR-1", "RECUPERACION con politica distinta de PLAN-REV-2.0 (O03)", "G", "O03", "A7_progresion",
     "paso de progresion con la progresion apagada", m_politica_no_rev2),
    ("N6", "control", "PROGRESION sin dos semanas medidas (O15, bloque P)", "P", "O15", "A7_progresion",
     "PROGRESION sin dos semanas medidas", m_progresion_sin_dos_semanas_P),
    ("N7", "control", "PROGRESION de +2 repeticiones (O15, bloque P)", "P", "O15", "A7_progresion",
     "no es +1 repeticion o +1 serie", m_progresion_mas_dos_reps),
    ("N8", "D-OR-2", "REINCORPORACION sin PROGRESION previa en el linaje (O19)", "P", "O19", "A8_reincorporacion",
     "sin un paso de PROGRESION posterior a la retirada", m_reincorporacion_sin_progresion),
    ("N9", "D-OR-2", "Reaparicion del retirado en su linaje sin REINCORPORACION (O19, S3)", "P", "O19",
     "A8_reincorporacion", "vuelve sin paso de REINCORPORACION", m_reaparicion_en_linaje),
    ("N10", "D-OR-2", "Reincorporado con dosis distinta de la inicial (O19)", "P", "O19", "A8_reincorporacion",
     "no con su dosis inicial", m_reincorporacion_dosis_distinta),
    ("N11", "D-OR-2", "Reincorporado fuera del final de la sesion (O19)", "P", "O19", "A8_reincorporacion",
     "no va al final de la sesion", m_reincorporacion_no_al_final),
    ("N12", "D-OR-2", "Retirado que aparece en una sesion de otro linaje (O18, S3)", "P", "O18", "A6_mantenimiento",
     "cambio sin orden ni paso registrado", m_aparicion_en_otra_sesion),
]

# Control sin mutar: valor esperado de cada celda (O01 no tiene pasos: A7 = NO APLICA).
CONTROL = [("O03", "A7_progresion", "APROBADO"), ("O14", "A7_progresion", "APROBADO"),
           ("O01", "A7_progresion", "NO APLICA"), ("O15", "A7_progresion", "APROBADO"),
           ("O19", "A8_reincorporacion", "APROBADO"), ("O18", "A6_mantenimiento", "APROBADO"),
           ("O18", "A8_reincorporacion", "APROBADO")]

# Celdas de OTROS perfiles que la mutacion afecta legitimamente (N5 cambia la politica de todo el bloque G,
# y O14 tambien tiene una RECUPERACION: debe pasar igualmente a FALLO).
OTROS_ESPERADOS = {"N5": {("O14", "A7_progresion"): "FALLO"}}


def correr(oraculo, perfiles, g, p, salida):
    os.makedirs(salida, exist_ok=True)
    cp = subprocess.run([sys.executable, "-I", oraculo, perfiles, salida, g, p], capture_output=True, text=True)
    if cp.returncode != 0:
        raise RuntimeError(f"{oraculo} fallo: {cp.stderr[-2000:]}")
    with open(os.path.join(salida, "detalle.json"), encoding="utf-8") as f:
        return {d["id"]: d for d in json.load(f)}


def main():
    perfiles, g, p, salida = sys.argv[1:5]
    os.makedirs(salida, exist_ok=True)
    filas, ok = [], True
    with tempfile.TemporaryDirectory() as tmp:
        cg, cpp = os.path.join(tmp, "G"), os.path.join(tmp, "P")
        shutil.copytree(g, cg)
        shutil.copytree(p, cpp)
        base = correr(V11, perfiles, cg, cpp, os.path.join(tmp, "control_v11"))
        for pid, asp, esp in CONTROL:
            est = base[pid]["estados"][asp]
            bien = est == esp
            ok &= bien
            filas.append({"prueba": "C-" + pid + "-" + asp, "correccion": "control", "descripcion":
                          "Salida real sin mutar", "perfil": pid, "aspecto": asp, "esperado": esp,
                          "v1_1": est, "motivo_v1_1": "-", "v1_0": "-", "resultado": "PASA" if bien else "NO PASA"})
        for cod, corr, desc, bloque, pid, asp, frag, fn in MUTACIONES:
            mg, mp = os.path.join(tmp, cod, "G"), os.path.join(tmp, cod, "P")
            shutil.copytree(g, mg)
            shutil.copytree(p, mp)
            T = Tablas(mg if bloque == "G" else mp)
            fn(T)
            T.guardar()
            r11 = correr(V11, perfiles, mg, mp, os.path.join(tmp, cod, "v11"))
            r10 = correr(V10, perfiles, mg, mp, os.path.join(tmp, cod, "v10"))
            e11, e10 = r11[pid]["estados"][asp], r10[pid]["estados"][asp]
            motivos = r11[pid]["checks"][asp]
            hallado = [m for m in motivos if frag in m]
            # Otras celdas del perfil o de otros perfiles no deben cambiar salvo en el perfil mutado
            esperados = OTROS_ESPERADOS.get(cod, {})
            otros = [(q, a) for q in r11 for a in r11[q]["estados"]
                     if q != pid and r11[q]["estados"][a] != base[q]["estados"][a]
                     and esperados.get((q, a)) != r11[q]["estados"][a]]
            otros += [(q, a) for (q, a), v in esperados.items() if r11[q]["estados"][a] != v]
            bien = e11 == "FALLO" and bool(hallado) and e10 == "FALLO" and not otros
            ok &= bien
            filas.append({"prueba": cod, "correccion": corr, "descripcion": desc, "perfil": pid, "aspecto": asp,
                          "esperado": "FALLO", "v1_1": e11, "motivo_v1_1": hallado[0] if hallado else " | ".join(motivos),
                          "v1_0": e10, "resultado": ("PASA" if bien else "NO PASA") +
                          (f" (cambian otros perfiles: {otros})" if otros else "")})
    with open(os.path.join(salida, "pruebas_negativas_v1_1.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(filas[0]), lineterminator="\n")
        w.writeheader()
        w.writerows(filas)
    for x in filas:
        print(f"{x['resultado']:8} {x['prueba']:24} {x['perfil']} {x['aspecto']:22} v1.1={x['v1_1']:10} v1.0={x['v1_0']:10} {x['motivo_v1_1'][:90]}")
    print("TODAS PASAN" if ok else "HAY PRUEBAS QUE NO PASAN")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
