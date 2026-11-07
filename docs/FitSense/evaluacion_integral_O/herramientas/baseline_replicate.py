#!/usr/bin/env python3
"""LINEA BASE SIN REGLAS (BASELINE_SIN_REGLAS) · cliente de Replicate REAL, aislado del backend.

Orden definitiva (07/11/2026). El modelo recibe el PERFIL y el CATALOGO FILTRADO del backend (los ejercicios elegibles
que FitSense recupera de PostgreSQL para ese perfil: PlanGenerationContext.availableExercises, volcado en solo lectura
por EvaluacionK20IT, con sus identificadores y atributos de catalogo). NO recibe ninguna regla interna de planificacion
ni de adaptacion: ni V1-V27/V24/V25/V8C, ni dosis, limites de series/repeticiones/volumen, patrones obligatorios (4C),
reparto o numero de ejercicios por sesion, division sugerida, frecuencia por nivel, descansos maximos, duraciones
internas, clases de dosis, slots, min_reps, candidatos de sustitucion ni politicas de conservacion; tampoco
retroalimentacion de validadores, ejemplos, reintentos guiados, normalizacion ni respaldo determinista. Sus respuestas
se GUARDAN tal cual; NUNCA se publican ni se aplican. Se cuentan despues, en solo lectura.

Mismos parametros que el backend (ReplicateTrainingPlanGenerator): modelo openai/gpt-5-structured, input
{model: gpt-5-mini, prompt, reasoning_effort: medium, json_schema}; Prefer: wait=25, sondeo cada 3 s, limite 150 s.
Version: se intenta fijar la de GRA (9f4cd9ec...) con POST /v1/predictions; si el proveedor la rechaza sin crear
prediccion, se usa el endpoint del modelo (version vigente) y se registra. El esquema es PlanJsonSchema con las
descripciones neutras (sin «exactamente N entrenamientos» ni «sin repetir exercise_id»).

Experimento A (generacion): un prompt por perfil O01-O20 (O11 incluido; sin bloqueo de factibilidad).
Experimento B (adaptacion): para cada ciclo con orden de IA de una corrida GRA: el plan publicado de esa semana (ids,
nombres y dosis), lo ocurrido en cada sesion (estado, motivo, marcas por ejercicio) y la peticion de proponer la semana
siguiente. No se envian las sesiones senaladas ni la intervencion del backend.

Sin reintentos por contenido. Un fallo DEL PROVEEDOR (prediccion failed/cancelada sin salida, o error HTTP) se reintenta
UNA vez, igual para todas las llamadas. El presupuesto se comprueba antes de cada prediccion con un registro compartido.

Uso:
  python3 -I baseline_replicate.py preparar <salida> <O_PERFILES.csv> <baseline_disponibles.csv> <tablas_catalogo_dir> \
          <GRA_G> <GRA_P>                          # escribe los prompts SIN llamar (para congelarlos)
  python3 -I baseline_replicate.py ejecutar <salida> <registro_presupuesto.json> <max_pred> <max_usd>
"""
import csv
import json
import os
import ssl
import sys
import time
import urllib.error
import urllib.request
from datetime import date, timedelta

csv.field_size_limit(sys.maxsize)
API = "https://api.replicate.com/v1/models/openai/gpt-5-structured/predictions"
MODELO, RAZONAMIENTO = "gpt-5-mini", "medium"
TARIFA_E, TARIFA_S, RESERVA = 0.25, 2.00, 0.05
S1 = date(2026, 11, 2)
DIAS = {1: "lunes", 2: "martes", 3: "miercoles", 4: "jueves", 5: "viernes", 6: "sabado", 7: "domingo"}
OBJ = {"GAIN_MUSCLE": "ganar masa muscular", "LOSE_WEIGHT": "perder peso", "INCREASE_STRENGTH": "aumentar la fuerza",
       "GENERAL_FITNESS": "mejorar la condicion fisica general"}
NIV = {"BEGINNER": "principiante", "INTERMEDIATE": "intermedio", "ADVANCED": "avanzado"}
SEXO = {"MALE": "hombre", "FEMALE": "mujer"}
RESTR = {"permite_alto_impacto": "No puede hacer ejercicios de alto impacto (saltos, carrera, golpes)",
         "permite_suelo": "No puede hacer ejercicios en el suelo",
         "permite_rodillas": "No puede apoyar las rodillas en el suelo (arrodillarse)",
         "permite_colgarse": "No puede hacer ejercicios colgado de una barra",
         "permite_por_encima_cabeza": "No puede hacer ejercicios con los brazos por encima de la cabeza"}
