#!/usr/bin/env python3
"""Oraculo independiente de la evaluacion K20 (short paper IEEE).

Lee SOLO las tablas exportadas por EvaluacionK20IT (carpeta tablas/), los perfiles congelados y los ficheros de la
corrida. No importa ni llama a nada del backend: recalcula con sus propias funciones las reglas documentadas
(adherencia, decision, senalamiento, validez, integridad, seguridad, pertinencia, origen, continuidad) y compara.

Uso:
    python3 -I oraculo_k20.py <carpeta_de_la_corrida> <perfiles.csv> [<oraculo_teorico_historico.csv>]

Escribe en la carpeta de la corrida:
    oraculo/matriz_20.csv        una fila por usuario y fase (OK / FAIL / NA) y el estado final
    oraculo/detalle.json         todas las comprobaciones con su evidencia
    oraculo/sesiones.csv         una fila por sesion senalada (origen, cambio, pertinencia)
    oraculo/hash_normalizado.txt huella reproducible de la corrida (sin ids sustitutos ni marcas de tiempo)
    oraculo/resumen.md           el resumen
Las reglas y sus fuentes estan en ../03_ORACULO_INDEPENDIENTE.md.
"""
import csv
import hashlib
import json
import math
import os
import sys
from datetime import date, timedelta
from decimal import Decimal, ROUND_HALF_UP

csv.field_size_limit(sys.maxsize)

S1 = date(2026, 11, 2)
S2 = S1 + timedelta(days=7)
S3 = S2 + timedelta(days=7)
UMBRAL_COBERTURA = Decimal("50")  # fitsense.adaptation.min-registration-coverage-pct (protocolo)

# Motivo de omision -> intervencion (tabla del protocolo, TipoIntervencion.deMotivo documentado)
INTERVENCION = {
    "FATIGUE": "REVISAR_CARGA_Y_RECUPERACION",
    "TOO_DIFFICULT": "VARIANTE_MAS_SENCILLA_O_CARGA",
    "LACK_OF_MOTIVATION": "INVESTIGAR_CAMBIO_MINIMO",
    "LACK_OF_TIME": "ACORTAR_O_REORGANIZAR",
    "SCHEDULE_CHANGE": "REPROGRAMAR_DIAS",
    "PAIN_OR_DISCOMFORT": "SEGURIDAD_ANTE_DOLOR",
}
REQUIERE_IA = {"REVISAR_CARGA_Y_RECUPERACION", "VARIANTE_MAS_SENCILLA_O_CARGA", "INVESTIGAR_CAMBIO_MINIMO",
               "ACORTAR_O_REORGANIZAR", "REPROGRAMAR_DIAS", "SEGURIDAD_ANTE_DOLOR"}
MAX_DIFICULTAD = {"BEGINNER": 1, "INTERMEDIATE": 2, "ADVANCED": 3}

FRASES_CAMBIO = ["usamos una variante registrada como de menor dificultad", "bajamos un poco el trabajo",
                 "Cambiamos ", "Por seguridad, cambiamos o retiramos", "Retiramos ", "Movimos "]
FRASES_SIN_CAMBIO = ["no aplicamos cambios", "no encontramos un cambio compatible"]


# ===================================================================== utilidades puras (probadas en test_oraculo)

def r2(x):
    """Redondeo a 2 decimales HALF_UP (el de la base)."""
    return Decimal(x).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


def round_java(x):
    """Math.round de Java: floor(x + 0,5)."""
    return math.floor(x + 0.5)


def entero(v, defecto=None):
    if v is None or v == "":
        return defecto
    return int(Decimal(v))


def decimal(v):
    return None if v in (None, "") else Decimal(v)


def lista_pg(v):
    """'{21,22}' -> [21, 22]; '{A,B}' -> ['A','B']."""
    if v in (None, "", "{}"):
        return []
    out = []
    for x in v.strip("{}").split(","):
        x = x.strip().strip('"')
        out.append(int(x) if x.lstrip("-").isdigit() else x)
    return out


def segundos_estimados(ejercicios, p):
    """Calentamiento + suma(trabajo + (series-1) x descanso) + (n-1) x transicion (formula documentada)."""
    n = len(ejercicios)
    if n == 0:
        return 0
    total = p["warmup_minutes"] * 60 + (n - 1) * p["transition_seconds"]
    for e in ejercicios:
        series = e["sets"] if e["sets"] is not None else 1
        if e["type"] == "DURATION":
            trabajo = series * (e["dur"] or 0)
        else:
            trabajo = series * (e["reps"] or 0) * p["seconds_per_rep"]
        descanso = e["rest"] if e["rest"] is not None else p["default_rest_seconds"]
        total += trabajo + max(0, series - 1) * descanso
    return total


def minutos_estimados(ejercicios, p):
    s = segundos_estimados(ejercicios, p)
    return 0 if not ejercicios else max(1, round_java(s / 60.0))


