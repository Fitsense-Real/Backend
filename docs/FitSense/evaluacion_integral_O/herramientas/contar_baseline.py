#!/usr/bin/env python3
"""LINEA BASE SIN REGLAS · instrumento de CONTEO (solo lectura). Congelado antes de la primera llamada pagada.

No modifica ninguna respuesta: copia la salida original del modelo, byte a byte, a un archivo por propuesta para que el
validador real del backend (EvaluacionK20IT, modo baseline-evaluar) la lea; despues clasifica y cuenta los mensajes.

Subcomandos:
  extraer_gra  <gra_pago_dir> <dir_propuestas>
      Escribe las propuestas de GENERACION de GRA-R1 y GRA-R2 (todos los intentos, de trazas_ia.jsonl) como
      GRA_R{1,2}_i{n}__<perfil>.json. Sirven para medir GRA con el MISMO instrumento y para verificarlo: en la etapa
      NORMALIZADO debe reproducir la decision que tomo el backend en cada intento.
  extraer_base <B-R dir> <corrida> <dir_propuestas>
      Escribe las propuestas de generacion de la linea base como <corrida>__<perfil>.json (salida original).
  generacion   <baseline_validacion.csv> <tablas_catalogo> <baseline_disponibles.csv> <O_PERFILES.csv> <salida_dir>
      Clasifica cada mensaje del validador (hallazgo por regla) y agrupa los hallazgos de una misma causa (infraccion
      unica). Escribe hallazgos_generacion.csv, infracciones_generacion.csv y planes_generacion.csv.
  adaptacion   <B-R dir> <GRA_G> <GRA_P> <tablas_catalogo> <baseline_disponibles.csv> <O_PERFILES.csv> <corrida> <salida_dir>
      Oraculo independiente de cambios no autorizados (politicas de la tabla PERMISOS y reglas de conservacion), para
      las propuestas de la linea base y, con el mismo codigo, para las adaptaciones publicadas por GRA.
"""
import csv
import json
import os
import re
import sys
from collections import Counter, defaultdict
from datetime import date

csv.field_size_limit(sys.maxsize)
AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(AQUI, "..", "..", "short_paper_ieee", "k20_adaptacion_real", "herramientas"))
import oraculo_k20 as ok  # noqa: E402  (funciones puras: segundos_estimados, trabajo, comparar_sesion)


