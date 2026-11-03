"""Auditoria de TODAS las sustituciones de las corridas C1/C2 (PR #29) y de la etapa C (PR #28). Solo lectura.

Emparejamiento: por POSICION dentro de la sesion (la IA devuelve la prescripcion completa en orden); si la longitud
cambia, se marca. Catalogo: evidencia/catalogo_exercises_v44_2.csv (las 46 migraciones aplicadas a una base local
efimera). Uso: python3 -I auditoria_sustituciones.py <raiz docs/FitSense/short_paper_ieee>
"""
import csv, json, os, re, sys

RAIZ = sys.argv[1]
OUT = os.path.join(RAIZ, "evaluacion_calidad_adaptacion_v2")
CAT = {int(r["exercise_id"]): r for r in csv.DictReader(open(os.path.join(OUT, "evidencia", "catalogo_exercises_v44_2.csv")))}
PERF = {r["id"]: r for r in csv.DictReader(open(os.path.join(RAIZ, "experimento_final_20_usuarios", "03_PERFILES_U01_U20.csv")))}
INTERV = {"TOO_DIFFICULT": "VARIANTE_MAS_SENCILLA_O_CARGA", "FATIGUE": "REVISAR_CARGA_Y_RECUPERACION",
          "LACK_OF_MOTIVATION": "INVESTIGAR_CAMBIO_MINIMO"}
CORRIDAS = [("C (PR #28)", os.path.join(RAIZ, "experimento_final_20_usuarios", "evidencia", "C"))] + [
    (c, os.path.join(RAIZ, "experimento_prompt_v2", "evidencia", c)) for c in ("C1_r1", "C2_r1", "C2_r2", "C1_r2")]


def sesion(firma):
    ej = []
    for id_, tipo, dosis, rest in re.findall(r"(\d+):(\w+):([^:]+):r(\d+|null)", firma):
        s, v = dosis.split("x")
        ej.append((int(id_), tipo, int(s) if s != "null" else None, v, int(rest) if rest != "null" else None))
    return ej


def segundos(ej):
    t = 300 + 60 * (len(ej) - 1)
    for _, tipo, s, v, r in ej:
        s1 = s or 1
        t += s1 * int(v.rstrip("s")) if tipo == "DURATION" else s1 * int(v) * 3
        t += max(0, s1 - 1) * (r if r is not None else 60)
    return t


def musculos(c):
    return {c["target_muscle"], c["synergist_muscle"], *[m for m in c["sec"].split("|") if m]} - {""}


def minimo_cambios_musculo(retirados, anadidos):
    """Minimo de cambios de musculo principal con el MEJOR emparejamiento posible dentro de cada patron
    (V8C solo exige que cada anadido tenga un patron de entre los retirados). Emparejamiento maximo por fuerza bruta."""
    import itertools
    total = 0
    pats = {CAT[r]["movement_pattern"] for r in retirados} | {CAT[a]["movement_pattern"] for a in anadidos}
    for p in pats:
        R = [r for r in retirados if CAT[r]["movement_pattern"] == p]
        A = [a for a in anadidos if CAT[a]["movement_pattern"] == p]
        k = min(len(R), len(A))
        mejor = 0
        for perm in itertools.permutations(A, k):
            mejor = max(mejor, sum(CAT[r]["target_muscle"] == CAT[a]["target_muscle"] for r, a in zip(R, perm)))
        total += k - mejor
    return total


def clasificar(o, n, interv):
    """Clasificacion del DESARROLLADOR (no clinica). Q1..Q4; Q5 se anota aparte."""
    obs = []
    if o["movement_pattern"] != n["movement_pattern"]:
        return "Q4", "patron distinto aceptado"
    if int(n["difficulty_level"]) > int(o["difficulty_level"]):
        return "Q4", "sube la dificultad"
    mismo = o["target_muscle"] == n["target_muscle"]
    solapa = o["target_muscle"] in musculos(n) or n["target_muscle"] in musculos(o)
    dudosa_cat = (o["exercise_id"] in ("580", "775", "780")) or (n["exercise_id"] in ("580", "775", "780"))
    if mismo and interv == "VARIANTE_MAS_SENCILLA_O_CARGA" and int(n["difficulty_level"]) == int(o["difficulty_level"]):
        q = "Q2"
        obs.append("mismo musculo y patron, misma dificultad: el catalogo no permite demostrar que sea mas sencilla"
                   + (" (la original ya tenia la dificultad minima, 1: no existe variante mas sencilla por ese atributo)"
                      if int(o["difficulty_level"]) == 1 else ""))
        return q, "; ".join(obs)
    if mismo:
        q = "Q1"
    elif dudosa_cat:
        q = "Q3"
        obs.append("clasificacion o nombre del catalogo discutible en uno de los dos ejercicios")
    else:
        q = "Q2"
    if not mismo:
        obs.append("musculo principal cambia" + (" (el original aparece como sinergista/secundario del nuevo)" if solapa
                                                else " (sin solape en principal/sinergista/secundarios)"))
    if interv == "VARIANTE_MAS_SENCILLA_O_CARGA" and int(n["difficulty_level"]) == int(o["difficulty_level"]):
        obs.append("misma dificultad: la 'variante mas sencilla' no lo es segun el unico atributo disponible")
    return q, "; ".join(obs)