MOTIVO = {"PAIN_OR_DISCOMFORT": "dolor o molestia", "FATIGUE": "cansancio", "LACK_OF_TIME": "falta de tiempo",
          "TOO_DIFFICULT": "le resulto demasiado dificil", "LACK_OF_MOTIVATION": "falta de motivacion",
          "SCHEDULE_CHANGE": "cambio de horario", "OTHER": "otro motivo"}

VERSION_GRA = "9f4cd9ec1133f55d442aeb426e42df5180a56e79a33183623611d62d4c3b44ae"
CAMPOS_CATALOGO = ("exercise_id", "name", "body_part", "equipment", "difficulty", "prescription_type", "target_muscle",
                   "movement_pattern")

ESQUEMA_EJERCICIO = {"type": "object", "additionalProperties": False,
                     "properties": {"exercise_id": {"type": "integer"}, "exercise_order": {"type": "integer"},
                                    "prescription_type": {"type": "string", "enum": ["SETS_REPS", "DURATION"]},
                                    "planned_sets": {"type": ["integer", "null"]}, "planned_reps": {"type": ["integer", "null"]},
                                    "planned_duration_seconds": {"type": ["integer", "null"]},
                                    "target_load_kg": {"type": ["number", "null"]}, "rest_seconds": {"type": ["integer", "null"]},
                                    "notes": {"type": ["string", "null"]}},
                     "required": ["exercise_id", "exercise_order", "prescription_type", "planned_sets", "planned_reps",
                                  "planned_duration_seconds", "target_load_kg", "rest_seconds", "notes"]}
ESQUEMA = {"format": {"type": "json_schema", "name": "weekly_training_plan", "schema": {
    "type": "object", "additionalProperties": False,
    "properties": {"schema_version": {"type": "string"}, "plan_name": {"type": "string"},
                   "total_volume": {"type": "integer"}, "rationale": {"type": "string"},
                   "workouts": {"type": "array", "description": "Los entrenamientos de la semana.", "items": {
                       "type": "object", "additionalProperties": False,
                       "properties": {"scheduled_date": {"type": "string"},
                                      "focus_code": {"type": "string", "enum": ["FULL_BODY", "UPPER_BODY", "LOWER_BODY",
                                                                                "PUSH", "PULL", "LEGS", "CORE"]},
                                      "workout_name": {"type": "string"},
                                      "expected_duration_minutes": {"type": "integer"},
                                      "exercises": {"type": "array", "description": "Los ejercicios de esa sesion.",
                                                    "items": ESQUEMA_EJERCICIO}},
                       "required": ["scheduled_date", "focus_code", "workout_name", "expected_duration_minutes",
                                    "exercises"]}}},
    "required": ["schema_version", "plan_name", "total_volume", "rationale", "workouts"]}}}

INSTRUCCION_GEN = """Eres un entrenador personal. Diseña el plan de entrenamiento de la semana del {ini} al {fin} para la persona descrita abajo.
Elige los ejercicios de la lista EJERCICIOS DISPONIBLES (el catalogo de la aplicacion para esta persona) e indica su exercise_id.
Para cada entrenamiento indica la fecha, el enfoque, un nombre, la duracion prevista en minutos y los ejercicios. Para cada ejercicio indica el orden, el tipo de prescripcion (series y repeticiones, o duracion), las series, las repeticiones o los segundos, la carga si procede y el descanso en segundos.
Devuelve UNICAMENTE el objeto JSON del esquema.

PERSONA
{perfil}

EJERCICIOS DISPONIBLES ({n})
{catalogo}
"""