def leer(r):
    with open(r, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def escribir(ruta, filas, campos=None):
    campos = campos or (list(filas[0].keys()) if filas else ["vacio"])
    with open(ruta, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=campos, lineterminator="\n", extrasaction="ignore")
        w.writeheader()
        w.writerows(filas)


# ============================================================== extraccion (sin tocar el contenido)

def extraer_gra(gra_pago, destino):
    os.makedirs(destino, exist_ok=True)
    n = 0
    for corrida in ("GRA_R1", "GRA_R2"):
        for bloque in ("G", "P"):
            for l in open(os.path.join(gra_pago, f"{corrida}_{bloque}", "trazas_ia.jsonl"), encoding="utf-8"):
                d = json.loads(l)
                if d.get("tipo") != "GENERACION":
                    continue
                pid = d["etiqueta"].split("/")[0]
                with open(os.path.join(destino, f"{corrida}_i{d['intento']}__{pid}.json"), "w", encoding="utf-8") as f:
                    f.write(d.get("salida") or "")
                n += 1
    print(f"{n} propuestas de generacion de GRA extraidas")


def salida_backend(texto):
    """Lo mismo que ReplicateResponse.joinedOutput() del backend: si la salida guardada es un objeto con json_output
    (forma de gpt-5-structured), el plan es ese objeto; con text, ese texto; si no, el texto tal cual. No cambia nada
    del contenido del plan."""
    try:
        o = json.loads(texto)
    except (TypeError, ValueError):
        return texto or ""
    if isinstance(o, dict) and "json_output" in o:
        return json.dumps(o["json_output"], ensure_ascii=False)
    if isinstance(o, dict) and "text" in o and isinstance(o["text"], str):
        return o["text"]
    return texto


def extraer_base(bdir, corrida, destino):
    os.makedirs(destino, exist_ok=True)
    n = 0
    for nombre in sorted(os.listdir(os.path.join(bdir, "respuestas"))):
        r = json.load(open(os.path.join(bdir, "respuestas", nombre), encoding="utf-8"))
        if r.get("experimento") != "A_GENERACION":
            continue
        with open(os.path.join(destino, f"{corrida}__{r['id']}.json"), "w", encoding="utf-8") as f:
            f.write(salida_backend(r.get("salida") or ""))
        n += 1
    print(f"{n} propuestas de generacion de {corrida} extraidas")


# ============================================================== generacion: clasificacion de mensajes

# Categoria de la orden (§6) y FAMILIA de causa. Hallazgos de la misma familia sobre el mismo objeto son UNA infraccion
# (p. ej., V4, V17, V20 y el coste de V24 sobre la misma sesion = una infraccion de duracion).
CAT = {
    "V2": ("Frecuencia semanal", "FRECUENCIA"),
    "V3": ("Estructura del plan (dias)", "DIAS"),
    "V12": ("Estructura del plan (dias)", "DIAS"),
    "V4": ("Duracion", "DURACION"),
    "V17": ("Duracion", "DURACION"),
    "V20": ("Duracion", "DURACION"),
    "V5": ("Cantidad de ejercicios por sesion", "N_EJERCICIOS"),
    "V6": ("Dificultad", "DIFICULTAD"),
    "V7": ("Series y repeticiones", "PRESCRIPCION"),
    "V9": ("Series y repeticiones", "DOSIS_EJERCICIO"),
    "V10": ("Series y repeticiones", "DOSIS_EJERCICIO"),
    "V16": ("Series y repeticiones", "DOSIS_EJERCICIO"),
    "V19": ("Series y repeticiones", "DOSIS_EJERCICIO"),
    "V11": ("Carga", "CARGA"),
    "V13": ("Distribucion de sesiones", "DISTRIB_SESIONES"),
    "V18": ("Distribucion de sesiones", "DISTRIB_SESIONES"),
    "V15": ("Distribucion de ejercicios", "DISTRIB_EJERCICIOS"),
    "V22": ("Distribucion de ejercicios", "DISTRIB_EJERCICIOS"),
    "V21": ("Descansos", "DESCANSO"),
    "V23": ("Patrones obligatorios", "PATRONES"),
    "V25": ("Patrones obligatorios", "PATRONES"),
    "V27": ("Patrones obligatorios", "PATRONES"),
    "V26": ("Volumen por grupo muscular", "VOLUMEN_GRUPO"),
}
FECHA = re.compile(r"\d{4}-\d{2}-\d{2}")


def clasificar(codigo, texto):
    """(categoria, familia) de un mensaje. V1 se subclasifica despues con el catalogo; V8 y V24 por su texto."""
    if codigo == "V8":
        return ("Estructura del plan", "ESTRUCTURA") if "declarado" in texto else ("Dosis (volumen semanal)", "VOLUMEN_SEMANAL")
    if codigo == "V24":
        if "colocaciones" in texto:
            return ("Cantidad de ejercicios por sesion", "N_EJERCICIOS")
        if "cuesta" in texto and "minutos" in texto:
            return ("Duracion", "DURACION")
        return ("Dosis", "DOSIS_EJERCICIO")
    return CAT.get(codigo, ("Otros V1-V27", "OTROS"))


def objeto(codigo, texto, familia):
    """Objeto afectado: sesion (fecha) y/o ejercicio, o la semana."""
    fechas = FECHA.findall(texto)
    ej = re.search(r"el ejercicio (\d+)", texto)
    if familia in ("DISTRIB_SESIONES",):
        return "|".join(fechas) or "semana", None
    if familia in ("FRECUENCIA", "VOLUMEN_SEMANAL", "ESTRUCTURA"):
        return "semana", None
    if familia == "PATRONES":
        m = re.search(r"(no cubre|slot|capacidad principal|omite la capacidad principal)\s+(\S+)", texto)
        return (fechas[0] if fechas else "semana"), (m.group(2).strip(".,") if m else None)
    if familia == "VOLUMEN_GRUPO":
        m = re.search(r"series directas de (.+?) y el techo", texto)
        return (fechas[0] if fechas else "semana"), (m.group(1) if m else None)
    return (fechas[0] if fechas else "semana"), (ej.group(1) if ej else None)


def partir_problemas(texto):
    if not texto:
        return []
    partes = re.split(r"\s*\|\|\s*", texto)
    return [p.strip() for p in partes if p.strip()]


REQUISITOS = {}   # exercise_id -> {grupo: {codigos}} (exercise_equipment_requirements; grupos AND, mismo grupo OR)


def cargar_requisitos(tablas):
    eqmap = {r["equipment_id"]: r["code"] for r in leer(os.path.join(tablas, "equipment_types.csv"))}
    ruta = os.path.join(tablas, "exercise_equipment_requirements.csv")
    if os.path.exists(ruta):
        for r in leer(ruta):
            g = r.get("requirement_group") or r.get("group_no") or r.get("grupo") or "0"
            REQUISITOS.setdefault(r["exercise_id"], {}).setdefault(g, set()).add(eqmap.get(r["equipment_id"], r["equipment_id"]))
    return eqmap


def motivo_v1(eid, pid, ex, disp, perfil, eqmap):
    """Por que un exercise_id no esta en el catalogo filtrado del perfil (solo para clasificar; no se corrige nada)."""
    c = ex.get(eid)
    if c is None:
        return "Ejercicio inexistente o no identificable", "INEXISTENTE"
    if c.get("is_active", "true") != "true":
        return "Fuera del catalogo filtrado (ejercicio inactivo)", "FUERA_CATALOGO"
    bloq = set((perfil.get("ejercicios_bloqueados") or "").split("|")) - {""}
    if eid in bloq:
        return "Ejercicios bloqueados", "BLOQUEADO"
    equipo = set(perfil["equipamiento"].split("|")) | {"body weight"}
    grupos = REQUISITOS.get(eid) or {"0": {eqmap.get(c["equipment_id"], c["equipment_id"])}}
    if not all(opciones & equipo for opciones in grupos.values()):
        return "Equipamiento", "EQUIPAMIENTO"
    flags = {"permite_alto_impacto": "high_impact", "permite_suelo": "requires_floor",
             "permite_rodillas": "requires_kneeling", "permite_colgarse": "requires_hanging",
             "permite_por_encima_cabeza": "requires_overhead"}
    if any(perfil.get(k, "true") == "false" and c.get(v) == "true" for k, v in flags.items()):
        return "Restricciones personales", "RESTRICCION"
    nivel = {"BEGINNER": 1, "INTERMEDIATE": 2, "ADVANCED": 3}[perfil["nivel"]]
    if c.get("difficulty_level") and int(c["difficulty_level"]) > nivel:
        return "Dificultad", "DIFICULTAD"
    return "Fuera del catalogo filtrado (otro filtro: edad, inversion, carga axial u otro)", "FUERA_CATALOGO"


def generacion(validacion_csv, tablas, disponibles_csv, perfiles_csv, salida):
    os.makedirs(salida, exist_ok=True)
    cargar_requisitos(tablas)
    ex = {r["exercise_id"]: r for r in leer(os.path.join(tablas, "exercises.csv"))}
    eqmap = {r["equipment_id"]: r["code"] for r in leer(os.path.join(tablas, "equipment_types.csv"))} \
        if "equipment_id" in leer(os.path.join(tablas, "equipment_types.csv"))[0] else {}
    disp = defaultdict(set)
    for r in leer(disponibles_csv):
        disp[r["id"]].add(r["exercise_id"])
    perfiles = {r["id"]: r for r in leer(perfiles_csv)}
    hallazgos, infracciones, planes = [], [], []
    for v in leer(validacion_csv):
        if v.get("archivo", "-") == "-":
            continue
        corrida = v["archivo"].split("__")[0]
        pid = v["id"]
        etapa = v.get("etapa", "CRUDO")
        claves = {}
        avisos = 0
        for i, msg in enumerate(partir_problemas(v["problemas"]) if v["resultado"] == "INVALIDO" else []):
            # Correccion del instrumento (I-1, documentada): el validador anade a los problemas una SUGERENCIA sin codigo
            # ("DIVISION: usa estas fechas...") cuando hay V13 o V18 (appendSplitSuggestion). No es un incumplimiento:
            # se registra como aviso y no se cuenta.
            if msg.startswith("DIVISION:"):
                avisos += 1
                continue
            m = re.match(r"(V\d+)\s*:", msg)
            codigo = m.group(1) if m else "SIN_CODIGO"
            cat, fam = clasificar(codigo, msg)
            ses, ej = objeto(codigo, msg, fam)
            if codigo == "V1" and ej:
                cat, fam = motivo_v1(ej, pid, ex, disp[pid], perfiles[pid], eqmap)
            clave = (fam, ses, ej)
            hallazgos.append({"corrida": corrida, "perfil": pid, "etapa": etapa, "codigo": codigo, "categoria": cat,
                              "familia": fam, "sesion": ses, "ejercicio": ej or "", "infraccion_id": "",
                              "mensaje": msg})
            if clave not in claves:
                claves[clave] = {"corrida": corrida, "perfil": pid, "etapa": etapa, "categoria": cat, "familia": fam,
                                 "sesion": ses, "ejercicio": ej or "", "codigos": [codigo], "n_hallazgos": 1,
                                 "mensaje": msg}
            else:
                claves[clave]["n_hallazgos"] += 1
                if codigo not in claves[clave]["codigos"]:
                    claves[clave]["codigos"].append(codigo)
            hallazgos[-1]["infraccion_id"] = f"{corrida}/{pid}/{etapa}/{fam}/{ses}/{ej or '-'}"
        for c in claves.values():
            c["solapamiento"] = "+".join(c["codigos"]) if len(c["codigos"]) > 1 else ""
            c["codigos"] = "+".join(c["codigos"])
            infracciones.append(c)
        por_cat = Counter(c["categoria"] for c in claves.values())
        planes.append({"corrida": corrida, "perfil": pid, "etapa": etapa, "resultado": v["resultado"],
                       "n_hallazgos_regla": sum(c["n_hallazgos"] for c in claves.values()),
                       "n_infracciones_unicas": len(claves),
                       "con_violaciones": "SI" if claves else ("NO" if v["resultado"] == "VALIDO" else "NO_EVALUABLE"),
                       "categorias": "; ".join(f"{k}={n}" for k, n in sorted(por_cat.items())),
                       "avisos_no_contados": avisos,
                       "correcciones_normalizador": v.get("correcciones_normalizador", ""),
                       "error": v["problemas"] if v["resultado"] == "NO_EVALUABLE" else ""})
    escribir(os.path.join(salida, "hallazgos_generacion.csv"), hallazgos,
             ["corrida", "perfil", "etapa", "codigo", "categoria", "familia", "sesion", "ejercicio", "infraccion_id",
              "mensaje"])
    escribir(os.path.join(salida, "infracciones_generacion.csv"), infracciones,
             ["corrida", "perfil", "etapa", "categoria", "familia", "sesion", "ejercicio", "codigos", "solapamiento",
              "n_hallazgos", "mensaje"])
    escribir(os.path.join(salida, "planes_generacion.csv"), planes)
    print(f"{len(planes)} evaluaciones, {len(hallazgos)} hallazgos, {len(infracciones)} infracciones unicas")


# ============================================================== adaptacion: oraculo de cambios no autorizados

# Permisos por intervencion (TipoIntervencion del backend): (sustituir, dosis, revision_seguridad).
PERMISOS = {"REPROGRAMAR_DIAS": (False, False, False), "ACORTAR_O_REORGANIZAR": (False, True, False),
            "VARIANTE_MAS_SENCILLA_O_CARGA": (True, True, False), "SEGURIDAD_ANTE_DOLOR": (True, True, True),
            "REVISAR_CARGA_Y_RECUPERACION": (False, True, False), "INVESTIGAR_CAMBIO_MINIMO": (False, True, False),
            "CAUSA_NO_REGISTRADA": (False, False, False), "CONSERVAR": (False, False, False)}
CATEGORIAS_ADA = {
    "C1": "Cambios sobre sesiones que no debian modificarse",
    "C2": "Incremento no permitido del volumen de la sesion",
    "C3": "Incremento no permitido de dosis de un ejercicio",
    "C4": "Incremento no permitido de dificultad",
    "C5": "Sustitucion incompatible con las restricciones, el equipamiento o los bloqueos del usuario",
    "C6": "Conservacion de un ejercicio excluido por dolor",
    "C7": "Ejercicio fuera del catalogo filtrado (inexistente o no elegible)",
    "C8": "Sesion que excede la duracion disponible",
    "C9": "Alteracion de patron no autorizada",
    "C10": "Cambio de un tipo que la intervencion no permite (politica de conservacion)",
    "C11": "Cambio que contradice la intervencion (mas trabajo que el retirado ante dolor)",
    "C12": "Estructura: sesion anadida, eliminada o movida sin autorizacion",
}
PATRON_LIBRE = "SEGURIDAD_ANTE_DOLOR"
SLOT = {"KNEE_DOMINANT": "LOWER_KNEE", "HIP_HINGE": "LOWER_HIP", "HIP_EXTENSION": "LOWER_HIP",
        "HORIZONTAL_PUSH": "UPPER_PUSH", "VERTICAL_PUSH": "UPPER_PUSH", "HORIZONTAL_PULL": "UPPER_PULL",
        "VERTICAL_PULL": "UPPER_PULL"}   # MandatorySlot
_MOD = re.compile(r"\b(trote|trotar|trotando|correr|carrera|corriendo|skipping|rodillas altas|jumping jacks?|"
                  r"piernas separadas|comba|saltar la cuerda|salto de cuerda|saltos? a la cuerda|tijeras|escaladores? r[aá]pidos?)\b")
_ALTO = re.compile(r"\b(salto|saltos|saltando|pliom[eé]trico|burpee|burpees|sprint|esprint|caja|rebote|zancada con salto|"
                   r"a una pierna con salto|patinador)\b")


def nivel_impacto(c):
    """ImpactoPolicy.nivel: 0 BAJO, 1 MODERADO, 2 ALTO, None DESCONOCIDO."""
    if c is None:
        return None
    nombre = (c.get("name_es") or "").lower()
    mod = bool(_MOD.search(nombre))
    if c.get("high_impact") == "true":
        if mod and not _ALTO.search(nombre.replace("saltos con las piernas separadas", "")):
            return 1
        return 2
    if c.get("high_impact") in (None, ""):
        return None
    return 1 if mod else 0


def sube_impacto(o, s):
    a, b = nivel_impacto(o), nivel_impacto(s)
    if b is None:
        return True
    if a is None:
        return b != 0
    return b > a   # rama de dolor: admite patrones nuevos si no hay mas anadidos que retirados


def _ej(e):
    t = e.get("prescription_type") or e.get("type")
    return {"id": str(e.get("exercise_id") if "exercise_id" in e else e["id"]), "type": t,
            "sets": _int(e.get("planned_sets", e.get("sets"))), "reps": _int(e.get("planned_reps", e.get("reps"))),
            "dur": _int(e.get("planned_duration_seconds", e.get("dur"))),
            "rest": _int(e.get("rest_seconds", e.get("rest"))), "load": None}


def _int(v):
    if v in (None, ""):
        return None
    try:
        return int(float(v))
    except (TypeError, ValueError):
        return None


def parametros(tablas):
    """Parametros del estimador de duracion de la configuracion activa (calculation_configs), como el oraculo v1.4."""
    cfg = [c for c in leer(os.path.join(tablas, "calculation_configs.csv")) if c["is_active"] == "true"]
    assert len(cfg) == 1, "una sola configuracion activa"
    pr = json.loads(cfg[0]["params"])["prescription"]
    return {"warmup_minutes": pr["warmup_minutes"], "seconds_per_rep": pr["seconds_per_rep"],
            "transition_seconds": pr["transition_seconds"], "default_rest_seconds": pr["default_rest_seconds"]}


def adaptacion(bdir, gra_g, gra_p, tablas, disponibles_csv, perfiles_csv, corrida, salida):
    os.makedirs(salida, exist_ok=True)
    cargar_requisitos(tablas)
    ex = {r["exercise_id"]: r for r in leer(os.path.join(tablas, "exercises.csv"))}
    eqmap = {r["equipment_id"]: r["code"] for r in leer(os.path.join(tablas, "equipment_types.csv"))} \
        if "equipment_id" in leer(os.path.join(tablas, "equipment_types.csv"))[0] else {}
    dif = {k: int(v["difficulty_level"]) for k, v in ex.items() if v.get("difficulty_level")}
    pat = {k: v.get("movement_pattern") or "" for k, v in ex.items()}
    disp = defaultdict(set)
    for r in leer(disponibles_csv):
        disp[r["id"]].add(r["exercise_id"])
    perfiles = {r["id"]: r for r in leer(perfiles_csv)}
    p = parametros(tablas)
    ctx = json.load(open(os.path.join(bdir, "contexto_adaptacion.json"), encoding="utf-8"))
    gdirs = {os.path.basename(d.rstrip("/")): d for d in (gra_g, gra_p)}
    filas, cambios = [], []
    for c in ctx:
        d = gdirs[c["gra_dir"]]
        t = lambda n: leer(os.path.join(d, "tablas", n + ".csv"))
        pw, pwe = t("planned_workouts"), t("planned_workout_exercises")
        ses, wse = t("workout_sessions"), t("workout_session_exercises")
        pid = c["id"]
        perfil = perfiles[pid]
        techo = int(perfil["minutos_sesion"]) * 60   # techo de V4 con PLAN-REV-2.0 (v4Maximo): segundos <= M x 60
        fuente = sorted([w for w in pw if w["plan_id"] == c["source_plan_id"]], key=lambda w: w["scheduled_date"])
        ej_de = lambda wid: [_ej(e) for e in sorted([e for e in pwe if e["planned_workout_id"] == wid],
                                                     key=lambda e: int(e["exercise_order"]))]
        antes = {date.fromisoformat(w["scheduled_date"]).isoweekday(): (w, ej_de(w["planned_workout_id"])) for w in fuente}
        # sesiones senaladas e intervenciones del backend (ids de planned_workout de la semana medida)
        sen = [x for x in re.split(r"[{},\s]+", c["sesiones_senaladas_backend"] or "") if x]
        inter = [x for x in re.split(r"[{},\s]+", c["intervenciones_backend"] or "") if x]
        interv = {}
        for i, wid in enumerate(sen):
            interv[wid] = inter[i] if i < len(inter) else (inter[-1] if inter else "CONSERVAR")
        # dolor: sesiones saltadas por dolor y ejercicios marcados con dolor
        dolor_ej = set()
        for w in fuente:
            ids_w = [e["id"] for e in ej_de(w["planned_workout_id"])]
            if w["status"] == "SKIPPED" and w["skip_reason"] == "PAIN_OR_DISCOMFORT":
                dolor_ej.update(ids_w)
            for s in [x for x in ses if x["planned_workout_id"] == w["planned_workout_id"]]:
                for x in [y for y in wse if y["session_id"] == s["session_id"]]:
                    if x.get("skip_reason") == "PAIN_OR_DISCOMFORT":
                        e = next((e for e in pwe if e["planned_exercise_id"] == x["planned_exercise_id"]), None)
                        if e:
                            dolor_ej.add(e["exercise_id"])
        for origen in ("BASE", "GRA"):
            if origen == "BASE":
                ruta_r = os.path.join(os.environ.get("RESPUESTAS_BASE") or os.path.join(bdir, "respuestas"),
                                      c["etiqueta"].replace("/", "_") + ".json")
                if not os.path.exists(ruta_r):
                    continue   # aun sin respuesta (o no ejecutada por presupuesto): no se evalua ni se cuenta
                r = json.load(open(ruta_r, encoding="utf-8"))
                pred = r.get("prediction_id")
                try:
                    prop = json.loads(salida_backend(r.get("salida") or ""))
                    workouts = prop["workouts"]
                    despues = {}
                    dup = []
                    for w in workouts:
                        k = date.fromisoformat(w["scheduled_date"]).isoweekday()
                        if k in despues:
                            dup.append(w["scheduled_date"])
                        despues.setdefault(k, (w, [_ej(e) for e in sorted(w["exercises"], key=lambda e: e["exercise_order"])]))
                except Exception as e:  # formato: NO EVALUABLE, nunca valido
                    filas.append({"corrida": corrida, "origen": origen, "etiqueta": c["etiqueta"], "perfil": pid,
                                  "ciclo": c["ciclo"], "prediction_id": pred, "estado": "NO_EVALUABLE",
                                  "error": f"{type(e).__name__}: {e}"})
                    continue
            else:
                pred = ""
                dup = []
                res = sorted([w for w in pw if w["plan_id"] == c["resulting_plan_id"]], key=lambda w: w["scheduled_date"]) \
                    if c["resulting_plan_id"] else []
                if not res:
                    filas.append({"corrida": corrida, "origen": origen, "etiqueta": c["etiqueta"], "perfil": pid,
                                  "ciclo": c["ciclo"], "estado": "SIN_PLAN_RESULTANTE", "error": ""})
                    continue
                despues = {date.fromisoformat(w["scheduled_date"]).isoweekday(): (w, ej_de(w["planned_workout_id"]))
                           for w in res}
            hall = []

            def anota(cat, sesion, ejercicio, valor, limite, detalle):
                hall.append({"corrida": corrida, "origen": origen, "etiqueta": c["etiqueta"], "perfil": pid,
                             "ciclo": c["ciclo"], "prediction_id": pred, "categoria": cat,
                             "descripcion_categoria": CATEGORIAS_ADA[cat], "sesion": sesion, "ejercicio": ejercicio,
                             "valor_propuesto": valor, "limite_o_condicion": limite, "detalle": detalle})
            for f in dup:
                anota("C12", f, "", "dos sesiones el mismo dia", "una sesion por dia", "sesion duplicada")
            ini_s = date.fromisoformat(c["semana_siguiente"])
            for dia, (wd, _) in despues.items():
                dd = date.fromisoformat(wd["scheduled_date"])
                if not 0 <= (dd - ini_s).days <= 6:
                    anota("C12", wd["scheduled_date"], "", "fecha fuera de la semana pedida",
                          f"{c['semana_siguiente']} a +6 dias", "")
            conservada_senalada = []
            for dia in sorted(set(antes) | set(despues)):
                if dia not in despues:
                    w = antes[dia][0]
                    if interv.get(w["planned_workout_id"]) != "REPROGRAMAR_DIAS":
                        anota("C12", w["scheduled_date"], "", "sesion eliminada", "conservar la sesion", "")
                    continue
                if dia not in antes:
                    anota("C12", despues[dia][0]["scheduled_date"], "", "sesion anadida",
                          "no anadir sesiones", "")
                    _fuera_catalogo(despues[dia][1], [], pid, ex, disp, perfil, eqmap, anota, despues[dia][0])
                    continue
                wa, ea = antes[dia]
                wd, ed = despues[dia]
                fecha = wd["scheduled_date"]
                iv = interv.get(wa["planned_workout_id"])
                cmp = ok.comparar_sesion(ea, ed, dif, p)
                anadidos = {e["id"] for e in ed} - {e["id"] for e in ea}
                retirados = {e["id"] for e in ea} - {e["id"] for e in ed}
                # C7 / C5: ejercicios nuevos fuera del catalogo filtrado
                _fuera_catalogo(ed, [e["id"] for e in ea], pid, ex, disp, perfil, eqmap, anota, wd)
                # C8: duracion (techo de V4 sobre la duracion estimada por contenido); solo si la sesion cambio
                seg = ok.segundos_estimados(ed, p)
                if seg > techo and not cmp["igual"]:
                    anota("C8", fecha, "", f"{seg} s", f"techo {techo} s ({perfil['minutos_sesion']} min, V4 de PLAN-REV-2.0)", "")
                # C6: ejercicio con dolor que sigue en la semana propuesta
                if iv is None:
                    if not cmp["igual"]:
                        anota("C1", fecha, "", f"{cmp['cambiados']} cambios", "sesion no senalada: se conserva",
                              json.dumps({k: cmp[k] for k in ("sustituciones", "aumentos", "reducciones",
                                                              "retirados", "anadidos")}, ensure_ascii=False)[:500])
                    continue
                sust, dosis, _seg = PERMISOS.get(iv, (False, False, False))
                if cmp["igual"]:
                    conservada_senalada.append(fecha)
                    continue
                cambiados_dosis = {eid for (eid, k, va, vb) in cmp["aumentos"] + cmp["reducciones"] if k != "menos_descanso"}
                # Q y A como multiconjuntos de ids (V8C compara por id, no por posicion)
                Q, A = sorted(retirados), sorted(anadidos)
                if cmp["trabajo_despues"] > cmp["trabajo_antes"]:
                    anota("C2", fecha, "", f"{cmp['trabajo_despues']} s de trabajo",
                          f"no superar {cmp['trabajo_antes']} s ({iv})", "")
                for (eid, k, va, vb) in cmp["aumentos"]:
                    if k == "menos_descanso":
                        continue
                    anota("C3", fecha, eid, f"{k} {vb}", f"no subir de {va} ({iv})", "")
                # dificultad: ningun ejercicio nuevo por encima del retirado (o del maximo de la sesion si no hay)
                ref = max([dif.get(q, 0) for q in Q] or [max([dif.get(e["id"], 0) for e in ea] or [0])])
                for a_id in A:
                    if dif.get(a_id, 0) > ref:
                        anota("C4", fecha, a_id, f"dificultad {dif.get(a_id)}", f"<= {ref} (retirado o sesion)", "")
                if iv == "INVESTIGAR_CAMBIO_MINIMO":
                    # P-A2 (PLAN-REV-2.0): o solo dosis, o UNA sustitucion equivalente estricta sin cambiar dosis
                    if A or Q:
                        if len(A) != 1 or len(Q) != 1:
                            anota("C10", fecha, ",".join(A + Q), f"anade {A}, retira {Q}",
                                  "P-A2: una sola sustitucion y nada mas", "")
                        elif cambiados_dosis:
                            anota("C10", fecha, ",".join(sorted(cambiados_dosis)), "sustituye y cambia dosis",
                                  "P-A2: una cosa o la otra", "")
                        else:
                            qq, aa = ex.get(Q[0]), ex.get(A[0])
                            if aa is not None and A[0] in disp[pid] and qq is not None:
                                if (qq.get("movement_pattern") or "") != (aa.get("movement_pattern") or "") or \
                                        SLOT.get(qq.get("movement_pattern")) != SLOT.get(aa.get("movement_pattern")):
                                    anota("C9", fecha, A[0], f"patron {aa.get('movement_pattern')}",
                                          f"P-A2: mismo patron que {Q[0]} ({qq.get('movement_pattern')})", "")
                                elif qq["body_part_id"] != aa["body_part_id"] or \
                                        ((qq.get("target_muscle") or "").lower() != (aa.get("target_muscle") or "").lower()
                                         and qq.get("target_muscle") and aa.get("target_muscle")):
                                    anota("C10", fecha, A[0], "otro grupo o musculo objetivo",
                                          f"P-A2: equivalente estricto de {Q[0]}", "")
                                elif sube_impacto(qq, aa):
                                    anota("C10", fecha, A[0], "mas impacto", f"P-A2: impacto no superior a {Q[0]}", "")
                elif iv == "ACORTAR_O_REORGANIZAR":
                    if A:
                        anota("C10", fecha, ",".join(A), "ejercicio anadido o sustituido", "ACORTAR no permite anadir", "")
                    elif len(Q) > 1:
                        anota("C10", fecha, ",".join(Q), f"retira {len(Q)}", "ACORTAR: retirar UNA colocacion de relleno", "")
                    elif len(Q) == 1 and SLOT.get((ex.get(Q[0]) or {}).get("movement_pattern")):
                        anota("C10", fecha, Q[0], "retira una estructural", "ACORTAR: solo relleno", "")
                elif iv in ("VARIANTE_MAS_SENCILLA_O_CARGA", "SEGURIDAD_ANTE_DOLOR"):
                    if iv == "VARIANTE_MAS_SENCILLA_O_CARGA":
                        for a_id in A:
                            if any(sube_impacto(ex.get(q), ex.get(a_id)) for q in Q) or (not Q and sube_impacto(None, ex.get(a_id))):
                                anota("C10", fecha, a_id, "mas impacto", "P-C2: VARIANTE no sube el impacto", "")
                    if iv == PATRON_LIBRE and len(A) > len(Q):
                        anota("C11", fecha, "", f"{len(A)} anadidos", f"no mas que retirados ({len(Q)})", "")
                else:
                    # REVISAR_CARGA (solo dosis), REPROGRAMAR_DIAS, CAUSA_NO_REGISTRADA, CONSERVAR
                    if A or Q:
                        anota("C10", fecha, ",".join(A + Q), f"anade {A}, retira {Q}", f"{iv} no permite sustituir ni retirar", "")
                    if cambiados_dosis and not dosis:
                        anota("C10", fecha, ",".join(sorted(cambiados_dosis)), "cambio de dosis", f"{iv} no permite cambiar la dosis", "")
            # C6 sobre toda la semana propuesta (la politica de dolor retira el ejercicio del plan)
            if dolor_ej and any(i == "SEGURIDAD_ANTE_DOLOR" for i in interv.values()):
                for dia, (wd, ed) in despues.items():
                    for e in ed:
                        if e["id"] in dolor_ej:
                            anota("C6", wd["scheduled_date"], e["id"], "ejercicio con dolor en el plan",
                                  "SEGURIDAD_ANTE_DOLOR: retirar", "")
            cambios.extend(hall)
            senaladas_fechas = [w["scheduled_date"] for w in fuente if w["planned_workout_id"] in interv]
            filas.append({"corrida": corrida, "origen": origen, "etiqueta": c["etiqueta"], "perfil": pid,
                          "ciclo": c["ciclo"], "prediction_id": pred, "estado": "EVALUADA",
                          "intervenciones": "; ".join(f"{w['scheduled_date']}={interv[w['planned_workout_id']]}"
                                                      for w in fuente if w["planned_workout_id"] in interv),
                          "sesiones_senaladas": len(senaladas_fechas),
                          "n_cambios_no_autorizados": len(hall),
                          "con_cambios_no_autorizados": "SI" if hall else "NO",
                          "categorias": "; ".join(f"{k}={n}" for k, n in sorted(Counter(h["categoria"] for h in hall).items())),
                          "senaladas_sin_cambio": ";".join(conservada_senalada),
                          "sin_efecto": "SI" if senaladas_fechas and len(conservada_senalada) == len(senaladas_fechas) else "NO",
                          "error": ""})
    escribir(os.path.join(salida, f"adaptacion_{corrida}.csv"), filas,
             ["corrida", "origen", "etiqueta", "perfil", "ciclo", "prediction_id", "estado", "intervenciones",
              "sesiones_senaladas", "n_cambios_no_autorizados", "con_cambios_no_autorizados", "categorias",
              "senaladas_sin_cambio", "sin_efecto", "error"])
    escribir(os.path.join(salida, f"cambios_{corrida}.csv"), cambios,
             ["corrida", "origen", "etiqueta", "perfil", "ciclo", "prediction_id", "categoria", "descripcion_categoria",
              "sesion", "ejercicio", "valor_propuesto", "limite_o_condicion", "detalle"])
    print(f"{corrida}: {len(filas)} evaluaciones, {len(cambios)} cambios no autorizados")


def _fuera_catalogo(ed, ids_antes, pid, ex, disp, perfil, eqmap, anota, wd):
    for e in ed:
        if e["id"] in ids_antes or e["id"] in disp[pid]:
            continue
        motivo, fam = motivo_v1(e["id"], pid, ex, disp[pid], perfil, eqmap)
        cat = "C5" if fam in ("EQUIPAMIENTO", "RESTRICCION", "BLOQUEADO") else "C7"
        anota(cat, wd["scheduled_date"], e["id"], e["id"], "catalogo filtrado del perfil", motivo)


if __name__ == "__main__":
    a = sys.argv
    {"extraer_gra": lambda: extraer_gra(a[2], a[3]), "extraer_base": lambda: extraer_base(a[2], a[3], a[4]),
     "generacion": lambda: generacion(a[2], a[3], a[4], a[5], a[6]),
     "adaptacion": lambda: adaptacion(a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9])}[a[1]]()
