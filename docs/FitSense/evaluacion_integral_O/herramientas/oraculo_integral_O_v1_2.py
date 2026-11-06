#!/usr/bin/env python3
"""Oraculo INDEPENDIENTE de la evaluacion integral de la cohorte O · VERSION 1.2 (IA REAL).

v1.2 = v1.1 (congelado en cc54a3c, aprobado) + UN solo cambio, R-1, necesario para evaluar respuestas REALES de
Replicate (orden del 06/11/2026). Los criterios A5 y A10 de v1.1 exigian que el "intento 1, invalido a proposito"
existiera y no fuera publicable: eso solo describe a la IA SIMULADA del laboratorio. Con la IA real no hay intento
invalido a proposito y un intento 1 valido es el caso normal. Por eso, SOLO cuando la corrida declara
`modo: REPLICATE`:
  R-1  no se exige el intento 1 invalido; se exige en su lugar que, si el plan publicado proviene de la IA
       (generation_source = AI), al menos un intento de ese ciclo haya sido juzgado PUBLICABLE por el backend
       (nunca se publica una propuesta rechazada). El resto de A5/A10 (mas trabajo, sustituto mas dificil o de otro
       patron/grupo, dolor, sesiones no senaladas, techo, tope de dificultad, prohibidos) se aplica igual al plan
       publicado, sea cual sea la respuesta del modelo.
En cualquier otro modo (DETERMINISTA, SIMULADA) el comportamiento es byte a byte el de v1.1.

(Texto de la version 1.1:) Oraculo INDEPENDIENTE de la evaluacion integral de la cohorte O · VERSION 1.1.

Corrige dos defectos del oraculo 1.0 (congelado; su resultado oficial se conserva) detectados en S_O y diagnosticados
en 05_DEFECTOS_Y_CORRECCION.md:
  D-OR-1  A7: con la progresion apagada y PLAN-REV-2.0, P-A1 (AdaptedWeekCommandService.continuidad, regla aprobada)
          admite la RECUPERACION de una reduccion por fatiga; el 1.0 la contaba como "paso con la progresion apagada".
  D-OR-2  A8: la reaparicion de un retirado por ACORTAR se buscaba en TODA la semana y no en su linaje; un ejercicio
          presente desde S1 en otra sesion daba un falso "restauracion automatica" y una posicion equivocada.
Ninguna otra regla cambia.

(Texto original de la version 1.0:) Oraculo INDEPENDIENTE de la evaluacion integral de la cohorte O (orden del 06/11/2026).

Lee SOLO las tablas exportadas por EvaluacionK20IT en modo integral (tablas/), los ficheros de la corrida
(semanas_integral.csv, sesiones_integral.csv, limitaciones.jsonl, intentos_ia.csv, resultados_20.csv) y la cohorte
congelada. No llama al backend. Reutiliza SOLO funciones puras del oraculo historico K20 (redondeos, dosis, segundos
estimados, comparacion de sesiones, pertinencia), igual que el oraculo versionado por politica.

Evalua los 10 aspectos del protocolo (PROTOCOLO_EVALUACION_20_USUARIOS.md, aprobado) con estados
APROBADO / FALLO / NO APLICA / REQUIERE REVISION. Reglas fijadas ANTES de ejecutar la cohorte O (ver
../02_CRITERIOS_DE_EVALUACION.md); no se cambian despues de ver resultados.

Uso:
    python3 -I oraculo_integral_O.py <O_PERFILES.csv> <salida_dir> <corrida1> [<corrida2> ...]
Cada perfil debe aparecer en una sola corrida (bloque G con progresion apagada; bloque P con progresion encendida).
"""
import csv
import hashlib
import importlib.util
import json
import os
import re
import sys
from collections import Counter
from datetime import date, datetime, timedelta
from decimal import Decimal

sys.dont_write_bytecode = True
csv.field_size_limit(sys.maxsize)
AQUI = os.path.dirname(os.path.abspath(__file__))
HIST = os.path.normpath(os.path.join(AQUI, "..", "..", "short_paper_ieee", "k20_adaptacion_real", "herramientas",
                                     "oraculo_k20.py"))
_spec = importlib.util.spec_from_file_location("oraculo_k20", HIST)
H = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(H)

S1 = date(2026, 11, 2)
TOPE = {"BEGINNER": 4, "INTERMEDIATE": 6, "ADVANCED": 6}
MAX_DIF = {"BEGINNER": 1, "INTERMEDIATE": 2, "ADVANCED": 3}
NORDICOS = {1195, 1196, 1197, 327, 915, 916}
NO_DEMOSTRADA = {775, 973, 1261, 1308, 1259, 1146}
PANTALLA = {853, 580, 1093, 1320}
SIN_RESISTIDO = {"LOCOMOTION_CARDIO", "MOBILITY", "BALANCE_CONTROL", "OTHER", "", None}
SLOT = {"HORIZONTAL_PUSH", "VERTICAL_PUSH", "HORIZONTAL_PULL", "VERTICAL_PULL", "KNEE_DOMINANT", "HIP_HINGE",
        "HIP_EXTENSION"}
TECHO = Decimal("1.10")
AP, FA, NA, RR = "APROBADO", "FALLO", "NO APLICA", "REQUIERE REVISION"
ASPECTOS = ["A1_generacion", "A2_personalizacion_restricciones", "A3_registro_ejecucion", "A4_adherencia",
            "A5_adaptacion_ia", "A6_mantenimiento", "A7_progresion", "A8_reincorporacion", "A9_continuidad",
            "A10_seguridad_integridad"]


def semana(k):
    return S1 + timedelta(weeks=k - 1)