INSTRUCCION_ADA = """Eres un entrenador personal. Abajo tienes a una persona, el plan que tenia programado la semana del {ini_k} al {fin_k} y lo que ocurrio en cada sesion.
Propon su plan de entrenamiento para la semana siguiente, del {ini_s} al {fin_s}, ajustandolo como consideres oportuno.
Elige los ejercicios de la lista EJERCICIOS DISPONIBLES (el catalogo de la aplicacion para esta persona) e indica su exercise_id.
Usa el mismo formato: para cada entrenamiento la fecha, el enfoque, un nombre, la duracion prevista y los ejercicios (orden, tipo de prescripcion, series, repeticiones o segundos, carga si procede y descanso).
Devuelve UNICAMENTE el objeto JSON del esquema.

PERSONA
{perfil}

PLAN DE LA SEMANA DEL {ini_k} AL {fin_k}
{plan}

LO QUE OCURRIO
{ocurrio}

EJERCICIOS DISPONIBLES ({n})
{catalogo}
"""


def leer(r):
    with open(r, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def edad(nac, ref=S1):
    n = date.fromisoformat(nac)
    return ref.year - n.year - ((ref.month, ref.day) < (n.month, n.day))


def bloque_perfil(p, ex, eq):
    dias = [int(x) for x in p["dias_disponibles"].split(" ")[0].split("|")]
    equipo = [f"{eq.get(c, c)} ({c})" for c in p["equipamiento"].split("|")]
    lineas = [f"- Objetivo: {OBJ[p['goal_type']]}.", f"- Nivel: {NIV[p['nivel']]}.",
              f"- Edad: {edad(p['nacimiento'])} años. Sexo: {SEXO.get(p['sexo'], p['sexo'])}. "
              f"Altura: {p['altura_cm']} cm. Peso: {p['peso_kg']} kg."
              + (f" Peso objetivo: {p['peso_objetivo_kg']} kg." if p.get("peso_objetivo_kg") else ""),
              f"- Lugar: {'gimnasio' if p['ubicacion'] == 'GYM' else 'casa'}. Material disponible: {', '.join(equipo)}.",
              f"- Dias disponibles: {', '.join(DIAS[d] for d in dias)}. Quiere entrenar {p['dias_por_semana']} "
              f"dia(s) por semana, {p['minutos_sesion']} minutos por sesion como maximo."]
    restr = [t for k, t in RESTR.items() if p.get(k, "true") == "false"]
    bloq = [ex[i]["name_es"] for i in (p.get("ejercicios_bloqueados") or "").split("|") if i and i in ex]
    if bloq:
        restr.append("No puede o no quiere hacer estos ejercicios: " + "; ".join(bloq))
    lineas.append("- Restricciones declaradas: " + ("; ".join(restr) + "." if restr else "ninguna."))
    lineas.append(f"- Notas de salud: {p.get('notas_salud') or 'ninguna'}.")
    return "\n".join(lineas)


def bloque_catalogo(filas):
    """Una linea JSON por ejercicio elegible, con los atributos de catalogo que viajan en available_exercises. Se
    omiten los campos que FitSense calcula con sus reglas (min_reps, slot, clase_dosis, dosis)."""
    out = []
    for r in filas:
        d = {k: r[k] for k in CAMPOS_CATALOGO}
        d["exercise_id"], d["difficulty"] = int(d["exercise_id"]), int(d["difficulty"])
        out.append(json.dumps({k: (v if v != "" else None) for k, v in d.items()}, ensure_ascii=False))
    return "\n".join(out)


def preparar(salida, perfiles_csv, disponibles_csv, tablas_cat, gra_dirs):
    os.makedirs(salida, exist_ok=True)
    ex = {r["exercise_id"]: r for r in leer(os.path.join(tablas_cat, "exercises.csv"))}
    eq = {r["code"]: r["name_es"] for r in leer(os.path.join(tablas_cat, "equipment_types.csv"))}
    perfiles = {r["id"]: r for r in leer(perfiles_csv)}
    catalogo = {}
    for r in leer(disponibles_csv):
        catalogo.setdefault(r["id"], []).append(r)
    assert set(perfiles) == set(catalogo), "catalogo filtrado incompleto"
    llamadas = []
    for pid, p in perfiles.items():
        prompt = INSTRUCCION_GEN.format(ini=S1.isoformat(), fin=(S1 + timedelta(days=6)).isoformat(),
                                        perfil=bloque_perfil(p, ex, eq), n=len(catalogo[pid]),
                                        catalogo=bloque_catalogo(catalogo[pid]))
        llamadas.append({"etiqueta": f"{pid}/generacion", "experimento": "A_GENERACION", "id": pid, "prompt": prompt})
    # Experimento B: ciclos con orden de IA de la corrida GRA (sus planes publicados y su ejecucion)
    contexto_b = []
    for d in gra_dirs:
        t = lambda n: leer(os.path.join(d, "tablas", n + ".csv"))
        us = {r["user_id"]: r["id"] for r in leer(os.path.join(d, "usuarios.csv"))}
        pw = t("planned_workouts")
        pwe = t("planned_workout_exercises")
        ses = t("workout_sessions")
        wse = t("workout_session_exercises")
        exd = {r["exercise_id"]: r for r in t("exercises")}
        for a in t("plan_adaptation_attempts"):
            if int(a["ai_calls"] or 0) == 0 or a["user_id"] not in us:
                continue
            pid = us[a["user_id"]]
            w_k = sorted([w for w in pw if w["plan_id"] == a["source_plan_id"]],
                         key=lambda w: (w["scheduled_date"], int(w["display_order"] or 0)))
            ini_k = date.fromisoformat(w_k[0]["scheduled_date"]) - timedelta(days=date.fromisoformat(w_k[0]["scheduled_date"]).weekday())
            ini_s = date.fromisoformat(a["new_week_start_date"])
            k = (ini_s - S1).days // 7
            plan_txt, ocurrio = [], []
            sesiones = []
            for w in w_k:
                ejs = sorted([e for e in pwe if e["planned_workout_id"] == w["planned_workout_id"]],
                             key=lambda e: int(e["exercise_order"]))
                plan_txt.append(f"* {w['scheduled_date']} ({DIAS[date.fromisoformat(w['scheduled_date']).isoweekday()]}), "
                                f"{w['focus_code']}, {w['expected_duration_minutes']} min:")
                for e in ejs:
                    c = exd[e["exercise_id"]]
                    dosis = (f"{e['planned_sets']} series x {e['planned_reps']} repeticiones" if e["prescription_type"] == "SETS_REPS"
                             else f"{e['planned_sets'] or 1} series x {e['planned_duration_seconds']} s")
                    plan_txt.append(f"    {e['exercise_order']}. [exercise_id {e['exercise_id']}] {c['name_es']}: {dosis}, "
                                    f"descanso {e['rest_seconds']} s")
                s = [x for x in ses if x["planned_workout_id"] == w["planned_workout_id"] and x["counts_toward_adherence"] == "true"]
                fecha = w["scheduled_date"]
                if w["status"] == "SKIPPED":
                    ocurrio.append(f"* {fecha}: no la hizo ({MOTIVO.get(w['skip_reason'], 'sin motivo indicado')}).")
                elif s:
                    s = s[-1]
                    marcas = []
                    for x in [y for y in wse if y["session_id"] == s["session_id"]]:
                        e = next((e for e in ejs if e["planned_exercise_id"] == x["planned_exercise_id"]), None)
                        if e and x["skip_reason"]:
                            marcas.append(f"en «{exd[e['exercise_id']]['name_es']}»: {MOTIVO.get(x['skip_reason'], x['skip_reason'])}")
                        elif e and x["status"] in ("SKIPPED",):
                            marcas.append(f"no hizo «{exd[e['exercise_id']]['name_es']}»")
                    pct = float(s["completion_percentage"] or 0)
                    estado = "la completo" if pct >= 99.99 else f"la hizo en parte ({pct:.0f} %)"
                    ocurrio.append(f"* {fecha}: {estado}" + (" — " + "; ".join(marcas) if marcas else "") + ".")
                else:
                    ocurrio.append(f"* {fecha}: sin registro.")
                sesiones.append({"planned_workout_id": w["planned_workout_id"], "fecha": fecha})
            prompt = INSTRUCCION_ADA.format(
                ini_k=ini_k.isoformat(), fin_k=(ini_k + timedelta(days=6)).isoformat(), ini_s=ini_s.isoformat(),
                fin_s=(ini_s + timedelta(days=6)).isoformat(), perfil=bloque_perfil(perfiles[pid], ex, eq),
                plan="\n".join(plan_txt), ocurrio="\n".join(ocurrio), n=len(catalogo[pid]),
                catalogo=bloque_catalogo(catalogo[pid]))
            et = f"{pid}/ciclo{k}"
            llamadas.append({"etiqueta": et, "experimento": "B_ADAPTACION", "id": pid, "prompt": prompt})
            contexto_b.append({"etiqueta": et, "id": pid, "ciclo": k, "gra_dir": os.path.basename(d.rstrip("/")),
                               "source_plan_id": a["source_plan_id"], "resulting_plan_id": a["resulting_plan_id"],
                               "semana_medida": ini_k.isoformat(), "semana_siguiente": ini_s.isoformat(),
                               "sesiones_senaladas_backend": a["targeted_sessions"],
                               "intervenciones_backend": a["targeted_interventions"], "decision_backend": a["decision"],
                               "estado_v32_gra": a["status"], "sesiones": sesiones})
    with open(os.path.join(salida, "llamadas_planificadas.json"), "w", encoding="utf-8") as f:
        json.dump(llamadas, f, ensure_ascii=False, indent=1)
    with open(os.path.join(salida, "contexto_adaptacion.json"), "w", encoding="utf-8") as f:
        json.dump(contexto_b, f, ensure_ascii=False, indent=1)
    os.makedirs(os.path.join(salida, "prompts"), exist_ok=True)
    for l in llamadas:
        with open(os.path.join(salida, "prompts", l["etiqueta"].replace("/", "_") + ".txt"), "w", encoding="utf-8") as f:
            f.write(l["prompt"])
    print(f"{len(llamadas)} llamadas preparadas ({sum(1 for l in llamadas if l['experimento'] == 'A_GENERACION')} de "
          f"generacion, {len(contexto_b)} de adaptacion)")


# ------------------------------------------------------------------ llamadas reales
CTX = ssl.create_default_context(cafile="/root/.ccr/ca-bundle.crt") if os.path.exists("/root/.ccr/ca-bundle.crt") \
    else ssl.create_default_context()


def http(metodo, url, cuerpo=None, cabeceras=None):
    req = urllib.request.Request(url, data=None if cuerpo is None else json.dumps(cuerpo).encode("utf-8"), method=metodo)
    req.add_header("Content-Type", "application/json")
    for k, v in (cabeceras or {}).items():
        req.add_header(k, v)
    with urllib.request.urlopen(req, context=CTX, timeout=90) as r:
        return json.loads(r.read().decode("utf-8"))


def version_modelo():
    d = http("GET", "https://api.replicate.com/v1/models/openai/gpt-5-structured")
    return d["latest_version"]["id"]


class VersionRechazada(Exception):
    pass


def una_prediccion(prompt, fijar):
    """fijar=True: POST /v1/predictions con la version de GRA. Si el proveedor la rechaza con un 4xx (sin crear
    prediccion), se lanza VersionRechazada y no se cuenta como prediccion. fijar=False: endpoint del modelo."""
    t0 = time.monotonic()
    entrada = {"model": MODELO, "prompt": prompt, "reasoning_effort": RAZONAMIENTO, "json_schema": ESQUEMA}
    if fijar:
        try:
            p = http("POST", "https://api.replicate.com/v1/predictions", {"version": VERSION_GRA, "input": entrada},
                     {"Prefer": "wait=25"})
        except urllib.error.HTTPError as e:
            if 400 <= e.code < 500 and e.code != 429:
                raise VersionRechazada(f"HTTP {e.code}: {e.read().decode('utf-8', 'replace')[:300]}")
            raise
    else:
        p = http("POST", API, {"input": entrada}, {"Prefer": "wait=25"})
    while p.get("status") in ("starting", "processing") and time.monotonic() - t0 < 150:
        time.sleep(3)
        p = http("GET", p["urls"]["get"])
    if p.get("status") in ("starting", "processing"):
        try:
            http("POST", p["urls"]["cancel"])
        except Exception:
            pass
        p = http("GET", p["urls"]["get"])
    ms = int((time.monotonic() - t0) * 1000)
    out = p.get("output")
    salida = "".join(out) if isinstance(out, list) else (out if isinstance(out, str) else json.dumps(out) if out else "")
    return p, salida, ms


def ejecutar(salida, registro, max_pred, max_usd):
    llamadas = json.load(open(os.path.join(salida, "llamadas_planificadas.json"), encoding="utf-8"))
    reg = json.load(open(registro)) if os.path.exists(registro) else {"predicciones": 0, "usd": 0.0, "detalle": []}
    os.makedirs(os.path.join(salida, "respuestas"), exist_ok=True)
    filas = []
    v_antes = version_modelo()
    fijar = True   # se intenta la version de GRA; si el proveedor la rechaza, version vigente del endpoint del modelo
    modo_txt = ""
    for l in llamadas:
        destino = os.path.join(salida, "respuestas", l["etiqueta"].replace("/", "_") + ".json")
        if os.path.exists(destino):
            continue   # ya obtenida (nunca se repite una respuesta por su contenido)
        intento, resultado = 0, None
        while intento < 2:   # 1 llamada + 1 reintento SOLO por fallo del proveedor
            if reg["predicciones"] + 1 > max_pred or reg["usd"] + RESERVA > max_usd:
                resultado = {"estado": "NO_EJECUTADA_PRESUPUESTO"}
                break
            intento += 1
            reg["predicciones"] += 1
            try:
                try:
                    p, out, ms = una_prediccion(l["prompt"], fijar)
                except VersionRechazada as e:
                    fijar = False
                    modo_txt = f"version GRA {VERSION_GRA} rechazada por el proveedor sin crear prediccion ({e}); se usa la vigente del endpoint del modelo"
                    print(modo_txt, flush=True)
                    p, out, ms = una_prediccion(l["prompt"], fijar)
                m = p.get("metrics") or {}
                te, ts = m.get("token_input_count"), m.get("token_output_count")
                usd = (te * TARIFA_E + ts * TARIFA_S) / 1e6 if te is not None and ts is not None else RESERVA
                reg["usd"] += usd
                resultado = {"estado": p.get("status"), "prediction_id": p.get("id"), "tokens_entrada": te,
                             "tokens_salida": ts, "predict_time_s": m.get("predict_time"), "ms": ms, "usd": round(usd, 6),
                             "error": p.get("error"), "salida": out, "version": p.get("version"), "intento": intento,
                             "modo_version": "FIJADA_GRA" if fijar else "VIGENTE_ENDPOINT_MODELO"}
                reg["detalle"].append({"etiqueta": l["etiqueta"], "prediction_id": p.get("id"), "usd": round(usd, 6)})
                if p.get("status") == "succeeded" and out:
                    break
            except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError, KeyError, ValueError) as e:
                reg["usd"] += RESERVA
                resultado = {"estado": "ERROR_PROVEEDOR", "error": f"{type(e).__name__}: {e}", "intento": intento}
            with open(registro, "w") as f:
                json.dump(reg, f, indent=1)
        resultado = resultado or {"estado": "SIN_RESULTADO"}
        resultado.update({"etiqueta": l["etiqueta"], "experimento": l["experimento"], "id": l["id"]})
        with open(destino, "w", encoding="utf-8") as f:
            json.dump(resultado, f, ensure_ascii=False, indent=1)
        filas.append({k: resultado.get(k) for k in ("etiqueta", "experimento", "estado", "prediction_id", "tokens_entrada",
                                                    "tokens_salida", "ms", "usd", "intento", "version", "modo_version")})
        print(l["etiqueta"], resultado.get("estado"), resultado.get("usd"), resultado.get("ms"), flush=True)
        if resultado.get("estado") == "NO_EJECUTADA_PRESUPUESTO":
            break
    v_despues = version_modelo()
    with open(os.path.join(salida, "versiones.txt"), "a") as f:
        f.write(f"vigente_antes {v_antes}\nvigente_despues {v_despues}\nmodo {'FIJADA_GRA' if fijar else 'VIGENTE_ENDPOINT_MODELO'} {modo_txt}\n")
    ruta = os.path.join(salida, "predicciones.csv")
    nuevo = not os.path.exists(ruta)
    with open(ruta, "a", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(filas[0]) if filas else ["etiqueta"], lineterminator="\n")
        if nuevo:
            w.writeheader()
        w.writerows(filas)
    with open(registro, "w") as f:
        json.dump(reg, f, indent=1)
    print("REGISTRO", reg["predicciones"], round(reg["usd"], 4), "version", v_antes, v_despues)


if __name__ == "__main__":
    if sys.argv[1] == "preparar":
        preparar(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6:])
    else:
        ejecutar(sys.argv[2], sys.argv[3], int(sys.argv[4]), float(sys.argv[5]))
