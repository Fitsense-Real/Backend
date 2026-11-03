"""H12: diagnostico de las sesiones TOO_DIFFICULT (VARIANTE_MAS_SENCILLA_O_CARGA) de las corridas con IA real
conservadas (PR #28 C, PR #29 C1/C2, PR #30 RC1/RC2). Solo lectura.

Para cada sesion senalada (aplicada o no): ejercicios, dificultad, alternativas del mismo patron y tipo de prescripcion
en el conjunto elegible REAL del usuario (available_exercises de su prompt), opciones de reducir dosis que caben en el
margen V20 (replica de SessionDurationEstimator y de los pisos de FactibilidadAdaptacion: min_reps, 2 series, 20 s; GM
sin reducciones por 4D), y lo que hizo la IA (sustituciones laterales, a menor dificultad, cambios de dosis), con la
rotacion de la semana 2.

Uso: python3 -I diagnostico_h12.py <raiz del repo>
Salidas (junto al script): causas_variante.csv, r01_r05_detalle.csv
"""
import csv, glob, json, math, os, re, sys
from collections import Counter

RAIZ = sys.argv[1]
SP = os.path.join(RAIZ, "docs", "FitSense", "short_paper_ieee")
F20 = os.path.join(SP, "experimento_final_20_usuarios")
PV2 = os.path.join(SP, "experimento_prompt_v2", "evidencia")
EV2 = os.path.join(SP, "evaluacion_calidad_adaptacion_v2")
AQUI = os.path.dirname(os.path.abspath(__file__))
CAT = {int(r["exercise_id"]): r for r in csv.DictReader(open(os.path.join(EV2, "evidencia", "catalogo_exercises_v44_2.csv")))}
PERF = {r["id"]: r for r in csv.DictReader(open(os.path.join(F20, "03_PERFILES_U01_U20.csv")))}
PERF.update({r["id"]: r for r in csv.DictReader(open(os.path.join(EV2, "10_PERFILES_SINTETICOS_R01_R08.csv")))})

CORRIDAS = [("C (PR #28)", os.path.join(F20, "evidencia", "C"), (1,))]
CORRIDAS += [(c, os.path.join(PV2, c), (1,)) for c in ("C1_r1", "C2_r1", "C2_r2", "C1_r2")]
CORRIDAS += [(c, os.path.join(EV2, "evidencia", c), (1, 2)) for c in ("RC1_r1", "RC2_r1")]

EJ = re.compile(r"(\d+):(\w+):(\d+)x(\d+)(s?):r(\d+)")


def ejercicios(firma):
    return [dict(id=int(a), tipo=t, series=int(s), v=int(v), descanso=int(r)) for a, t, s, v, _, r in EJ.findall(firma)]


def duracion(ej):  # SessionDurationEstimator (MVP-1.2)
    t = 300 + 60 * (len(ej) - 1)
    for e in ej:
        t += e["series"] * e["v"] * (1 if e["tipo"] == "DURATION" else 3) + (e["series"] - 1) * e["descanso"]
    return t


def elegibles(carpeta, usuario):
    """available_exercises del primer prompt de adaptacion de ese usuario en esa corrida (constante por usuario)."""
    for p in sorted(glob.glob(os.path.join(carpeta, "prompts", usuario + "_adaptacion*.txt"))):
        t = open(p, encoding="utf-8").read()
        j = json.loads(t[t.index("Datos de entrada:") + len("Datos de entrada:"):].strip())
        return {e["exercise_id"]: e for e in j["available_exercises"]}, j["constraints"]["max_difficulty_level"]
    # La etapa C del PR #28 no conservo los prompts: el mismo usuario, perfil y catalogo estan en C1_r1 (PR #29).
    if carpeta.endswith(os.sep + "C"):
        return elegibles(os.path.join(PV2, "C1_r1"), usuario)
    return None, None