filas = []
for corrida, c in CORRIDAS:
    res = {r["id"]: r for r in csv.DictReader(open(os.path.join(c, "resultados_20.csv")))}
    uid = {r["user_id"]: r["id"] for r in res.values()}
    log = open(os.path.join(c, "ejecucion_maven.log"), encoding="utf-8", errors="replace").read()
    pred = {}
    for m in re.finditer(r"REPLICATE_TIMING user=(\d+) week=\d+ attempt=(\d+) prediction_id=(\w+)", log):
        u = uid.get(m.group(1))
        if u:
            pred[u] = (m.group(2), m.group(3))  # el ultimo intento es el aceptado o el ultimo rechazado
    for x in csv.DictReader(open(os.path.join(c, "plan_siguiente.csv"))):
        if x["igual_a_la_medida"] == "true":
            continue
        a, b = sesion(x["medida"]), sesion(x["siguiente"])
        u, s = x["id"], x["sesion"]
        motivo = PERF[u]["motivo_omision_s2"] if s == "S2" else PERF[u]["motivo_omision_s3"]
        interv = INTERV.get(motivo, "?")
        ta, tb = segundos(a), segundos(b)
        minutos = int(PERF[u]["minutos_sesion"])
        minimo = -(-minutos * 60 * 70 // 100)
        ids_a, ids_b = [e[0] for e in a], [e[0] for e in b]
        retir = [i for i in ids_a if i not in ids_b]
        anad = [i for i in ids_b if i not in ids_a]
        min_cambios = minimo_cambios_musculo(retir, anad) if retir and anad else 0
        for i in range(max(len(a), len(b))):
            ea = a[i] if i < len(a) else None
            eb = b[i] if i < len(b) else None
            if ea and eb and ea[0] == eb[0]:
                continue
            o, n = CAT.get(ea[0]) if ea else None, CAT.get(eb[0]) if eb else None
            q, obs = clasificar(o, n, interv) if (o and n) else ("Q4", "no emparejable por posicion")
            filas.append(dict(
                corrida=corrida, usuario=u, objetivo=PERF[u]["goal_type"], nivel=PERF[u]["nivel"], sesion=s,
                motivo=motivo, intervencion=interv, posicion=i + 1,
                id_original=ea[0] if ea else "", nombre_original=f"{o['name_es']} / {o['name_en']}" if o else "",
                id_sustituto=eb[0] if eb else "", nombre_sustituto=f"{n['name_es']} / {n['name_en']}" if n else "",
                patron_original=o["movement_pattern"] if o else "", patron_nuevo=n["movement_pattern"] if n else "",
                musculo_original=o["target_muscle"] if o else "", musculo_nuevo=n["target_muscle"] if n else "",
                sinergista_original=o["synergist_muscle"] if o else "", sinergista_nuevo=n["synergist_muscle"] if n else "",
                secundarios_original=o["sec"] if o else "", secundarios_nuevo=n["sec"] if n else "",
                zona_original=o["bp"] if o else "", zona_nueva=n["bp"] if n else "",
                equipo_original=o["eq"] if o else "", equipo_nuevo=n["eq"] if n else "",
                aislamiento_original=o["is_isolation"] if o else "", aislamiento_nuevo=n["is_isolation"] if n else "",
                dificultad_original=o["difficulty_level"] if o else "", dificultad_nueva=n["difficulty_level"] if n else "",
                dosis_antes=f"{ea[2]}x{ea[3]} r{ea[4]}" if ea else "", dosis_despues=f"{eb[2]}x{eb[3]} r{eb[4]}" if eb else "",
                duracion_sesion_antes_s=ta, duracion_sesion_despues_s=tb, minimo_v20_s=minimo,
                validadores="aceptada (V8C + V1-V25); publicada", prediccion_aceptada=pred.get(u, ("", ""))[1],
                intento_aceptado=pred.get(u, ("", ""))[0],
                cambios_musculo_minimos_en_la_sesion=min_cambios, clasificacion=q, observaciones=obs,
                requiere_revision_profesional="si" if q != "Q1" else "recomendable (muestra)"))

campos = list(filas[0])
with open(os.path.join(OUT, "02_AUDITORIA_SUSTITUCIONES.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=campos)
    w.writeheader()
    w.writerows(filas)
from collections import Counter
print(len(filas), Counter(f["corrida"] for f in filas), Counter(f["clasificacion"] for f in filas))
for f in filas:
    if f["clasificacion"] != "Q1":
        print(f["corrida"], f["usuario"], f["sesion"], f["intervencion"][:9], f["id_original"], "->", f["id_sustituto"],
              f["musculo_original"], "->", f["musculo_nuevo"], f["dificultad_original"], "->", f["dificultad_nueva"],
              f["clasificacion"], "|", f["observaciones"][:120])