def leer(ruta):
    if not os.path.exists(ruta) or os.path.getsize(ruta) == 0:
        return []
    with open(ruta, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def lista(v):
    return H.lista_pg(v)


def volumen(ej):
    """Volumen de ProgresionIT / ProgresionResolver: series x reps; DURATION: round(series x segundos / 30)."""
    t = 0
    for e in ej:
        s = e["sets"] if e["sets"] is not None else 1
        if e["type"] == "DURATION":
            t += H.round_java(s * (e["dur"] or 0) / 30.0)
        else:
            t += s * (e["reps"] or 0)
    return t


def dosis(e):
    return (e["id"], e["type"], e["sets"], e["reps"], e["dur"], str(e["load"]), e["rest"])


class Corrida:
    def __init__(self, carpeta):
        self.carpeta = carpeta
        self.D = H.Datos(carpeta)
        self.semanas = leer(os.path.join(carpeta, "semanas_integral.csv"))
        self.sesiones_guion = leer(os.path.join(carpeta, "sesiones_integral.csv"))
        self.lim = []
        ruta = os.path.join(carpeta, "limitaciones.jsonl")
        if os.path.exists(ruta):
            with open(ruta, encoding="utf-8") as f:
                self.lim = [json.loads(l) for l in f if l.strip()]
        self.cfg = {}
        with open(os.path.join(carpeta, "00_configuracion.txt"), encoding="utf-8") as f:
            for l in f:
                if ": " in l:
                    k, v = l.rstrip("\n").split(": ", 1)
                    self.cfg[k] = v
        self.fecha = datetime.fromisoformat(self.cfg["fecha_ejecucion_utc"].replace("Z", "+00:00")).date()
        D = self.D
        self.bp = {r["body_part_id"]: r["code"] for r in D.tab["body_parts"]}
        self.umbral_completada = Decimal(str(json.loads([c for c in D.tab["calculation_configs"]
                                                          if c["is_active"] == "true"][0]["params"])
                                             ["adherence"]["session_completed_threshold_pct"]))

    # --------------------------------------------------------------- utilidades sobre las tablas
    def planes_usuario(self, u):
        return sorted([p for p in self.D.planes if int(p["user_id"]) == u], key=lambda p: (p["week_start_date"],
                                                                                         int(p["plan_version"])))

    def plan_vigente(self, u, k):
        c = [p for p in self.D.planes if int(p["user_id"]) == u and p["week_start_date"] == semana(k).isoformat()
             and not p["replaced_at"]]
        return c

    def ej(self, w):
        return self.D.ejercicios(w)

    def ses(self, plan):
        return self.D.sesiones(int(plan["plan_id"]))

    def grupo(self, eid):
        c = self.D.ejercicio.get(eid)
        return self.bp.get(c["body_part_id"]) if c else None

    def patron(self, eid):
        c = self.D.ejercicio.get(eid)
        return c["movement_pattern"] if c else None

    def estado_sesion(self, w):
        """Cumplimiento de UNA sesion (regla documentada) y su causa (SessionOutcomeIdentifier)."""
        D = self.D
        wid = int(w["planned_workout_id"])
        intentos = [s for s in D.ses.get(wid, []) if s["counts_toward_adherence"] == "true"]
        r = {"id": wid, "fecha": w["scheduled_date"], "estado_backend": w["status"], "registrada": False, "pct": None,
             "motivo": None, "dolor": False, "fatiga": False, "pct_backend": None, "causas": []}
        if intentos:
            s = intentos[-1]
            filas = D.wse.get(int(s["session_id"]), {})
            pcts, causas = [], []
            for e in self.ej(w):
                f = filas.get(e["pe"])
                real = None if f is None else (f["actual_duration_seconds"] if e["type"] == "DURATION"
                                               else f["actual_reps_total"])
                pcts.append(H.pct_ejercicio(H.entero(real, 0), H.dosis_total(e)))
                if f is not None and f.get("skip_reason"):
                    causas.append(f["skip_reason"])
            r["registrada"] = True
            r["pct"] = H.r2(min(Decimal(100), sum(pcts, Decimal(0)) / len(pcts))) if pcts else Decimal(0)
            r["pct_backend"] = H.decimal(s["completion_percentage"])
            r["causas"] = causas
            r["dolor"] = "PAIN_OR_DISCOMFORT" in causas
            r["fatiga"] = "FATIGUE" in causas
            dominante = None
            if causas:
                cuenta = Counter(causas).most_common()
                dominante = cuenta[0][0]
                r["empate_de_causas"] = len(cuenta) > 1 and cuenta[0][1] == cuenta[1][1]
            r["motivo"] = "PAIN_OR_DISCOMFORT" if r["dolor"] else dominante or (w["skip_reason"] or None)
        elif w["status"] == "SKIPPED" and w["skip_reason"]:
            r["registrada"] = True
            r["pct"] = Decimal(0)
            r["motivo"] = w["skip_reason"]
            r["dolor"] = w["skip_reason"] == "PAIN_OR_DISCOMFORT"
            r["fatiga"] = w["skip_reason"] == "FATIGUE"
        return r

    def semana_adherencia(self, plan):
        sesiones = [self.estado_sesion(w) for w in self.ses(plan)]
        n = len(sesiones)
        reg = [s for s in sesiones if s["registrada"]]
        aobs = H.r2(sum((s["pct"] for s in reg), Decimal(0)) / len(reg)) if reg else None
        cob = H.r2(Decimal(100) * len(reg) / n) if n else None
        aw = H.r2(sum((s["pct"] or Decimal(0) for s in sesiones), Decimal(0)) / n) if n else None
        return sesiones, aobs, cob, aw

    def decidir(self, sesiones, aobs, cob):
        """AdaptationOrderDecider, recalculado: dolor primero; evidencia; >= bueno; senaladas; alcance."""
        D = self.D
        bueno, moderado = D.good, D.moderate
        con_dolor = [s for s in sesiones if s["registrada"] and s["motivo"] == "PAIN_OR_DISCOMFORT"]
        evidencia = cob is not None and aobs is not None and cob >= H.UMBRAL_COBERTURA
        adaptaria = evidencia and aobs < bueno

        def interv(s):
            return H.INTERVENCION.get(s["motivo"], "CAUSA_NO_REGISTRADA") if s["motivo"] else "CAUSA_NO_REGISTRADA"
        if con_dolor and not adaptaria:
            return "ADAPT_LOCALIZED", [(s["id"], "SEGURIDAD_ANTE_DOLOR") for s in con_dolor]
        if not evidencia:
            return "INSUFFICIENT_DATA", []
        if aobs >= bueno:
            return "NO_CHANGE", []
        senaladas = sorted([s for s in sesiones if s["registrada"] and (s["pct"] is None or s["pct"] < self.umbral_completada)],
                           key=lambda s: (s["pct"] or Decimal(0), s["fecha"]))
        if not senaladas and not con_dolor:
            return "INSUFFICIENT_DATA", []
        if aobs < moderado:
            obj = list(senaladas) + [d for d in con_dolor if d["id"] not in {s["id"] for s in senaladas}]
            return "ADAPT_EXPANDED", [(s["id"], interv(s)) for s in obj]
        if con_dolor:
            return "ADAPT_LOCALIZED", [(s["id"], interv(s)) for s in con_dolor]
        return "ADAPT_LOCALIZED", [(senaladas[0]["id"], interv(senaladas[0]))]

    def limitaciones(self, pid, momento):
        for m in self.lim:
            if m.get("id") == pid and m.get("momento") == momento:
                return m
        return None

    def perfil_seguridad(self, perfil):
        nac = date.fromisoformat(perfil["birth_date"])
        edad = self.fecha.year - nac.year - ((self.fecha.month, self.fecha.day) < (nac.month, nac.day))
        nivel = perfil["fitness_level"]

        def prohibe(col):
            return perfil.get(col) == "false"
        return edad, {
            "high_impact": edad >= 60 or nivel == "BEGINNER" or prohibe("allow_high_impact"),
            "requires_floor": edad >= 70 or prohibe("allow_floor_exercises"),
            "axial_load": edad >= 65,
            "requires_kneeling": prohibe("allow_kneeling_exercises"),
            "requires_hanging": prohibe("allow_hanging_exercises"),
            "requires_overhead": prohibe("allow_overhead_exercises"),
            "requires_inversion": edad >= 65,
        }


# ======================================================================== evaluacion de un usuario

def evaluar(C, pid, prof, u, res_row):
    D = C.D
    det = {"id": pid, "user_id": u, "carpeta": os.path.basename(C.carpeta), "checks": {a: [] for a in ASPECTOS},
           "notas": {a: [] for a in ASPECTOS}}
    est = {}
    perfil = D.perfil.get(u)
    esperado_sin_plan = prof["escenario"] == "ESPERADO_SIN_PLAN"
    # La progresion la decide la CONFIGURACION REAL de la corrida, no la etiqueta del perfil (se comprueban las dos).
    bloque_p = C.cfg.get("progresion", "false").strip() == "true"
    if (prof["bloque"] == "P") != bloque_p:
        det["checks"]["A10_seguridad_integridad"].append(
            f"el perfil declara el bloque {prof['bloque']} y la corrida tiene progresion={C.cfg.get('progresion')}")
    semanas = sorted([r for r in C.semanas if r["id"] == pid], key=lambda r: int(r["semana"]))
    guiones = [r for r in C.sesiones_guion if r["id"] == pid]
    planes = C.planes_usuario(u)
    vigentes = {}
    for k in range(1, 40):
        v = C.plan_vigente(u, k)
        if v:
            vigentes[k] = v
    det["generacion_backend"] = res_row.get("generacion")
    edad, prohibido = C.perfil_seguridad(perfil)
    det["edad_en_la_ejecucion"] = edad
    det["prohibido"] = {k: v for k, v in prohibido.items() if v}
    bloqueados = set(lista(perfil["blocked_exercise_ids"]))
    equipo = set(lista(perfil["equipment_codes"]))
    nivel = perfil["fitness_level"]
    tope = TOPE.get(nivel, 6)
    esperadas = min(int(perfil["days_per_week"]), tope, len(set(lista(perfil["available_days"]))))
    det["sesiones_esperadas"] = esperadas
    lim_g = C.limitaciones(pid, "TRAS_GENERACION")
    lim_f = C.limitaciones(pid, "FINAL")
    codigos_g = sorted({l.get("code") for l in (lim_g or {}).get("cuerpo", {}).get("limitations", [])}) if lim_g else []
    codigos_f = sorted({l.get("code") for l in (lim_f or {}).get("cuerpo", {}).get("limitations", [])}) if lim_f else []
    det["limitaciones_tras_generacion"] = codigos_g
    det["limitaciones_final"] = codigos_f

    # ------------------------------------------------------------------ A1 generacion
    a1 = det["checks"]["A1_generacion"]
    p1 = vigentes.get(1)
    if esperado_sin_plan:
        if p1:
            a1.append("se esperaba ESPERADO_SIN_PLAN y se publico un plan (prediccion no cumplida)")
            est["A1_generacion"] = RR
        else:
            if not lim_g or lim_g.get("http") != 200 or not codigos_g:
                a1.append(f"sin plan, pero /plan/limitations no lo explica: {lim_g and lim_g.get('http')} {codigos_g}")
            est["A1_generacion"] = AP if not a1 else FA
        det["estado_generacion"] = "ESPERADO_SIN_PLAN" if not p1 else "PLAN_PUBLICADO"
    else:
        if not p1:
            a1.append(f"sin plan en S1: {res_row.get('generacion')}")
        elif len(p1) != 1:
            a1.append(f"{len(p1)} planes vigentes en S1")
        else:
            plan = p1[0]
            val = H.validar_plan(D, plan, perfil, "S1")
            # REV2: sin V20; V4M (segundos <= M x 60), V26O, V27O, QINS (oraculo versionado) y DIAS-1
            val = [x for x in val if ": V20 " not in x and "sesiones y el perfil pide" not in x]
            n = len(C.ses(plan))
            if n != esperadas:
                val.append(f"S1: DIAS1 {n} sesiones; se esperan {esperadas} (min(dias, tope {tope}, disponibles))")
            if plan.get("planned_days_count") and int(plan["planned_days_count"]) != n:
                val.append(f"S1: planned_days_count {plan['planned_days_count']} != {n}")
            for w in C.ses(plan):
                ej = C.ej(w)
                m = int(perfil["session_minutes"])
                seg = H.segundos_estimados(ej, D.p)
                if seg > m * 60:
                    val.append(f"S1 {w['scheduled_date']}: V4M {seg} s > {m * 60} s")
                series = sum((e["sets"] or 1) for e in ej)
                if len(ej) < 3 or series < 6:
                    val.append(f"S1 {w['scheduled_date']}: QINS {len(ej)} ejercicios y {series} series")
            a1.extend(val)
            det["s1"] = {"plan_id": int(plan["plan_id"]), "sesiones": n, "politica": plan.get("planning_policy"),
                         "minutos_declarados": [int(w["expected_duration_minutes"]) for w in C.ses(plan)],
                         "minutos_estimados": [H.minutos_estimados(C.ej(w), D.p) for w in C.ses(plan)],
                         "ejercicios_por_sesion": [len(C.ej(w)) for w in C.ses(plan)]}
            traza = {}
            try:
                traza = json.loads(plan.get("planning_policy_trace") or "{}")
            except ValueError:
                pass
            det["s1"]["estado_dosis"] = (traza.get("dosis") or {}).get("estado")
            det["s1"]["patrones"] = sorted({C.patron(e["id"]) for w in C.ses(plan) for e in C.ej(w)
                                            if C.patron(e["id"]) in SLOT})
        est["A1_generacion"] = AP if not a1 else FA
        det["estado_generacion"] = "PLAN_PUBLICADO" if p1 else "SIN_PLAN"

    sin_plan = not p1
    # ------------------------------------------------------------------ A2 personalizacion y restricciones
    a2 = det["checks"]["A2_personalizacion_restricciones"]
    if not lim_g or lim_g.get("http") != 200:
        a2.append(f"/plan/limitations tras la generacion: HTTP {lim_g and lim_g.get('http')}")
    smax_esperado = int(perfil["days_per_week"]) > tope
    if ("S_MAX" in codigos_g) != smax_esperado and not sin_plan:
        a2.append(f"S_MAX {'ausente' if smax_esperado else 'presente'} (dias {perfil['days_per_week']}, tope {tope})")
    if not sin_plan:
        for k, v in sorted(vigentes.items()):
            for w in C.ses(v[0]):
                for e in C.ej(w):
                    c = D.ejercicio.get(e["id"])
                    if e["id"] in bloqueados:
                        a2.append(f"S{k} {w['scheduled_date']}: ejercicio bloqueado {e['id']}")
                    if not H.equipo_cumple(D.grupos.get(e["id"], {}), equipo):
                        a2.append(f"S{k} {w['scheduled_date']}: equipamiento de {e['id']} no declarado")
                    for flag, prohib in prohibido.items():
                        if prohib and c and c.get(flag) == "true":
                            a2.append(f"S{k} {w['scheduled_date']}: {e['id']} exige {flag} (restringido)")
        # Avisos de dosis: DOSIS_NO_DEMOSTRADA (E2-a) sobre el plan mas reciente, recalculado
        for momento, codigos, plan in (("TRAS_GENERACION", codigos_g, p1[0] if p1 else None),
                                       ("FINAL", codigos_f, max(planes, key=lambda p: (p["week_start_date"],
                                                                                        int(p["plan_version"]))))):
            if plan is None:
                continue
            ids = {e["id"] for w in C.ses(plan) for e in C.ej(w)}
            esperado_nd = False
            for g in ("back", "shoulders"):
                fuentes = {i for i in ids if C.grupo(i) == g and C.patron(i) not in SIN_RESISTIDO}
                if fuentes and fuentes <= NO_DEMOSTRADA:
                    esperado_nd = True
            if ("DOSIS_NO_DEMOSTRADA" in codigos) != esperado_nd:
                a2.append(f"{momento}: DOSIS_NO_DEMOSTRADA {'falta' if esperado_nd else 'sobra'}")
            det.setdefault("aviso_e2a", {})[momento] = esperado_nd
    est["A2_personalizacion_restricciones"] = AP if not a2 else FA

    if sin_plan:
        for a in ASPECTOS[2:]:
            est[a] = NA
            det["notas"][a].append("sin plan (ESPERADO_SIN_PLAN)" if esperado_sin_plan else "sin plan")
        if not esperado_sin_plan:
            est["A9_continuidad"] = FA
        det["estados"] = est
        det["semanas"] = []
        return det

    # ------------------------------------------------------------------ recorrido semanal
    a3 = det["checks"]["A3_registro_ejecucion"]
    a4 = det["checks"]["A4_adherencia"]
    a5 = det["checks"]["A5_adaptacion_ia"]
    a6 = det["checks"]["A6_mantenimiento"]
    a7 = det["checks"]["A7_progresion"]
    a8 = det["checks"]["A8_reincorporacion"]
    a9 = det["checks"]["A9_continuidad"]
    a10 = det["checks"]["A10_seguridad_integridad"]
    v32 = sorted(D.v32.get(u, []), key=lambda r: (r["new_week_start_date"], int(r["attempt_id"])))
    raiz = {}            # posicion de la sesion (orden en S1) -> ejercicios de la generacion
    linaje = {}          # planned_workout_id -> posicion en S1
    for i, w in enumerate(C.ses(p1[0])):
        raiz[i] = C.ej(w)
        linaje[int(w["planned_workout_id"])] = i
    retirados = {}       # exercise_id -> (semana del ciclo, posicion de linaje, dosis inicial exacta)
    excluidos_dolor = set()
    pasos_por_linaje = {}
    hubo_adaptacion_ia = False
    hubo_mantenimiento = False
    hubo_progresion_esperable = False
    filas_semana, filas_adapt, filas_pasos = [], [], []
    flags_a1 = set()
    flags_a5 = set()
    flags_a7 = set()
    for w in C.ses(p1[0]):
        for e in C.ej(w):
            if e["id"] in NORDICOS:
                flags_a1.add("E3 curl nordico " + str(e["id"]))
            if e["id"] in PANTALLA:
                flags_a1.add("E4/E7 nombre o instrucciones " + str(e["id"]))
    if det.get("aviso_e2a", {}).get("TRAS_GENERACION"):
        flags_a1.add("E2 dosis de espalda u hombro solo de ejercicios no demostrados")
    adherencia_100 = {}
    sesiones_por_semana = {}
    for fila in semanas:
        k = int(fila["semana"])
        plan_k = vigentes.get(k, [None])[0]
        if plan_k is None:
            a9.append(f"S{k}: no hay plan vigente")
            break
        if str(fila["plan_medido"]) != plan_k["plan_id"]:
            a9.append(f"S{k}: plan medido {fila['plan_medido']} != vigente {plan_k['plan_id']}")
        sess_k = C.ses(plan_k)
        for w in sess_k:
            pos = linaje.get(int(w["source_planned_workout_id"] or 0)) if k > 1 else linaje.get(int(w["planned_workout_id"]))
            if pos is not None:
                linaje[int(w["planned_workout_id"])] = pos
        # A3: registro segun el guion
        pasos = [g for g in guiones if int(g["semana"]) == k]
        pasos.sort(key=lambda g: int(g["orden"]))
        if len(pasos) != len(sess_k):
            a3.append(f"S{k}: {len(pasos)} pasos de guion para {len(sess_k)} sesiones")
        for g, w in zip(pasos, sess_k):
            a3.extend(comprobar_paso(C, g, w, k))
        # A4: adherencia
        sesiones, aobs, cob, aw = C.semana_adherencia(plan_k)
        sesiones_por_semana[k] = sesiones
        for s in sesiones:
            if s["registrada"] and s["pct_backend"] is not None and abs(s["pct_backend"] - s["pct"]) > Decimal("0.01"):
                a4.append(f"S{k} sesion {s['id']}: cumplimiento backend {s['pct_backend']} != oraculo {s['pct']}")
        m = D.metricas.get((u, semana(k).isoformat()))
        if not m:
            a4.append(f"S{k}: sin weekly_user_metrics")
        else:
            for col, val in (("observed_adherence_pct", aobs), ("registration_coverage_pct", cob),
                             ("weighted_adherence_pct", aw)):
                b = H.decimal(m.get(col))
                if (b is None) != (val is None) or (b is not None and abs(b - val) > Decimal("0.01")):
                    a4.append(f"S{k}: {col} backend {b} != oraculo {val}")
        adherencia_100[k] = aobs is not None and aobs == Decimal(100) and cob == Decimal(100)
        # ciclo k
        intento = [r for r in v32 if r["new_week_start_date"] == semana(k + 1).isoformat()]
        if not intento:
            a9.append(f"C{k}: sin fila V32")
            break
        if len(intento) > 1:
            a10.append(f"C{k}: {len(intento)} filas V32 para la misma semana")
        r = intento[-1]
        for col, val in (("observed_adherence_pct", aobs), ("registration_coverage_pct", cob)):
            b = H.decimal(r.get(col))
            if (b is None) != (val is None) or (b is not None and abs(b - val) > Decimal("0.01")):
                a4.append(f"C{k}: V32 {col} {b} != oraculo {val}")
        dec_o, obj_o = C.decidir(sesiones, aobs, cob)
        obj_o = afinar_por_causa(obj_o, sess_k)
        dec_b = r["decision"]
        obj_b = set(lista(r["targeted_sessions"]))
        int_b = set(lista(r["targeted_interventions"]))
        coincide = dec_o == dec_b and obj_b == {x for x, _ in obj_o} and int_b == {i for _, i in obj_o}
        informe = {}
        try:
            informe = json.loads(r["conservation_report"] or "{}")
        except ValueError:
            pass
        pasos_inf = informe.get("pasos") or []
        plan_sig = vigentes.get(k + 1, [None])[0]
        fs = {"id": pid, "semana": k, "inicio": semana(k).isoformat(), "plan": int(plan_k["plan_id"]),
              "programadas": len(sess_k),
              "completas": sum(1 for w in sess_k if w["status"] == "COMPLETED"),
              "parciales": sum(1 for w in sess_k if w["status"] == "PARTIAL"),
              "omitidas": sum(1 for w in sess_k if w["status"] == "SKIPPED"),
              "sin_registro": sum(1 for s in sesiones if not s["registrada"]),
              "guion": fila["guion"], "adherencia_observada": aobs, "cobertura": cob, "adherencia_ponderada": aw,
              "senal": ";".join(sorted({s["motivo"] for s in sesiones if s["motivo"]})) or "-",
              "decision_oraculo": dec_o, "decision_backend": dec_b, "coinciden": coincide,
              "intervenciones": "|".join(sorted(int_b)) or "-", "estado_v32": r["status"],
              "fuente": r["generation_source"], "llamadas_ia": r["ai_calls"],
              "plan_siguiente": int(plan_sig["plan_id"]) if plan_sig else None,
              "pasos": "|".join(f"{p.get('tipo')}:{p.get('exercise_id')}" for p in pasos_inf) or "-"}
        filas_semana.append(fs)
        if not coincide:
            (a5 if dec_b.startswith("ADAPT") or dec_o.startswith("ADAPT") else a6).append(
                f"C{k}: decision backend {dec_b} {sorted(obj_b)} {sorted(int_b)} != oraculo {dec_o} {obj_o}")
        if plan_sig is None:
            a9.append(f"C{k}: no se publico la semana {k + 1}")
            break
        sig = C.ses(plan_sig)
        por_origen = {int(w["source_planned_workout_id"] or 0): w for w in sig}
        if len(sig) != len(sess_k) or set(por_origen) != {int(w["planned_workout_id"]) for w in sess_k}:
            a9.append(f"C{k}: linaje incompleto ({len(sig)} sesiones; origenes {sorted(por_origen)})")
        for w in sess_k:
            n = por_origen.get(int(w["planned_workout_id"]))
            if n and date.fromisoformat(n["scheduled_date"]) != date.fromisoformat(w["scheduled_date"]) + timedelta(days=7):
                a9.append(f"C{k}: la sesion derivada de {w['planned_workout_id']} no cae 7 dias despues")
        # Dolor: el ejercicio marcado no vuelve (y queda excluido)
        for w in sess_k:
            s = next(x for x in sesiones if x["id"] == int(w["planned_workout_id"]))
            if s["dolor"] and s["registrada"]:
                intentos_w = [x for x in D.ses.get(int(w["planned_workout_id"]), []) if x["counts_toward_adherence"] == "true"]
                if intentos_w:
                    filas = D.wse.get(int(intentos_w[-1]["session_id"]), {})
                    for e in C.ej(w):
                        f = filas.get(e["pe"])
                        if f is not None and f.get("skip_reason") == "PAIN_OR_DISCOMFORT":
                            excluidos_dolor.add(e["id"])
        objetivo_ids = {x for x, _ in obj_o}
        interv_por_sesion = dict(obj_o)
        ia_autorizada = any(i in H.REQUIERE_IA for _, i in obj_o)
        # ---------------------------------------------- A5 / A6: cambios sesion a sesion
        for w in sess_k:
            wid = int(w["planned_workout_id"])
            n = por_origen.get(wid)
            if n is None:
                continue
            antes, despues = C.ej(w), C.ej(n)
            d = H.comparar_sesion(antes, despues, D.dificultad, D.p)
            paso = next((p for p in pasos_inf if int(p.get("origen") or -1) == wid), None)
            if wid in objetivo_ids and interv_por_sesion[wid] in H.REQUIERE_IA:
                hubo_adaptacion_ia = True
                problemas = []
                if d["aumentos"] or d["anadidos"] or d["trabajo_despues"] > d["trabajo_antes"]:
                    problemas.append(f"mas trabajo: aumentos {d['aumentos']} anadidos {d['anadidos']} "
                                     f"trabajo {d['trabajo_antes']}->{d['trabajo_despues']}")
                for sus in d["sustituciones"]:
                    if (sus["dif_a"] or 0) > (sus["dif_de"] or 0):
                        problemas.append(f"sustituto mas dificil {sus['de']}->{sus['a']}")
                    if C.patron(sus["de"]) != C.patron(sus["a"]) or C.grupo(sus["de"]) != C.grupo(sus["a"]):
                        problemas.append(f"sustituto de otro patron o grupo {sus['de']}->{sus['a']}")
                    if sus["a"] in NORDICOS or sus["a"] in PANTALLA:
                        flags_a5.add(f"sustituto con revision de catalogo pendiente {sus['a']}")
                if interv_por_sesion[wid] == "SEGURIDAD_ANTE_DOLOR":
                    for e in despues:
                        if e["id"] in excluidos_dolor:
                            problemas.append(f"el ejercicio con dolor {e['id']} sigue en la sesion")
                if interv_por_sesion[wid] == "ACORTAR_O_REORGANIZAR":
                    for x in d["retirados"]:
                        ini = next((e for e in raiz.get(linaje.get(wid), []) if e["id"] == x), None)
                        retirados[x] = (k, linaje.get(wid), ini)
                cons_j = d["igual"] and (r["status"] in ("SIN_EFECTO", "NO_FACTIBLE") or
                                         wid in set(lista(r["no_effect_sessions"])))
                pert = pertinencia(interv_por_sesion[wid], d, cons_j)
                filas_adapt.append({"id": pid, "ciclo": k, "sesion": wid, "intervencion": interv_por_sesion[wid],
                                    "estado_v32": r["status"], "fuente": r["generation_source"],
                                    "llamadas_ia": r["ai_calls"], "igual": d["igual"],
                                    "sustituciones": ";".join(f"{s['de']}->{s['a']}" for s in d["sustituciones"]) or "-",
                                    "reducciones": ";".join(f"{x[0]}.{x[1]} {x[2]}->{x[3]}" for x in d["reducciones"]) or "-",
                                    "retirados": ";".join(map(str, d["retirados"])) or "-",
                                    "trabajo_antes_s": d["trabajo_antes"], "trabajo_despues_s": d["trabajo_despues"],
                                    "pertinencia": pert, "problemas": " | ".join(problemas) or "-"})
                a5.extend(f"C{k} sesion {wid}: {p}" for p in problemas)
            elif wid in objetivo_ids:
                # intervencion que no autoriza cambios (CAUSA_NO_REGISTRADA): se conserva
                hubo_mantenimiento = True
                if not d["igual"]:
                    a6.append(f"C{k} sesion {wid}: {interv_por_sesion[wid]} no autoriza cambios y la sesion cambio")
                filas_adapt.append({"id": pid, "ciclo": k, "sesion": wid, "intervencion": interv_por_sesion[wid],
                                    "estado_v32": r["status"], "fuente": r["generation_source"],
                                    "llamadas_ia": r["ai_calls"], "igual": d["igual"], "sustituciones": "-",
                                    "reducciones": "-", "retirados": "-", "trabajo_antes_s": d["trabajo_antes"],
                                    "trabajo_despues_s": d["trabajo_despues"],
                                    "pertinencia": pertinencia(interv_por_sesion[wid], d, True), "problemas": "-"})
            else:
                hubo_mantenimiento = True
                if paso is None:
                    if not d["igual"]:
                        a6.append(f"C{k} sesion {wid}: cambio sin orden ni paso registrado {d['aumentos']} "
                                  f"{d['reducciones']} {d['sustituciones']} {d['retirados']} {d['anadidos']}")
                else:
                    a7.extend(comprobar_paso_progresion(C, k, w, n, antes, despues, paso, raiz, linaje, retirados,
                                                        pasos_por_linaje, sesiones_por_semana, vigentes, filas_pasos,
                                                        pid, flags_a7, excluidos_dolor))
        if pasos_inf and dec_b != "NO_CHANGE":
            a7.append(f"C{k}: paso de progresion con decision {dec_b}")
        if len(pasos_inf) > 1:
            a7.append(f"C{k}: {len(pasos_inf)} pasos en un ciclo (maximo 1)")
        if pasos_inf and not bloque_p:
            # D-OR-1: P-A1 (REV2) admite solo la RECUPERACION con la progresion apagada.
            rev2 = C.cfg.get("politica_por_defecto", "").strip() == "PLAN-REV-2.0"
            if not (rev2 and all(p.get("tipo") == "RECUPERACION" for p in pasos_inf)):
                a7.append(f"C{k}: paso de progresion con la progresion apagada")
            else:
                det["notas"]["A7_progresion"].append(f"C{k}: RECUPERACION con la progresion apagada (P-A1, REV2)")
        # Progresion esperable no producida (solo bloque P, sin pasos ni bloqueos registrados)
        if bloque_p and dec_b == "NO_CHANGE" and not pasos_inf and k >= 2:
            candidatas = sesiones_progresables(C, k, sess_k, sesiones_por_semana, vigentes, linaje, raiz,
                                              pasos_por_linaje, adherencia_100)
            if candidatas:
                hubo_progresion_esperable = True
                if not (informe.get("pasos_bloqueados") or []):
                    det["notas"]["A7_progresion"].append(
                        f"C{k}: condiciones de dos semanas cumplidas en {candidatas} y no hubo paso ni bloqueo registrado")
        # restauracion automatica y exclusiones de dolor, en la semana siguiente
        ids_sig = {e["id"]: (w2, i2, e) for w2 in sig for i2, e in enumerate(C.ej(w2))}
        # D-OR-2: el retirado se busca SOLO en la sesion de su linaje (no en toda la semana).
        por_linaje = {linaje.get(int(w2["source_planned_workout_id"] or 0)): w2 for w2 in sig}
        for x, (kr, pos, ini) in list(retirados.items()):
            w_lin = por_linaje.get(pos)
            en_linaje = {e["id"]: (w_lin, i2, e) for i2, e in enumerate(C.ej(w_lin))} if w_lin is not None else {}
            if kr < k + 1 and x in en_linaje:
                ids_sig = dict(ids_sig)
                ids_sig[x] = en_linaje[x]
                es_reinc = any(p.get("tipo") == "REINCORPORACION" and int(p.get("exercise_id") or -1) == x for p in pasos_inf)
                if not es_reinc:
                    a8.append(f"C{k}: el retirado {x} vuelve sin paso de REINCORPORACION (restauracion automatica)")
                else:
                    w2, i2, e2 = ids_sig[x]
                    prev = [p for p in pasos_por_linaje.get(pos, []) if p[0] > kr and p[1] == "PROGRESION"]
                    if not prev:
                        a8.append(f"C{k}: reincorporacion de {x} sin un paso de PROGRESION posterior a la retirada (C{kr})")
                    if i2 != len(C.ej(w2)) - 1:
                        a8.append(f"C{k}: el reincorporado {x} no va al final de la sesion")
                    if ini is not None and dosis(e2) != dosis(ini):
                        a8.append(f"C{k}: el reincorporado {x} vuelve con {dosis(e2)} y no con su dosis inicial {dosis(ini)}")
                    det.setdefault("reincorporaciones", []).append({"ciclo": k, "exercise_id": x, "retirado_en": kr})
                    del retirados[x]
        for x in excluidos_dolor:
            if x in ids_sig and k >= 1:
                ultima_dolor = max((kk for kk, ss in sesiones_por_semana.items()
                                    for s in ss if s["dolor"]), default=0)
                if k >= ultima_dolor:
                    a10.append(f"C{k}: el ejercicio excluido por dolor {x} vuelve en la semana {k + 1}")
        # techo de volumen por linaje
        for w2 in sig:
            pos = linaje.get(int(w2["source_planned_workout_id"] or 0))
            if pos is None:
                continue
            base = volumen(raiz[pos])
            if volumen(C.ej(w2)) > int((Decimal(base) * TECHO).to_integral_value(rounding="ROUND_FLOOR")):
                a10.append(f"S{k + 1} {w2['scheduled_date']}: volumen {volumen(C.ej(w2))} > techo 1,10 x {base}")
            for e in C.ej(w2):
                if (D.dificultad.get(e["id"]) or 0) > MAX_DIF.get(nivel, 2):
                    a10.append(f"S{k + 1}: {e['id']} dificultad {D.dificultad.get(e['id'])} > tope {MAX_DIF.get(nivel)}")
        if C.cfg.get("modo", "").strip() == "REPLICATE":
            # R-1 (v1.2, IA real): un plan publicado desde la IA exige un intento juzgado publicable en ese ciclo.
            if int(r["ai_calls"] or 0) > 0 and r["generation_source"] == "AI":
                et = f"{pid}/ciclo{k}"
                if not any(x["publicable"] == "true" for x in D.intentos if x["etiqueta"] == et):
                    a10.append(f"C{k}: se publico una adaptacion de la IA sin ningun intento juzgado publicable")
        # intento 1 de la IA simulada: siempre rechazado
        elif int(r["ai_calls"] or 0) > 0:
            et = f"{pid}/ciclo{k}"
            i1 = [i for i in D.intentos if i["etiqueta"] == et and i["intento"] == "1"]
            if not i1:
                a5.append(f"C{k}: con llamadas de IA y sin intento 1 registrado")
            elif any(x["publicable"] == "true" for x in i1):
                a10.append(f"C{k}: la propuesta invalida del intento 1 se considero publicable")
    det["semanas"] = filas_semana
    det["adaptaciones"] = filas_adapt
    det["pasos"] = filas_pasos
    det["retirados_sin_reincorporar"] = sorted(retirados)
    det["excluidos_por_dolor"] = sorted(excluidos_dolor)
    # A9: API y repeticion
    if str(res_row.get("api_plan_final_http")) != "200":
        a9.append(f"GET /plan/<final>: HTTP {res_row.get('api_plan_final_http')}")
    if semanas and int(semanas[-1]["semana"]) < 4:
        a9.append(f"solo {semanas[-1]['semana']} semanas medidas")
    for g in guiones:
        if g["ejecucion"] != "OK":
            a3.append(f"S{g['semana']} sesion {g['orden']}: {g['ejecucion']}")
    s1row = next((x for x in semanas if x["semana"] == "1"), None)
    if s1row is not None:
        if str(s1row.get("repeticion_v32_nuevas")) not in ("0", "0.0") or \
                str(s1row.get("repeticion_planes_nuevos")) not in ("0", "0.0"):
            a10.append(f"idempotencia: repetir el ciclo 1 creo {s1row.get('repeticion_v32_nuevas')} filas V32 y "
                       f"{s1row.get('repeticion_planes_nuevos')} planes")
    # ------------------------------------------------------------------ estados
    est["A3_registro_ejecucion"] = AP if not a3 else FA
    est["A4_adherencia"] = AP if not a4 else FA
    est["A5_adaptacion_ia"] = (FA if a5 else (RR if flags_a5 else AP)) if hubo_adaptacion_ia else NA
    if not hubo_adaptacion_ia and a5:
        est["A5_adaptacion_ia"] = FA
    if not hubo_adaptacion_ia:
        det["notas"]["A5_adaptacion_ia"].append("ningun ciclo ordeno una adaptacion que requiera IA")
    est["A6_mantenimiento"] = (FA if a6 else AP) if hubo_mantenimiento else (FA if a6 else NA)
    if not bloque_p:
        if a7:
            est["A7_progresion"] = FA
        elif filas_pasos:
            est["A7_progresion"] = RR if flags_a7 else AP   # D-OR-1: recuperaciones P-A1 evaluadas con sus reglas
        else:
            est["A7_progresion"] = NA
            det["notas"]["A7_progresion"].append("progresion apagada en su bloque (G); no hubo ningun paso")
    else:
        hubo_pasos = bool(filas_pasos)
        if a7:
            est["A7_progresion"] = FA
        elif det["notas"]["A7_progresion"] and not hubo_pasos:
            est["A7_progresion"] = FA
            a7.extend(det["notas"]["A7_progresion"])
        elif hubo_pasos or hubo_progresion_esperable:
            est["A7_progresion"] = RR if flags_a7 else AP
        else:
            est["A7_progresion"] = NA
            det["notas"]["A7_progresion"].append("las condiciones de dos semanas no se cumplieron en el horizonte")
    reinc = det.get("reincorporaciones", [])
    if a8:
        est["A8_reincorporacion"] = FA
    elif reinc:
        est["A8_reincorporacion"] = AP
        det["notas"]["A8_reincorporacion"].append("positiva: reincorporacion observada y valida")
    elif retirados and not bloque_p:
        est["A8_reincorporacion"] = AP
        det["notas"]["A8_reincorporacion"].append(
            f"negativa: retirados {sorted(retirados)} sin reincorporar con la progresion apagada")
    elif excluidos_dolor:
        est["A8_reincorporacion"] = AP
        det["notas"]["A8_reincorporacion"].append(
            f"negativa: excluidos por dolor {sorted(excluidos_dolor)} no vuelven")
    elif retirados:
        est["A8_reincorporacion"] = NA
        det["notas"]["A8_reincorporacion"].append(
            f"retirados {sorted(retirados)} pendientes: la reincorporacion no se alcanzo en el horizonte")
    else:
        est["A8_reincorporacion"] = NA
        det["notas"]["A8_reincorporacion"].append("ningun ejercicio retirado ni excluido")
    est["A9_continuidad"] = AP if not a9 else FA
    est["A10_seguridad_integridad"] = AP if not a10 else FA
    if est["A1_generacion"] == AP and flags_a1:
        est["A1_generacion"] = RR
    det["revision_catalogo"] = {"A1": sorted(flags_a1), "A5": sorted(flags_a5), "A7": sorted(flags_a7)}
    det["estados"] = est
    return det


def afinar_por_causa(objetivo, sesiones_plan):
    """FaltaDeTiempoPolicy (documentada): una OMISION por falta de tiempo sin la respuesta MENOS_TIEMPO_HABITUAL con
    minutos, o con minutos suficientes, se CONSERVA (sin IA); una parcial por falta de tiempo se sigue acortando."""
    por_id = {int(w["planned_workout_id"]): w for w in sesiones_plan}
    out = []
    for wid, interv in objetivo:
        w = por_id.get(wid)
        if interv == "ACORTAR_O_REORGANIZAR" and w is not None and w["status"] == "SKIPPED" \
                and w["skip_reason"] == "LACK_OF_TIME":
            m = H.entero(w.get("declared_available_minutes"))
            if w.get("lack_of_time_context") != "MENOS_TIEMPO_HABITUAL" or m is None \
                    or m >= H.entero(w.get("expected_duration_minutes"), 0):
                interv = "CONSERVAR"
        out.append((wid, interv))
    return out


def pertinencia(intervencion, d, conservada_justificada):
    """Tabla de pertinencia del protocolo (oraculo historico) + ACORTAR_O_REORGANIZAR, que la tabla historica no
    contemplaba: acortar es pertinente si quita trabajo (retira o reduce) sin endurecer nada."""
    if intervencion != "ACORTAR_O_REORGANIZAR":
        return H.pertinencia(intervencion, d, conservada_justificada)
    if d["igual"]:
        return "CONS-J" if conservada_justificada else "INC"
    mas_trabajo = d["trabajo_despues"] > d["trabajo_antes"] or bool(d["aumentos"]) or bool(d["anadidos"])
    mas_dificil = any(s["dif_a"] is not None and s["dif_de"] is not None and s["dif_a"] > s["dif_de"]
                      for s in d["sustituciones"])
    if mas_trabajo or mas_dificil:
        return "INC"
    return "ACC" if (d["retirados"] or d["reducciones"]) and d["trabajo_despues"] < d["trabajo_antes"] else "ALT"


def comprobar_paso(C, g, w, k):
    """A3: lo registrado coincide con el paso del guion."""
    D = C.D
    paso = g["paso"]
    wid = int(w["planned_workout_id"])
    if int(g["planned_workout_id"]) != wid:
        return [f"S{k} sesion {g['orden']}: guion sobre {g['planned_workout_id']} y la sesion es {wid}"]
    intentos = [s for s in D.ses.get(wid, []) if s["counts_toward_adherence"] == "true"]
    out = []
    ej = C.ej(w)
    if paso in ("H", "P40", "P60", "HF", "HD", "PT"):
        if len(intentos) != 1:
            return [f"S{k} sesion {g['orden']} ({paso}): {len(intentos)} sesiones que cuentan (esperada 1)"]
        filas = D.wse.get(int(intentos[0]["session_id"]), {})
        n = len(ej)
        for i, e in enumerate(ej):
            total = H.dosis_total(e)
            causa = None
            if paso == "H":
                esp = total
            elif paso in ("P40", "P60"):
                esp = H.round_java(total * (0.4 if paso == "P40" else 0.6))
            elif paso in ("HF", "HD"):
                ultimo = i == n - 1
                esp = total // 2 if ultimo else total
                causa = ("FATIGUE" if paso == "HF" else "PAIN_OR_DISCOMFORT") if ultimo else None
            else:  # PT
                parte = max(0.0, min(1.0, 0.35 * n - i))
                esp = H.round_java(total * parte)
                causa = None if parte >= 1 else "LACK_OF_TIME"
            f = filas.get(e["pe"])
            real = None if f is None else H.entero(f["actual_duration_seconds"] if e["type"] == "DURATION"
                                                  else f["actual_reps_total"])
            if real != esp:
                out.append(f"S{k} sesion {g['orden']} ({paso}) ej {e['id']}: registrado {real}, esperado {esp}")
            if causa is not None and (f is None or f.get("skip_reason") != causa):
                out.append(f"S{k} sesion {g['orden']} ({paso}) ej {e['id']}: causa {f and f.get('skip_reason')} != {causa}")
    elif paso.startswith("O:"):
        if w["status"] != "SKIPPED" or w["skip_reason"] != paso[2:]:
            out.append(f"S{k} sesion {g['orden']}: omision {w['status']}/{w['skip_reason']} != SKIPPED/{paso[2:]}")
        if intentos:
            out.append(f"S{k} sesion {g['orden']}: omitida con sesion que cuenta")
    elif paso == "N":
        if intentos or (w["status"] == "SKIPPED" and w["skip_reason"]):
            out.append(f"S{k} sesion {g['orden']}: sin registro esperado y hay registro")
    else:
        out.append(f"S{k} sesion {g['orden']}: paso desconocido {paso}")
    return out


def sesiones_progresables(C, k, sess_k, sesiones_por_semana, vigentes, linaje, raiz, pasos_por_linaje, adh100):
    """Sesiones de la semana k que cumplen el criterio de dos semanas (D-P1, D-P2) recalculado."""
    out = []
    if not adh100.get(k) or not adh100.get(k - 1):
        return out
    prev = {s["id"]: s for s in sesiones_por_semana.get(k - 1, [])}
    actual = {s["id"]: s for s in sesiones_por_semana.get(k, [])}
    for w in sess_k:
        s = actual.get(int(w["planned_workout_id"]))
        o = prev.get(int(w["source_planned_workout_id"] or 0))
        if not s or not o or s["pct"] != Decimal(100) or o["pct"] != Decimal(100):
            continue
        if s["fatiga"] or s["dolor"] or o["fatiga"] or o["dolor"]:
            continue
        pos = linaje.get(int(w["planned_workout_id"]))
        if any(p[0] == k - 1 for p in pasos_por_linaje.get(pos, [])):
            continue  # O -> S fue un paso: las dos semanas cuentan desde el ultimo
        if pos is not None and volumen(C.ej(w)) >= int((Decimal(volumen(raiz[pos])) * TECHO)
                                                         .to_integral_value(rounding="ROUND_FLOOR")):
            continue
        if not any(e["type"] == "SETS_REPS" for e in C.ej(w)):
            continue
        out.append(int(w["planned_workout_id"]))
    return out


def comprobar_paso_progresion(C, k, w, n, antes, despues, paso, raiz, linaje, retirados, pasos_por_linaje,
                              sesiones_por_semana, vigentes, filas_pasos, pid, flags, excluidos_dolor):
    """A7 (y la parte de A8 que depende del informe): el paso registrado es el unico cambio y cumple sus reglas."""
    out = []
    tipo = paso.get("tipo")
    pos = linaje.get(int(w["planned_workout_id"]))
    eid = int(paso.get("exercise_id") or -1)
    pasos_por_linaje.setdefault(pos, []).append((k, tipo, eid))
    base = volumen(raiz.get(pos, [])) if pos is not None else None
    techo = int((Decimal(base) * TECHO).to_integral_value(rounding="ROUND_FLOOR")) if base is not None else None
    fila = {"id": pid, "ciclo": k, "tipo": tipo, "sesion_origen": int(w["planned_workout_id"]), "exercise_id": eid,
            "antes": json.dumps(paso.get("antes"), sort_keys=True), "despues": json.dumps(paso.get("despues"), sort_keys=True),
            "volumen_antes": volumen(antes), "volumen_despues": volumen(despues), "volumen_base_oraculo": base,
            "techo_oraculo": techo, "techo_backend": paso.get("techo"), "volumen_base_backend": paso.get("volumen_base"),
            "estado_reserva": paso.get("estado_reserva"), "condiciones": "", "problemas": ""}
    if eid in NORDICOS:
        flags.add(f"paso sobre un curl nordico {eid}")
    prev = {s["id"]: s for s in sesiones_por_semana.get(k - 1, [])}
    act = {s["id"]: s for s in sesiones_por_semana.get(k, [])}
    s = act.get(int(w["planned_workout_id"]))
    o = prev.get(int(w["source_planned_workout_id"] or 0))
    cond = []
    if tipo in ("PROGRESION",):
        if k < 2 or not s or not o:
            out.append(f"C{k}: PROGRESION sin dos semanas medidas")
        else:
            if s["pct"] != Decimal(100) or o["pct"] != Decimal(100):
                out.append(f"C{k}: PROGRESION con cumplimiento S {s['pct']} / O {o['pct']} (se exige 100 %)")
            if s["fatiga"] or s["dolor"] or o["fatiga"] or o["dolor"]:
                out.append(f"C{k}: PROGRESION con fatiga o dolor en S u O")
            cond.append(f"S={s['pct']} O={o['pct']}")
        if any(p[0] == k - 1 for p in pasos_por_linaje.get(pos, [])[:-1]):
            out.append(f"C{k}: PROGRESION justo despues de otro paso en el mismo linaje (O->S no fue una copia)")
        cambiados = [(a, b) for a, b in zip(antes, despues) if dosis(a) != dosis(b)]
        if len(antes) != len(despues) or len(cambiados) != 1:
            out.append(f"C{k}: PROGRESION cambia {len(cambiados)} ejercicios (se espera 1) o la lista")
        else:
            a, b = cambiados[0]
            ok = a["id"] == b["id"] == eid and a["rest"] == b["rest"] and (
                (b["reps"] is not None and a["reps"] is not None and b["reps"] == a["reps"] + 1 and b["sets"] == a["sets"])
                or (b["sets"] is not None and a["sets"] is not None and b["sets"] == a["sets"] + 1 and b["reps"] == a["reps"]))
            if not ok:
                out.append(f"C{k}: PROGRESION no es +1 repeticion o +1 serie sobre {eid}: {dosis(a)} -> {dosis(b)}")
            ini = next((e for e in raiz.get(pos, []) if e["id"] == eid), None)
            if ini is not None and b["reps"] is not None and ini["reps"] is not None and b["reps"] > ini["reps"] + 2 \
                    and b["sets"] == ini["sets"]:
                out.append(f"C{k}: PROGRESION supera la dosis exacta + 2 repeticiones (D-P3): {ini['reps']} -> {b['reps']}")
    elif tipo == "RECUPERACION":
        cambiados = [(a, b) for a, b in zip(antes, despues) if dosis(a) != dosis(b)]
        for a, b in cambiados:
            ini = next((e for e in raiz.get(pos, []) if e["id"] == b["id"]), None)
            if ini is not None and (H.dosis_total(b) > H.dosis_total(ini)):
                out.append(f"C{k}: RECUPERACION por encima de la referencia {dosis(ini)} -> {dosis(b)}")
    elif tipo == "REINCORPORACION":
        if eid in excluidos_dolor:
            out.append(f"C{k}: reincorpora un ejercicio excluido por dolor ({eid})")
        if eid not in retirados:
            out.append(f"C{k}: REINCORPORACION de {eid}, que no fue retirado por ACORTAR")
    else:
        cond.append(f"tipo {tipo}")
    if techo is not None and volumen(despues) > techo:
        out.append(f"C{k}: volumen {volumen(despues)} > techo {techo} (1,10 x {base})")
    if tipo != "REINCORPORACION":
        ids_a, ids_d = [e["id"] for e in antes], [e["id"] for e in despues]
        if ids_a != ids_d:
            out.append(f"C{k}: el paso {tipo} cambia la lista de ejercicios {ids_a} -> {ids_d}")
    fila["condiciones"] = "; ".join(cond)
    fila["problemas"] = " | ".join(out) or "-"
    filas_pasos.append(fila)
    return out


# ======================================================================== huella

def huella(C, pid, u):
    D = C.D
    pos = {}
    planes = sorted([p for p in D.planes if int(p["user_id"]) == u and not p["replaced_at"]],
                    key=lambda p: p["week_start_date"])
    for p in planes:
        for i, w in enumerate(C.ses(p)):
            pos[int(w["planned_workout_id"])] = f"{p['week_start_date']}#{i + 1}"
    semanas = []
    for p in planes:
        ses = []
        for w in C.ses(p):
            wid = int(w["planned_workout_id"])
            intentos = [s for s in D.ses.get(wid, []) if s["counts_toward_adherence"] == "true"]
            hecho = None
            if intentos:
                filas = D.wse.get(int(intentos[-1]["session_id"]), {})
                hecho = [[f["actual_sets"], f["actual_reps_total"], f["actual_duration_seconds"],
                          f["completion_percentage"], f["skip_reason"]] for _, f in sorted(filas.items())]
            ses.append({"fecha": w["scheduled_date"], "foco": w["focus_code"], "min": w["expected_duration_minutes"],
                        "estado": w["status"], "omision": w["skip_reason"],
                        "origen": pos.get(int(w["source_planned_workout_id"] or 0)),
                        "ejercicios": [list(dosis(e)) for e in C.ej(w)], "ejecucion": hecho})
        m = D.metricas.get((u, p["week_start_date"]), {})
        semanas.append({"semana": p["week_start_date"], "fuente": p["generation_source"], "estado": p["status"],
                        "version": p["plan_version"], "sesiones": ses,
                        "metricas": [m.get(k) for k in ("weighted_adherence_pct", "observed_adherence_pct",
                                                        "registration_coverage_pct")]})
    v32 = []
    for r in sorted(D.v32.get(u, []), key=lambda r: (r["new_week_start_date"], int(r["attempt_id"]))):
        try:
            inf = json.loads(r["conservation_report"] or "{}")
        except ValueError:
            inf = {}
        v32.append([r["decision"], r["status"], r["generation_source"], r["ai_calls"], r["new_week_start_date"],
                    [pos.get(x, x) for x in lista(r["targeted_sessions"])], r["targeted_interventions"],
                    [pos.get(x, x) for x in lista(r["applied_sessions"])],
                    [pos.get(x, x) for x in lista(r["progressed_sessions"])],
                    [[p.get("tipo"), p.get("exercise_id"), p.get("antes"), p.get("despues")] for p in inf.get("pasos") or []],
                    r["reason_codes"], r["rejection_reason"]])
    return {"id": pid, "semanas": semanas, "v32": v32}


# ======================================================================== principal

def main():
    perfiles_csv, salida = sys.argv[1], sys.argv[2]
    corridas = [Corrida(c) for c in sys.argv[3:]]
    perfiles = {r["id"]: r for r in leer(perfiles_csv)}
    os.makedirs(salida, exist_ok=True)
    resultados, canon = [], {}
    vistos = {}
    for C in corridas:
        for pid, u in C.D.usuarios.items():
            if pid in vistos:
                sys.exit(f"{pid} aparece en dos corridas: {vistos[pid]} y {C.carpeta}")
            vistos[pid] = C.carpeta
            det = evaluar(C, pid, perfiles[pid], u, C.D.res.get(pid, {}))
            resultados.append(det)
            canon[pid] = huella(C, pid, u)
    faltan = sorted(set(perfiles) - set(vistos))
    resultados.sort(key=lambda d: d["id"])
    with open(os.path.join(salida, "matriz_10_aspectos.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["id", "bloque", "escenario"] + ASPECTOS + ["motivos_fallo", "revision_catalogo", "notas"])
        for d in resultados:
            p = perfiles[d["id"]]
            fallos = [f"{a}: {x}" for a in ASPECTOS for x in d["checks"][a]] if any(
                d["estados"].get(a) == FA for a in ASPECTOS) else []
            rc = "; ".join(f"{k}: {', '.join(v)}" for k, v in d.get("revision_catalogo", {}).items() if v)
            notas = "; ".join(f"{a}: {x}" for a in ASPECTOS for x in d["notas"][a])
            w.writerow([d["id"], p["bloque"], p["escenario"]] + [d["estados"].get(a, NA) for a in ASPECTOS]
                       + [" | ".join(fallos)[:4000], rc, notas[:4000]])
    def volcar(nombre, filas):
        if not filas:
            open(os.path.join(salida, nombre), "w").close()
            return
        campos = list(filas[0].keys())
        with open(os.path.join(salida, nombre), "w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=campos, lineterminator="\n")
            w.writeheader()
            for r in filas:
                w.writerow({k: r.get(k) for k in campos})
    volcar("semanas.csv", [s for d in resultados for s in d.get("semanas", [])])
    volcar("adaptaciones.csv", [s for d in resultados for s in d.get("adaptaciones", [])])
    volcar("pasos_progresion.csv", [s for d in resultados for s in d.get("pasos", [])])
    with open(os.path.join(salida, "detalle.json"), "w", encoding="utf-8") as f:
        json.dump(resultados, f, ensure_ascii=False, indent=1, default=str, sort_keys=True)
    texto = json.dumps([canon[k] for k in sorted(canon)], sort_keys=True, ensure_ascii=False, default=str)
    hh = hashlib.sha256(texto.encode("utf-8")).hexdigest()
    with open(os.path.join(salida, "huella_normalizada.txt"), "w") as f:
        f.write(hh + "\n")
    tot = Counter(d["estados"].get(a, NA) for d in resultados for a in ASPECTOS)
    aplicables = tot[AP] + tot[FA]
    lineas = ["# Oraculo independiente · evaluacion integral O", "",
              f"- Corridas: {', '.join(os.path.basename(C.carpeta) for C in corridas)}",
              f"- Perfiles evaluados: {len(resultados)}; sin evaluar: {faltan or 'ninguno'}",
              f"- Huella normalizada: `{hh}`",
              f"- Totales (20 x 10 = {len(resultados) * 10}): {dict(tot)}",
              f"- Aprobacion tecnica sobre aplicables (APROBADO / (APROBADO + FALLO)): {tot[AP]}/{aplicables}"
              + (f" = {100 * tot[AP] / aplicables:.1f} %" if aplicables else ""), ""]
    lineas.append("| Aspecto | APROBADO | FALLO | NO APLICA | REQUIERE REVISION |")
    lineas.append("|---|---|---|---|---|")
    for a in ASPECTOS:
        c = Counter(d["estados"].get(a, NA) for d in resultados)
        lineas.append(f"| {a} | {c[AP]} | {c[FA]} | {c[NA]} | {c[RR]} |")
    with open(os.path.join(salida, "resumen.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(lineas) + "\n")
    print("\n".join(lineas))


if __name__ == "__main__":
    main()