def opciones_dosis(ej, goal, elig, margen):
    """Reducciones de UN cambio (las de FactibilidadAdaptacion): [(id, cambio, segundos, cabe)]."""
    if goal == "GAIN_MUSCLE":
        return []
    ops = []
    for e in ej:
        mr = (elig.get(e["id"]) or {}).get("min_reps") or 1
        if e["tipo"] == "DURATION":
            if e["v"] - 10 >= 20:
                ops.append((e["id"], "-10 s", e["series"] * 10))
            if e["series"] - 1 >= 2:
                ops.append((e["id"], "-1 serie", e["v"] + e["descanso"]))
        else:
            if e["v"] - 1 >= mr:
                ops.append((e["id"], "-1 rep", e["series"] * 3))
            if e["series"] - 1 >= 2:
                ops.append((e["id"], "-1 serie", e["v"] * 3 + e["descanso"]))
    return [(i, c, s, s <= margen) for i, c, s in ops]


def motivo(u, ses, semana):
    p = PERF[u]
    suf = "_semana2" if semana == 2 else ""
    return p.get("motivo_omision_" + ses.lower() + suf) or p.get("motivo_omision_" + ses.lower()) or ""


filas, detalle = [], []
for corrida, carpeta, semanas in CORRIDAS:
    previas = {}
    for semana in semanas:
        fn = os.path.join(carpeta, "plan_siguiente.csv" if semana == 1 else "plan_siguiente_semana2.csv")
        for x in csv.DictReader(open(fn)):
            u, ses = x["id"], x["sesion"]
            if motivo(u, ses, semana) != "TOO_DIFFICULT":
                continue
            elig, maxdif = elegibles(carpeta, u)
            goal = PERF[u]["goal_type"]
            a, b = ejercicios(x["medida"]), ejercicios(x["siguiente"])
            minimo = math.ceil(int(PERF[u].get("minutos_sesion") or 0) * 60 * 70 / 100)
            margen = duracion(a) - minimo
            con_menor, solo_lateral, en_minimo = 0, 0, 0
            for e in a:
                c = CAT[e["id"]]
                d = int(c["difficulty_level"])
                en_minimo += d == 1
                alt = [k for k, v in (elig or {}).items() if k != e["id"] and v["movement_pattern"] == c["movement_pattern"]
                       and v["prescription_type"] == e["tipo"]]
                menores = [k for k in alt if (elig[k]["difficulty"] or 9) < d]
                iguales = [k for k in alt if elig[k]["difficulty"] == d]
                con_menor += bool(menores)
                solo_lateral += (not menores) and bool(iguales)
            ops = opciones_dosis(a, goal, elig or {}, margen)
            subs = [(ea, eb) for ea, eb in zip(a, b) if ea["id"] != eb["id"]]
            dosis = [(ea, eb) for ea, eb in zip(a, b) if ea["id"] == eb["id"] and (ea["series"], ea["v"]) != (eb["series"], eb["v"])]
            dif = [(int(CAT[o["id"]]["difficulty_level"]), int(CAT[n["id"]]["difficulty_level"])) for o, n in subs]
            mus = sum(CAT[o["id"]]["target_muscle"] != CAT[n["id"]]["target_muscle"] for o, n in subs)
            rot = [o["id"] for o, _ in subs if o["id"] in previas.get((u, ses), set())]
            if semana == 1:
                previas[(u, ses)] = {n["id"] for _, n in subs}
            filas.append(dict(
                corrida=corrida, semana=semana, usuario=u, objetivo=goal, nivel=PERF[u]["nivel"],
                ubicacion=PERF[u]["ubicacion"], sesion=ses, max_dificultad_perfil=maxdif,
                elegibles=len(elig or {}), ejercicios=len(a), en_dificultad_minima=en_minimo,
                con_alternativa_menor=con_menor, solo_alternativa_lateral=solo_lateral,
                duracion_s=duracion(a), minimo_v20_s=minimo, margen_s=margen,
                opciones_dosis=len(ops), opciones_dosis_que_caben=sum(1 for o in ops if o[3]),
                dosis_bloqueada_por_4d=goal == "GAIN_MUSCLE",
                cambio=x["igual_a_la_medida"] == "false", sustituciones=len(subs),
                sustituciones_menor_dificultad=sum(n < o for o, n in dif),
                sustituciones_misma_dificultad=sum(n == o for o, n in dif),
                sustituciones_mayor_dificultad=sum(n > o for o, n in dif),
                sustituciones_cambio_musculo=mus, cambios_dosis=len(dosis),
                sustituciones_con_dosis_menor=sum((n["series"], n["v"]) < (o["series"], o["v"]) for o, n in subs),
                rota_lo_introducido_en_semana_1=len(rot),
                proporcion_sesion_cambiada=round(len(subs) / len(a), 2) if a else 0))
            if u in ("R01", "R05"):
                for i, ea in enumerate(a):
                    eb = b[i] if i < len(b) else None
                    co, cn = CAT[ea["id"]], CAT[eb["id"]] if eb else None
                    detalle.append(dict(
                        corrida=corrida, semana=semana, usuario=u, objetivo=goal, nivel=PERF[u]["nivel"], sesion=ses,
                        motivo="TOO_DIFFICULT", posicion=i + 1, original=ea["id"], nombre_original=co["name_en"],
                        dificultad_original=co["difficulty_level"], patron_original=co["movement_pattern"],
                        musculo_original=co["target_muscle"], dosis_original=f'{ea["series"]}x{ea["v"]}{"s" if ea["tipo"] == "DURATION" else ""} r{ea["descanso"]}',
                        sustituto=eb["id"] if eb and eb["id"] != ea["id"] else "", nombre_sustituto=cn["name_en"] if eb and eb["id"] != ea["id"] else "",
                        dificultad_sustituto=cn["difficulty_level"] if eb and eb["id"] != ea["id"] else "",
                        patron_sustituto=cn["movement_pattern"] if eb and eb["id"] != ea["id"] else "",
                        musculo_sustituto=cn["target_muscle"] if eb and eb["id"] != ea["id"] else "",
                        dosis_nueva=f'{eb["series"]}x{eb["v"]}{"s" if eb["tipo"] == "DURATION" else ""} r{eb["descanso"]}' if eb else "",
                        duracion_s=duracion(a), duracion_nueva_s=duracion(b), minimo_v20_s=minimo, margen_s=margen,
                        era_sustituto_de_semana_1=ea["id"] in rot))