def piso_v20(minutos, floor_pct):
    """ceil(minutos x 60 x floor / 100) en segundos."""
    return -(-(minutos * 60 * floor_pct) // 100)


def trabajo(e, p):
    series = e["sets"] if e["sets"] is not None else 1
    if e["type"] == "DURATION":
        return series * (e["dur"] or 0)
    return series * (e["reps"] or 0) * p["seconds_per_rep"]


def dosis_total(e):
    series = e["sets"] if e["sets"] is not None else 1
    return series * ((e["dur"] if e["type"] == "DURATION" else e["reps"]) or 0)


def pct_ejercicio(ejecutada, prescrita):
    if prescrita is None or prescrita <= 0:
        return Decimal("0")
    return r2(min(Decimal(100), Decimal(100) * Decimal(ejecutada or 0) / Decimal(prescrita)))


def equipo_cumple(grupos, disponibles):
    """Grupos distintos = AND; mismo grupo = OR; sin grupos = peso corporal (siempre)."""
    return all(any(eq in disponibles for eq in opciones) for opciones in grupos.values())


def decidir(sesiones, good=Decimal("80"), moderate=Decimal("50")):
    """Politica documentada (AdaptationOrderDecider, protocolo §2.8), sobre los valores REALES.

    sesiones: lista ordenada de dicts {id, registrada, pct, motivo, dolor}.
    Devuelve (decision, [(id, intervencion)], Aw, Aobs, C).
    """
    n = len(sesiones)
    reg = [s for s in sesiones if s["registrada"]]
    aw = r2(sum((s["pct"] for s in sesiones), Decimal(0)) / n) if n else None
    aobs = r2(sum((s["pct"] for s in reg), Decimal(0)) / len(reg)) if reg else None
    c = r2(Decimal(100) * len(reg) / n) if n else None
    dolor = [s for s in sesiones if s["dolor"]]
    if dolor:
        return "ADAPT_LOCALIZED", [(s["id"], "SEGURIDAD_ANTE_DOLOR") for s in dolor], aw, aobs, c
    if c is None or c < UMBRAL_COBERTURA or aobs is None:
        return "INSUFFICIENT_DATA", [], aw, aobs, c
    if aobs >= good:
        return "NO_CHANGE", [], aw, aobs, c
    problema = [s for s in reg if s["pct"] < good]
    if not problema:
        return "INSUFFICIENT_DATA", [], aw, aobs, c

    def intervencion(s):
        return INTERVENCION.get(s["motivo"], "CAUSA_NO_REGISTRADA") if s["motivo"] else "CAUSA_NO_REGISTRADA"

    if aobs < moderate:
        return "ADAPT_EXPANDED", [(s["id"], intervencion(s)) for s in problema], aw, aobs, c
    peor = min(problema, key=lambda s: s["pct"])
    return "ADAPT_LOCALIZED", [(peor["id"], intervencion(peor))], aw, aobs, c


def comparar_sesion(antes, despues, dificultad, p):
    """Diferencias entre una sesion medida y su derivada (mismo orden de ejercicios = mismo ejercicio)."""
    d = {"igual": False, "sustituciones": [], "reducciones": [], "aumentos": [], "retirados": [], "anadidos": [],
         "cambiados": 0}
    ids_a = [e["id"] for e in antes]
    ids_d = [e["id"] for e in despues]
    pares = []
    if len(antes) == len(despues):
        pares = list(zip(antes, despues))
    else:
        quedan = {e["id"]: e for e in despues}
        for e in antes:
            if e["id"] in quedan:
                pares.append((e, quedan.pop(e["id"])))
            else:
                d["retirados"].append(e["id"])
        d["anadidos"] = list(quedan)
    for a, b in pares:
        if a["id"] != b["id"]:
            d["sustituciones"].append({"de": a["id"], "a": b["id"], "dif_de": dificultad.get(a["id"]),
                                       "dif_a": dificultad.get(b["id"]),
                                       "dosis_no_mayor": dosis_total(b) <= dosis_total(a)
                                       and trabajo(b, p) <= trabajo(a, p)})
            d["cambiados"] += 1
            continue
        cambio = False
        for k in ("sets", "reps", "dur", "load"):
            va, vb = a[k], b[k]
            if va == vb:
                continue
            cambio = True
            if va is not None and vb is not None and vb < va:
                d["reducciones"].append((a["id"], k, str(va), str(vb)))
            else:
                d["aumentos"].append((a["id"], k, str(va), str(vb)))
        if a["type"] != b["type"] or a["rest"] != b["rest"]:
            cambio = True
            if (b["rest"] or 0) < (a["rest"] or 0):
                d["aumentos"].append((a["id"], "menos_descanso", str(a["rest"]), str(b["rest"])))
        if cambio:
            d["cambiados"] += 1
    d["cambiados"] += len(d["retirados"]) + len(d["anadidos"])
    d["igual"] = ids_a == ids_d and d["cambiados"] == 0
    d["trabajo_antes"] = sum(trabajo(e, p) for e in antes)
    d["trabajo_despues"] = sum(trabajo(e, p) for e in despues)
    return d


def pertinencia(intervencion, d, conservada_justificada):
    """Tabla del protocolo (03 §3): ACC, ALT, CONS-J o INC."""
    if d["igual"]:
        return "CONS-J" if conservada_justificada else "INC"
    mas_trabajo = d["trabajo_despues"] > d["trabajo_antes"] or bool(d["aumentos"]) or bool(d["anadidos"])
    mas_dificil = any(s["dif_a"] is not None and s["dif_de"] is not None and s["dif_a"] > s["dif_de"]
                      for s in d["sustituciones"])
    if intervencion == "VARIANTE_MAS_SENCILLA_O_CARGA":
        if mas_dificil or mas_trabajo:
            return "INC"
        if any(s["dif_a"] is not None and s["dif_de"] is not None and s["dif_a"] < s["dif_de"]
               for s in d["sustituciones"]) or d["reducciones"]:
            return "ACC"
        return "ALT"
    if intervencion == "REVISAR_CARGA_Y_RECUPERACION":
        if mas_trabajo or mas_dificil:
            return "INC"
        if d["reducciones"] and not d["sustituciones"]:
            return "ACC"
        return "ALT"
    if intervencion == "INVESTIGAR_CAMBIO_MINIMO":
        if mas_trabajo or mas_dificil or d["cambiados"] > 2:
            return "INC"
        return "ACC" if d["cambiados"] <= 1 else "ALT"
    if intervencion == "SEGURIDAD_ANTE_DOLOR":
        return "INC" if mas_trabajo or mas_dificil else "ACC"
    # CAUSA_NO_REGISTRADA / CONSERVAR: la politica no autoriza cambios
    return "INC"


# ===================================================================== carga

def leer(ruta):
    with open(ruta, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


class Datos:
    def __init__(self, carpeta):
        t = os.path.join(carpeta, "tablas")
        self.tab = {n[:-4]: leer(os.path.join(t, n)) for n in os.listdir(t) if n.endswith(".csv")}
        self.res = {r["id"]: r for r in leer(os.path.join(carpeta, "resultados_20.csv"))}
        self.usuarios = {r["id"]: int(r["user_id"]) for r in leer(os.path.join(carpeta, "usuarios.csv"))}
        ruta_int = os.path.join(carpeta, "intentos_ia.csv")
        self.intentos = leer(ruta_int) if os.path.exists(ruta_int) and os.path.getsize(ruta_int) > 1 else []
        ruta_f = os.path.join(carpeta, "factibilidad.csv")
        self.factibilidad = leer(ruta_f) if os.path.exists(ruta_f) and os.path.getsize(ruta_f) > 1 else []
        ruta_t = os.path.join(carpeta, "trazas_ia.jsonl")
        self.trazas = []
        if os.path.exists(ruta_t):
            with open(ruta_t, encoding="utf-8") as f:
                self.trazas = [json.loads(l) for l in f if l.strip()]
        cfg = [c for c in self.tab["calculation_configs"] if c["is_active"] == "true"]
        assert len(cfg) == 1, "una sola configuracion activa"
        par = json.loads(cfg[0]["params"])
        pr = par["prescription"]
        self.p = {"warmup_minutes": pr["warmup_minutes"], "seconds_per_rep": pr["seconds_per_rep"],
                  "transition_seconds": pr["transition_seconds"], "default_rest_seconds": pr["default_rest_seconds"],
                  "floor_pct": pr["session_minutes_floor_pct"], "tolerance_pct": pr["duration_tolerance_pct"],
                  "max_rest": pr["max_rest_seconds"]}
        aj = par["adjustment"]
        self.good = Decimal(str(aj["good_threshold_pct"]))
        self.moderate = Decimal(str(aj["moderate_threshold_pct"]))
        self.cfg_version = cfg[0]["version"]
        eq = {r["equipment_id"]: r["code"] for r in self.tab["equipment_types"]}
        self.ejercicio = {int(e["exercise_id"]): e for e in self.tab["exercises"]}
        self.dificultad = {k: entero(v["difficulty_level"]) for k, v in self.ejercicio.items()}
        self.grupos = {}
        for r in self.tab["exercise_equipment_requirements"]:
            g = self.grupos.setdefault(int(r["exercise_id"]), {})
            g.setdefault(r["group_no"], set()).add(eq[r["equipment_id"]])
        self.planes = self.tab["weekly_training_plans"]
        self.pw = {int(r["planned_workout_id"]): r for r in self.tab["planned_workouts"]}
        self.pwe = {}
        for r in self.tab["planned_workout_exercises"]:
            self.pwe.setdefault(int(r["planned_workout_id"]), []).append(r)
        for v in self.pwe.values():
            v.sort(key=lambda r: int(r["exercise_order"]))
        self.ses = {}
        for r in self.tab["workout_sessions"]:
            self.ses.setdefault(int(r["planned_workout_id"]), []).append(r)
        self.wse = {}
        for r in self.tab["workout_session_exercises"]:
            self.wse.setdefault(int(r["session_id"]), {})[int(r["planned_exercise_id"])] = r
        self.metricas = {(int(r["user_id"]), r["week_start_date"]): r for r in self.tab["weekly_user_metrics"]}
        self.v32 = {}
        for r in self.tab["plan_adaptation_attempts"]:
            self.v32.setdefault(int(r["user_id"]), []).append(r)
        self.perfil = {int(r["user_id"]): r for r in self.tab["user_profiles"]}
        self.exclusiones = self.tab.get("user_safety_exclusions", [])

    def intentos_de(self, etiqueta):
        """Propuestas de la IA juzgadas (intento >= 1) en ese ciclo."""
        return [i for i in self.intentos if i["etiqueta"] == etiqueta and int(i["intento"] or 0) >= 1]

    def plan(self, u, semana):
        c = [p for p in self.planes if int(p["user_id"]) == u and p["week_start_date"] == semana.isoformat()
             and not p["replaced_at"]]
        return c

    def sesiones(self, plan_id):
        return sorted([w for w in self.pw.values() if int(w["plan_id"]) == plan_id], key=lambda w: w["scheduled_date"])

    def ejercicios(self, w):
        return [{"id": int(e["exercise_id"]), "type": e["prescription_type"], "sets": entero(e["planned_sets"]),
                 "reps": entero(e["planned_reps"]), "dur": entero(e["planned_duration_seconds"]),
                 "load": decimal(e["target_load_kg"]), "rest": entero(e["rest_seconds"]),
                 "pe": int(e["planned_exercise_id"])} for e in self.pwe.get(int(w["planned_workout_id"]), [])]


# ===================================================================== comprobaciones

def validar_plan(D, plan, perfil, nombre):
    """Reglas re-verificadas por el oraculo (03_ORACULO §3). Devuelve la lista de incumplimientos."""
    problemas = []
    sesiones = D.sesiones(int(plan["plan_id"]))
    dias = set(lista_pg(perfil["available_days"]))
    minutos = int(perfil["session_minutes"])
    equipo = set(lista_pg(perfil["equipment_codes"]))
    nivel = perfil["fitness_level"]
    inicio = date.fromisoformat(plan["week_start_date"])
    if len(sesiones) != int(perfil["days_per_week"]):
        problemas.append(f"{nombre}: {len(sesiones)} sesiones y el perfil pide {perfil['days_per_week']}")
    fechas = [w["scheduled_date"] for w in sesiones]
    if len(set(fechas)) != len(fechas):
        problemas.append(f"{nombre}: dos sesiones el mismo dia")
    for w in sesiones:
        f = date.fromisoformat(w["scheduled_date"])
        if not (inicio <= f <= inicio + timedelta(days=6)):
            problemas.append(f"{nombre} {f}: fuera de la semana")
        if f.isoweekday() not in dias:
            problemas.append(f"{nombre} {f}: dia {f.isoweekday()} no disponible {sorted(dias)}")
        ej = D.ejercicios(w)
        if len(ej) < 2:
            problemas.append(f"{nombre} {f}: V5 {len(ej)} ejercicios")
        ids = [e["id"] for e in ej]
        if len(set(ids)) != len(ids):
            problemas.append(f"{nombre} {f}: ejercicio repetido")
        for e in ej:
            c = D.ejercicio.get(e["id"])
            if c is None or c["is_active"] != "true":
                problemas.append(f"{nombre} {f}: V1 ejercicio {e['id']} inactivo o inexistente")
                continue
            if (D.dificultad[e["id"]] or 0) > MAX_DIFICULTAD.get(nivel, 2):
                problemas.append(f"{nombre} {f}: V6 ejercicio {e['id']} dificultad {D.dificultad[e['id']]} > "
                                 f"{MAX_DIFICULTAD.get(nivel)}")
            if not equipo_cumple(D.grupos.get(e["id"], {}), equipo):
                problemas.append(f"{nombre} {f}: equipamiento de {e['id']} no disponible")
            if c["default_prescription"] and c["default_prescription"] != e["type"]:
                problemas.append(f"{nombre} {f}: V7 tipo {e['type']} != catalogo {c['default_prescription']}")
            if e["rest"] is not None and e["rest"] > D.p["max_rest"]:
                problemas.append(f"{nombre} {f}: V21 descanso {e['rest']}")
        seg = segundos_estimados(ej, D.p)
        exento = bool(w.get("lack_of_time_context")) or bool(w.get("declared_available_minutes"))
        if not exento and seg < piso_v20(minutos, D.p["floor_pct"]):
            problemas.append(f"{nombre} {f}: V20 {seg} s < {piso_v20(minutos, D.p['floor_pct'])} s")
        declarados = int(w["expected_duration_minutes"])
        if declarados > round_java(minutos * 1.15):
            problemas.append(f"{nombre} {f}: V4 {declarados} min > {round_java(minutos * 1.15)}")
        est = minutos_estimados(ej, D.p)
        if est > 0 and abs(declarados - est) * 100.0 / est > D.p["tolerance_pct"]:
            problemas.append(f"{nombre} {f}: V17 declara {declarados} y el contenido dura {est}")
    return problemas


def ejecucion_esperada(paso, total):
    if paso == "H":
        return total
    if paso == "P40":
        return round_java(total * 0.4)
    return None


def comprobar_registro(D, sesiones, pasos, motivos, nombre):
    problemas, estado = [], []
    for i, w in enumerate(sesiones):
        paso = pasos[i] if i < len(pasos) else "N"
        wid = int(w["planned_workout_id"])
        intentos = [s for s in D.ses.get(wid, []) if s["counts_toward_adherence"] == "true"]
        if paso in ("H", "P40"):
            if len(intentos) != 1:
                problemas.append(f"{nombre} S{i + 1}: {len(intentos)} sesiones que cuentan (esperada 1)")
                estado.append(None)
                continue
            filas = D.wse.get(int(intentos[0]["session_id"]), {})
            for e in D.ejercicios(w):
                esperado = ejecucion_esperada(paso, dosis_total(e))
                fila = filas.get(e["pe"])
                real = None if fila is None else entero(fila["actual_duration_seconds"] if e["type"] == "DURATION"
                                                        else fila["actual_reps_total"])
                if real != esperado:
                    problemas.append(f"{nombre} S{i + 1} ej {e['id']}: registrado {real}, esperado {esperado}")
            estado.append("REGISTRADA")
        elif paso == "O":
            if w["status"] != "SKIPPED" or w["skip_reason"] != motivos[i]:
                problemas.append(f"{nombre} S{i + 1}: omision {w['status']}/{w['skip_reason']} != SKIPPED/{motivos[i]}")
            if intentos:
                problemas.append(f"{nombre} S{i + 1}: omitida con sesion que cuenta")
            estado.append("OMITIDA")
        elif paso == "N":
            if intentos or (w["status"] == "SKIPPED" and w["skip_reason"]):
                problemas.append(f"{nombre} S{i + 1}: sin registro esperado y hay registro")
            estado.append("SIN_REGISTRO")
    return problemas, estado


def adherencia_sesiones(D, sesiones):
    out = []
    for w in sesiones:
        wid = int(w["planned_workout_id"])
        intentos = [s for s in D.ses.get(wid, []) if s["counts_toward_adherence"] == "true"]
        dolor = w["skip_reason"] == "PAIN_OR_DISCOMFORT"
        if intentos:
            filas = D.wse.get(int(intentos[-1]["session_id"]), {})
            pcts = []
            for e in D.ejercicios(w):
                fila = filas.get(e["pe"])
                if fila is not None and fila.get("skip_reason") == "PAIN_OR_DISCOMFORT":
                    dolor = True
                real = None if fila is None else (fila["actual_duration_seconds"] if e["type"] == "DURATION"
                                                  else fila["actual_reps_total"])
                pcts.append(pct_ejercicio(entero(real, 0), dosis_total(e)))
            pct = r2(min(Decimal(100), sum(pcts, Decimal(0)) / len(pcts))) if pcts else Decimal(0)
            out.append({"id": wid, "registrada": True, "pct": pct, "motivo": None, "dolor": dolor})
        elif w["status"] == "SKIPPED" and w["skip_reason"]:
            out.append({"id": wid, "registrada": True, "pct": Decimal(0), "motivo": w["skip_reason"], "dolor": dolor})
        else:
            out.append({"id": wid, "registrada": False, "pct": Decimal(0), "motivo": None, "dolor": False})
    return out


def fase(problemas):
    return "OK" if not problemas else "FAIL"


def evaluar_usuario(D, perfil_csv, teorico):
    uid = perfil_csv["id"]
    det = {"id": uid, "comprobaciones": {}}
    ch = det["comprobaciones"]
    if uid not in D.usuarios:
        det["estado"] = "NO_EVALUADO"
        return det, []
    u = D.usuarios[uid]
    res = D.res.get(uid, {})
    perfil = D.perfil.get(u)
    filas_sesion = []

    # ---------------------------------------------------------------- A. perfil y generacion
    pa = []
    if perfil is None:
        pa.append("sin perfil")
    else:
        if perfil["goal_type"] != perfil_csv["goal_type"] or perfil["fitness_level"] != perfil_csv["nivel"] \
                or perfil["training_location"] != perfil_csv["ubicacion"] \
                or int(perfil["session_minutes"]) != int(perfil_csv["minutos_sesion"]) \
                or int(perfil["days_per_week"]) != int(perfil_csv["dias_por_semana"]) \
                or set(lista_pg(perfil["available_days"])) != {int(x) for x in perfil_csv["dias_disponibles"].split(" ")[0].split("|")} \
                or set(lista_pg(perfil["equipment_codes"])) != set(perfil_csv["equipamiento"].split("|")):
            pa.append("el perfil almacenado no coincide con el congelado")
    p1 = D.plan(u, S1)
    if len(p1) != 1:
        pa.append(f"{len(p1)} planes vigentes en S1")
        det["estado"] = "FAIL"
        ch["A_generacion"] = pa
        return det, []
    pa += validar_plan(D, p1[0], perfil, "S1")
    ch["A_generacion"] = pa
    s1 = D.sesiones(int(p1[0]["plan_id"]))

    # ---------------------------------------------------------------- B. registro
    pasos = perfil_csv["secuencia"].split("/")
    motivos = ["", perfil_csv["motivo_omision_s2"], perfil_csv["motivo_omision_s3"]]
    pb, _ = comprobar_registro(D, s1, pasos, motivos, "S1")
    ch["B_registro"] = pb

    # ---------------------------------------------------------------- C. adherencia
    pc = []
    ads = adherencia_sesiones(D, s1)
    dec, senal, aw, aobs, c = decidir(ads, D.good, D.moderate)
    m = D.metricas.get((u, S1.isoformat()))
    det["adherencia"] = {"Aw": str(aw), "Aobs": str(aobs), "C": str(c),
                         "por_sesion": [str(a["pct"]) if a["registrada"] else "sin_registro" for a in ads]}
    if m is None:
        pc.append("sin fila de metricas S1")
    else:
        for nombre, mio, col in (("Aw", aw, "weighted_adherence_pct"), ("Aobs", aobs, "observed_adherence_pct"),
                                 ("C", c, "registration_coverage_pct")):
            suyo = decimal(m[col])
            if (mio is None) != (suyo is None) or (mio is not None and abs(mio - suyo) > Decimal("0.01")):
                pc.append(f"{nombre}: backend {suyo} oraculo {mio}")
    ch["C_adherencia"] = pc

    # ---------------------------------------------------------------- D. decision
    pd = []
    v32 = sorted(D.v32.get(u, []), key=lambda r: int(r["attempt_id"]))
    v1 = [r for r in v32 if r["new_week_start_date"] == S2.isoformat()]
    if len(v1) != 1:
        pd.append(f"{len(v1)} filas V32 para S2")
        ch["D_decision"] = pd
        det["estado"] = "FAIL"
        return det, []
    v1 = v1[0]
    if v1["decision"] != dec:
        pd.append(f"decision backend {v1['decision']} oraculo {dec}")
    sen_b = lista_pg(v1["targeted_sessions"])
    int_b = lista_pg(v1["targeted_interventions"])
    if sorted(sen_b) != sorted(i for i, _ in senal):
        pd.append(f"senaladas backend {sen_b} oraculo {[i for i, _ in senal]}")
    elif dict(zip(sen_b, int_b)) != dict(senal):
        pd.append(f"intervenciones backend {dict(zip(sen_b, int_b))} oraculo {dict(senal)}")
    ch["D_decision"] = pd
    det["decision"] = {"oraculo_politica": dec, "backend": v1["decision"],
                       "senaladas": [f"S{[int(w['planned_workout_id']) for w in s1].index(i) + 1}:{iv}" for i, iv in senal],
                       "teorico_articulo": teorico.get("decision_esperada_backend") if teorico else None,
                       "coincide_teorico": (teorico.get("decision_esperada_backend") == dec) if teorico else None}

    # ---------------------------------------------------------------- E/F. adaptacion y publicacion (S1 -> S2)
    pe, pf = [], []
    p2 = D.plan(u, S2)
    estado_v32 = v1["status"]
    det["v32"] = {"status": estado_v32, "source": v1["generation_source"], "ai_calls": v1["ai_calls"],
                  "applied": lista_pg(v1["applied_sessions"]), "no_effect": lista_pg(v1["no_effect_sessions"]),
                  "conserved": lista_pg(v1["conserved_sessions"]), "reason_codes": v1["reason_codes"]}
    if not p2:
        if estado_v32 == "NO_FACTIBLE":
            det["estado"] = "NO_FACTIBLE"
            ch["E_adaptacion"] = ["NO_FACTIBLE: " + (v1["rejection_reason"] or "")]
            return det, []
        pf.append("sin plan S2 y V32 no es NO_FACTIBLE")
        ch["F_publicacion"] = pf
        det["estado"] = "FAIL"
        return det, []
    if len(p2) != 1:
        pf.append(f"{len(p2)} planes vigentes para S2 (duplicados)")
    p2 = p2[0]
    if v1["resulting_plan_id"] and int(v1["resulting_plan_id"]) != int(p2["plan_id"]):
        pf.append("V32 resulting_plan_id no es el plan publicado")
    pf += validar_plan(D, p2, perfil, "S2")
    s2 = D.sesiones(int(p2["plan_id"]))
    por_origen = {int(w["source_planned_workout_id"]): w for w in s2 if w["source_planned_workout_id"]}
    regenerado = "REGENERACION_SEGURA" in (v1["conservation_report"] or "")
    if not regenerado and len(por_origen) != len(s1):
        pf.append(f"linaje {len(por_origen)}/{len(s1)}")
    # idempotencia
    if str(res.get("ciclo1_repetido_plan")) != str(p2["plan_id"]) or str(res.get("ciclo1_repetido_v32_nuevas")) != "0":
        pf.append(f"idempotencia: repetido {res.get('ciclo1_repetido_plan')} nuevas V32 {res.get('ciclo1_repetido_v32_nuevas')}")
    senaladas = dict(senal)
    aplicadas = set(lista_pg(v1["applied_sessions"]))
    hubo_cambio_ia = False
    n_cambiadas = n_iguales = 0
    for i, w in enumerate(s1):
        wid = int(w["planned_workout_id"])
        d2 = por_origen.get(wid)
        if d2 is None:
            if not regenerado:
                pe.append(f"S{i + 1}: sin derivada")
            continue
        antes, despues = D.ejercicios(w), D.ejercicios(d2)
        cmp_ = comparar_sesion(antes, despues, D.dificultad, D.p)
        fecha_ok = date.fromisoformat(d2["scheduled_date"]) == date.fromisoformat(w["scheduled_date"]) + timedelta(days=7)
        if wid not in senaladas:
            if not cmp_["igual"] or not fecha_ok or d2["expected_duration_minutes"] != w["expected_duration_minutes"]:
                pe.append(f"S{i + 1} NO senalada cambio (S3 del protocolo)")
            continue
        iv = senaladas[wid]
        if not fecha_ok and iv != "REPROGRAMAR_DIAS":
            pe.append(f"S{i + 1}: cambio de fecha no autorizado")
        # seguridad
        if cmp_["aumentos"] or cmp_["anadidos"] or cmp_["trabajo_despues"] > cmp_["trabajo_antes"]:
            pe.append(f"S{i + 1}: SEGURIDAD S2 aumento {cmp_['aumentos']} anadidos {cmp_['anadidos']}")
        for s in cmp_["sustituciones"]:
            if s["dif_a"] is not None and s["dif_de"] is not None and s["dif_a"] > s["dif_de"]:
                pe.append(f"S{i + 1}: SEGURIDAD S5 sustituto mas dificil {s}")
            if not s["dosis_no_mayor"]:
                pe.append(f"S{i + 1}: SEGURIDAD S2 el sustituto sube la dosis {s}")
        # origen
        if cmp_["igual"]:
            n_iguales += 1
            if estado_v32 == "RESPALDO":
                origen = "RESPALDO-COPIA"
            elif estado_v32 in ("SIN_EFECTO", "APLICADA"):
                origen = "SIN_EFECTO" if iv in REQUIERE_IA else "SIN_EFECTO_SIN_IA"
            else:
                origen = "NINGUNO"
            if wid in aplicadas:
                pe.append(f"S{i + 1}: V32 la da por aplicada y no cambio")
        else:
            n_cambiadas += 1
            nota = v1["conservation_report"] or ""
            if wid in aplicadas and v1["generation_source"] == "AI":
                origen = "IA"
                hubo_cambio_ia = True
            elif "RETIRADA_SEGURA" in nota:
                origen = "RETIRADA"
            elif estado_v32 == "RESPALDO":
                origen = "RESPALDO-SUST"
            else:
                origen = "DESCONOCIDO"
                pe.append(f"S{i + 1}: cambio sin origen identificable ({estado_v32})")
        # Factibilidad medida por la sonda (modos con IA): ¿habia alguna modificacion simple valida?
        etiqueta = f"{uid}/ciclo1"
        sonda = [f for f in D.factibilidad if f["etiqueta"] == etiqueta and int(f["sesion"]) == wid]
        factible = None if not sonda else int(sonda[0]["validas"]) > 0
        # Conservacion justificada: la politica conserva sin preguntar (CAUSA_NO_REGISTRADA), la IA no estaba
        # (RESPALDO sin IA), o no habia ninguna modificacion valida. Si la habia, la conservacion es la que la
        # politica PERMITE (la IA no propuso un cambio valido), no una conservacion justificada: CONS-POL.
        justificada = cmp_["igual"] and (iv not in REQUIERE_IA or (estado_v32 == "RESPALDO" and not D.intentos_de(etiqueta))
                                         or factible is False)
        permitida = cmp_["igual"] and estado_v32 in ("RESPALDO", "SIN_EFECTO", "APLICADA")
        cat = pertinencia(iv, cmp_, justificada)
        if cat == "INC" and permitida and not justificada:
            cat = "CONS-POL"
        if cat == "INC" and origen != "IA":
            pe.append(f"S{i + 1}: INC de origen determinista ({origen})")
        filas_sesion.append({"id": uid, "sesion": f"S{i + 1}", "intervencion": iv, "estado_v32": estado_v32,
                             "origen": origen, "cambio": "no" if cmp_["igual"] else "si", "categoria": cat,
                             "factible_sonda": "" if factible is None else ("si" if factible else "no"),
                             "sustituciones": json.dumps(cmp_["sustituciones"]),
                             "reducciones": json.dumps(cmp_["reducciones"]),
                             "trabajo_antes_s": cmp_["trabajo_antes"], "trabajo_despues_s": cmp_["trabajo_despues"]})
    # NO_CHANGE / INSUFFICIENT_DATA: copia exacta
    if dec in ("NO_CHANGE", "INSUFFICIENT_DATA") and estado_v32 != "SIN_ORDEN":
        pe.append(f"sin orden y V32 {estado_v32}")
    # texto (T1)
    texto = p2["adjustment_reason"] or ""
    if senaladas:
        dice_cambio = any(f in texto for f in FRASES_CAMBIO)
        dice_igual = any(f in texto for f in FRASES_SIN_CAMBIO)
        if (n_cambiadas > 0) != dice_cambio or (n_iguales > 0) != dice_igual:
            pf.append(f"T1 texto incoherente (cambiadas {n_cambiadas}, iguales {n_iguales}): {texto[:160]}")
        if "IA" in texto.split() and not hubo_cambio_ia:
            pf.append("T1 el texto atribuye a la IA un cambio que no hizo")
    elif any(f in texto for f in FRASES_CAMBIO):
        pf.append(f"T1 sin orden y el texto anuncia cambios: {texto[:120]}")
    det["texto_s2"] = texto
    # exclusiones de seguridad activas no reaparecen
    excl = {int(e["exercise_id"]) for e in D.exclusiones if int(e["user_id"]) == u and e["status"] == "ACTIVE"}
    for w in s2:
        for e in D.ejercicios(w):
            if e["id"] in excl:
                pe.append(f"SEGURIDAD S1: el excluido {e['id']} reaparece en S2")
    ch["E_adaptacion"] = pe
    ch["F_publicacion"] = pf

    # ---------------------------------------------------------------- G. continuidad (S2 -> S3)
    pg = []
    if str(res.get("s2_api_http")) != "200" or str(res.get("s2_api_sesiones")) != str(len(s2)):
        pg.append(f"API plan S2: {res.get('s2_api_http')} sesiones {res.get('s2_api_sesiones')}")
    if str(res.get("s2_api_current_http")) != "200":
        pg.append(f"API plan actual: {res.get('s2_api_current_http')}")
    pr, _ = comprobar_registro(D, s2, ["H"] * len(s2), [""] * len(s2), "S2")
    pg += pr
    ads2 = adherencia_sesiones(D, s2)
    dec2, senal2, aw2, aobs2, c2 = decidir(ads2, D.good, D.moderate)
    m2 = D.metricas.get((u, S2.isoformat()))
    if m2 is None or abs(decimal(m2["weighted_adherence_pct"]) - aw2) > Decimal("0.01"):
        pg.append(f"metricas S2: {None if m2 is None else m2['weighted_adherence_pct']} oraculo {aw2}")
    v2 = [r for r in v32 if r["new_week_start_date"] == S3.isoformat()]
    if len(v2) != 1 or v2[0]["decision"] != dec2:
        pg.append(f"decision S2: backend {[r['decision'] for r in v2]} oraculo {dec2}")
    p3 = D.plan(u, S3)
    if len(p3) != 1:
        pg.append(f"{len(p3)} planes vigentes S3")
    else:
        pg += validar_plan(D, p3[0], perfil, "S3")
        s3 = D.sesiones(int(p3[0]["plan_id"]))
        por_origen3 = {int(w["source_planned_workout_id"]): w for w in s3 if w["source_planned_workout_id"]}
        if dec2 in ("NO_CHANGE", "INSUFFICIENT_DATA"):
            for i, w in enumerate(s2):
                d3 = por_origen3.get(int(w["planned_workout_id"]))
                if d3 is None or not comparar_sesion(D.ejercicios(w), D.ejercicios(d3), D.dificultad, D.p)["igual"]:
                    pg.append(f"S3 sesion {i + 1}: no es copia de S2 con NO_CHANGE")
        # ningun ejercicio retirado o sustituido en S1->S2 vuelve en S3 (reincorporacion automatica)
        retirados = set()
        for i, w in enumerate(s1):
            d2 = por_origen.get(int(w["planned_workout_id"]))
            if d2 is not None and int(w["planned_workout_id"]) in senaladas:
                retirados |= {e["id"] for e in D.ejercicios(w)} - {e["id"] for e in D.ejercicios(d2)}
        for w in s3:
            for e in D.ejercicios(w):
                if e["id"] in retirados and int(w["source_planned_workout_id"] or 0) and e["id"] not in \
                        {x["id"] for x in D.ejercicios(D.pw[int(w["source_planned_workout_id"])])}:
                    pg.append(f"reincorporacion indebida de {e['id']} en S3")
                if e["id"] in excl:
                    pg.append(f"SEGURIDAD S1: el excluido {e['id']} reaparece en S3")
    ch["G_continuidad"] = pg

    malas = [k for k, v in ch.items() if v]
    det["estado"] = "PASS" if not malas else "FAIL"
    det["adaptacion_efectiva_ia"] = hubo_cambio_ia
    return det, filas_sesion


# ===================================================================== huella reproducible

def huella(D):
    """Representacion canonica: ids sustitutos -> (usuario, semana, posicion); sin marcas de tiempo."""
    canon = {}
    for uid, u in sorted(D.usuarios.items()):
        semanas = {}
        posicion = {}
        for semana in (S1, S2, S3):
            for p in D.plan(u, semana):
                for i, w in enumerate(D.sesiones(int(p["plan_id"]))):
                    posicion[int(w["planned_workout_id"])] = f"{semana}#{i + 1}"
        for semana in (S1, S2, S3):
            planes = D.plan(u, semana)
            if not planes:
                semanas[str(semana)] = None
                continue
            p = planes[0]
            ses = []
            for w in D.sesiones(int(p["plan_id"])):
                wid = int(w["planned_workout_id"])
                ej = [[e["id"], e["type"], e["sets"], e["reps"], e["dur"], str(e["load"]), e["rest"]] for e in D.ejercicios(w)]
                intentos = [s for s in D.ses.get(wid, []) if s["counts_toward_adherence"] == "true"]
                hecho = None
                if intentos:
                    filas = D.wse.get(int(intentos[-1]["session_id"]), {})
                    hecho = [[f["actual_sets"], f["actual_reps_total"], f["actual_duration_seconds"], f["completion_percentage"]]
                             for _, f in sorted(filas.items())]
                ses.append({"fecha": w["scheduled_date"], "foco": w["focus_code"], "min": w["expected_duration_minutes"],
                            "estado": w["status"], "omision": w["skip_reason"],
                            "origen": posicion.get(int(w["source_planned_workout_id"] or 0)), "ejercicios": ej,
                            "ejecucion": hecho})
            m = D.metricas.get((u, str(semana)), {})
            semanas[str(semana)] = {"fuente": p["generation_source"], "estado": p["status"], "version": p["plan_version"],
                                    "ajuste": p["adjustment_applied"], "texto": p["adjustment_reason"], "sesiones": ses,
                                    "metricas": [m.get(k) for k in ("weighted_adherence_pct", "observed_adherence_pct",
                                                                    "registration_coverage_pct")]}
        v32 = [[r["decision"], r["status"], r["generation_source"], r["ai_calls"], r["new_week_start_date"],
                [posicion.get(x, x) for x in lista_pg(r["targeted_sessions"])], r["targeted_interventions"],
                [posicion.get(x, x) for x in lista_pg(r["applied_sessions"])], r["reason_codes"], r["rejection_reason"]]
               for r in sorted(D.v32.get(u, []), key=lambda r: r["new_week_start_date"])]
        canon[uid] = {"semanas": semanas, "v32": v32}
    texto = json.dumps(canon, sort_keys=True, ensure_ascii=False)
    return hashlib.sha256(texto.encode("utf-8")).hexdigest(), canon


# ===================================================================== principal

def main():
    carpeta, ruta_perfiles = sys.argv[1], sys.argv[2]
    teoricos = {}
    if len(sys.argv) > 3:
        teoricos = {r["id"]: r for r in leer(sys.argv[3])}
    D = Datos(carpeta)
    perfiles = leer(ruta_perfiles)
    salida = os.path.join(carpeta, "oraculo")
    os.makedirs(salida, exist_ok=True)
    detalles, sesiones = [], []
    for p in perfiles:
        det, fs = evaluar_usuario(D, p, teoricos.get(p["id"]))
        detalles.append(det)
        sesiones += fs
    fases = ["A_generacion", "B_registro", "C_adherencia", "D_decision", "E_adaptacion", "F_publicacion", "G_continuidad"]
    with open(os.path.join(salida, "matriz_20.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["id", "patron"] + fases + ["decision", "estado_v32", "adaptacion_efectiva_ia", "estado"])
        for p, d in zip(perfiles, detalles):
            ch = d.get("comprobaciones", {})
            w.writerow([p["id"], p["patron"] + " " + p["secuencia"]] + [fase(ch[k]) if k in ch else "NA" for k in fases]
                       + [d.get("decision", {}).get("backend"), d.get("v32", {}).get("status"),
                          "si" if d.get("adaptacion_efectiva_ia") else "no", d.get("estado")])
    with open(os.path.join(salida, "sesiones.csv"), "w", newline="", encoding="utf-8") as f:
        if sesiones:
            w = csv.DictWriter(f, fieldnames=list(sesiones[0].keys()))
            w.writeheader()
            w.writerows(sesiones)
    with open(os.path.join(salida, "detalle.json"), "w", encoding="utf-8") as f:
        json.dump(detalles, f, ensure_ascii=False, indent=1, default=str)
    h, _ = huella(D)
    with open(os.path.join(salida, "hash_normalizado.txt"), "w", encoding="utf-8") as f:
        f.write(h + "\n")
    estados = {}
    for d in detalles:
        estados[d["estado"]] = estados.get(d["estado"], 0) + 1
    lineas = [f"# Oraculo K20 · {os.path.basename(os.path.normpath(carpeta))}", "",
              f"- Configuracion de calculo activa: {D.cfg_version}",
              f"- Estados: {json.dumps(estados, sort_keys=True)}",
              f"- Huella normalizada: `{h}`", "", "| Usuario | Estado | Fallos |", "|---|---|---|"]
    for d in detalles:
        fallos = "; ".join(f"{k}: {' / '.join(v)}" for k, v in d.get("comprobaciones", {}).items() if v)
        lineas.append(f"| {d['id']} | {d['estado']} | {fallos[:400] or '-'} |")
    with open(os.path.join(salida, "resumen.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(lineas) + "\n")
    print(json.dumps(estados, sort_keys=True), h)


if __name__ == "__main__":
    main()