for nombre, datos in (("causas_variante.csv", filas), ("r01_r05_detalle.csv", detalle)):
    with open(os.path.join(AQUI, nombre), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(datos[0]))
        w.writeheader()
        w.writerows(datos)
cambio = [f for f in filas if f["cambio"]]
print("sesiones TOO_DIFFICULT:", len(filas), "cambiadas:", len(cambio))
print("por nivel:", Counter((f["nivel"], f["max_dificultad_perfil"]) for f in filas))
for k in ("ejercicios", "en_dificultad_minima", "con_alternativa_menor", "solo_alternativa_lateral", "sustituciones",
          "sustituciones_menor_dificultad", "sustituciones_misma_dificultad", "sustituciones_mayor_dificultad",
          "sustituciones_cambio_musculo", "cambios_dosis", "rota_lo_introducido_en_semana_1"):
    print(k, sum(f[k] for f in filas), "| en las cambiadas:", sum(f[k] for f in cambio))
print("con alguna opcion de dosis que cabe:", sum(f["opciones_dosis_que_caben"] > 0 for f in filas),
      "| bloqueadas por 4D:", sum(f["dosis_bloqueada_por_4d"] for f in filas),
      "| margen 0:", sum(f["margen_s"] == 0 for f in filas), "| margen < 30:", sum(f["margen_s"] < 30 for f in filas))
print("con alguna alternativa menor:", sum(f["con_alternativa_menor"] > 0 for f in filas))
