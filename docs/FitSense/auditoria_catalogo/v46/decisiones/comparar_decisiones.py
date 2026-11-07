#!/usr/bin/env python3
"""Decisiones deportivas del catalogo v46: impacto de cada variante frente a V46 (SOLO LECTURA).

Uso: python3 -I comparar_decisiones.py <catalogo.tsv> <salida_dir> BASE_DIR_REV2 BASE_DIR_HIST NOMBRE=DIR_REV2,DIR_HIST ...

Cada DIR contiene barrido_generacion.csv y planes.csv (o .gz) de BarridoGeneracionInicialIT.
Escribe <salida_dir>/RESUMEN.csv (una fila por variante y politica), PERFILES_SIN_ISQUIOS.csv (perfiles que tenian
curl nordico en V46 y en la variante no tienen ningun ejercicio de isquios) y PERDIDOS.csv (perfiles validos en V46 que
dejan de serlo).

Definiciones (fijadas antes de mirar resultados):
  isquios  = ejercicio resistido con target 'hamstrings' (los 6 nordicos lo son) o con patron HIP_HINGE (bisagra);
             HIP_EXTENSION (puentes, empuje de cadera) se cuenta aparte como 'cadera' porque trabaja sobre todo gluteo.
  resistido = patron fuera de {LOCOMOTION_CARDIO, MOBILITY, BALANCE_CONTROL, OTHER, vacio}.
  rodilla solo aislados = todos los KNEE_DOMINANT del plan tienen is_isolation=true.
"""
import collections, csv, gzip, json, os, sys

sys.dont_write_bytecode = True
COLS = "id src act name_en name_es grupo target syn sec equipo req dif presc patron iso".split()
NORDICOS = {1195, 1196, 1197, 327, 915, 916}
NO_RES = {"LOCOMOTION_CARDIO", "MOBILITY", "BALANCE_CONTROL", "OTHER", ""}
ASIENTO = {115, 119, 130, 215, 216, 219, 220, 221, 227, 229, 239, 250, 251, 252, 274, 315, 327, 330, 331, 349, 371, 416,
           421, 435, 447, 448, 512, 513, 524, 562, 660, 679, 695, 702, 703, 704, 705, 706, 707, 708, 709, 721, 722, 723,
           725, 726, 727, 728, 729, 732, 734, 735, 736, 739, 740, 757, 758, 759, 783, 803, 835, 846, 847, 898, 916, 939,
           949, 950, 1103, 1136, 1154, 1155, 1159, 1160, 1188}
NO_DEMOSTRADA = {775, 973, 1261, 1308, 1259, 1146}
OBJ = ("GAIN_MUSCLE", "LOSE_WEIGHT", "INCREASE_STRENGTH", "GENERAL_FITNESS")
NIV = ("BEGINNER", "INTERMEDIATE", "ADVANCED")

cat = {}
for r in csv.reader(open(sys.argv[1], encoding="utf-8"), delimiter="\t", quoting=csv.QUOTE_NONE):
    if not r or not r[0].isdigit(): continue
    d = dict(zip(COLS, r)); cat[int(d["id"])] = d


def leer(carpeta):
    p = os.path.join(carpeta, "planes.csv")
    f = open(p, encoding="utf-8") if os.path.exists(p) else gzip.open(p + ".gz", "rt", encoding="utf-8")
    planes = collections.defaultdict(list)
    for r in csv.DictReader(f): planes[r["perfil"]].append(r)
    filas = {}
    for f in csv.DictReader(open(os.path.join(carpeta, "barrido_generacion.csv"), encoding="utf-8")):
        filas["|".join([f["objetivo"], f["nivel"], f["equipo"], f["dias"], f["minutos"]])] = f
    return filas, planes


def res(i): return cat[i]["patron"] not in NO_RES
def isquio(i): return res(i) and (cat[i]["target"] == "hamstrings" or cat[i]["patron"] == "HIP_HINGE")


def medir(filas, planes):
    m = collections.OrderedDict()
    val = {k: f for k, f in filas.items() if f["resultado"] == "VALIDO"}
    m["validos"] = len(val)
    for o in OBJ: m[f"validos_{o}"] = sum(f["objetivo"] == o for f in val.values())
    for n in NIV: m[f"validos_{n}"] = sum(f["nivel"] == n for f in val.values())
    m["codigos_invalidos"] = json.dumps(dict(collections.Counter(f["codigos"] for f in filas.values()
                                                                 if f["resultado"] != "VALIDO")), sort_keys=True)
    m["dosis_limitada"] = sum(f["estado_dosis"] == "DOSIS_LIMITADA" for f in val.values())
    m["grupos_bajo_objetivo_total"] = sum(int(f["grupos_bajo_objetivo"] or 0) for f in val.values())
    m["seg_medio_promedio"] = round(sum(float(f["seg_medio"] or 0) for f in val.values()) / max(1, len(val)), 1)
    m["pct_tiempo_medio_promedio"] = round(sum(float(f["pct_tiempo_medio"] or 0) for f in val.values()) / max(1, len(val)), 2)
    m["principiante_dificultad_max_gt1"] = sum(f["nivel"] == "BEGINNER" and int(f["dificultad_max"] or 0) > 1
                                               for f in val.values())
    m["principiante_alto_impacto"] = sum(f["nivel"] == "BEGINNER" and int(f["alto_impacto"] or 0) > 0
                                         for f in val.values())
    c = collections.Counter()
    for k, f in val.items():
        ids = [int(e["ejercicio"]) for e in planes[k]]
        s = set(ids)
        if s & NORDICOS:
            c["con_nordico"] += 1
            if f["nivel"] == "BEGINNER": c["principiante_con_nordico"] += 1
            if f["equipo"] == "CASA_PESO_CORPORAL": c["casa_peso_corporal_con_nordico"] += 1
        if not any(isquio(i) for i in s): c["sin_isquios"] += 1
        if not any(res(i) and cat[i]["target"] == "hamstrings" for i in s): c["sin_target_isquios"] += 1
        if not any(cat[i]["patron"] == "HIP_HINGE" for i in s): c["sin_bisagra"] += 1
        if not any(cat[i]["patron"] in ("HIP_HINGE", "HIP_EXTENSION") for i in s): c["sin_patron_cadera"] += 1
        rod = [i for i in s if cat[i]["patron"] == "KNEE_DOMINANT"]
        if not rod: c["sin_patron_rodilla"] += 1
        elif all(cat[i]["iso"] == "true" for i in rod): c["rodilla_solo_aislados"] += 1
        if s & ASIENTO:
            c["con_ejercicio_de_asiento"] += 1
            if f["equipo"] != "GIMNASIO": c["casa_con_ejercicio_de_asiento"] += 1
        for g in ("back", "shoulders"):
            fuentes = {i for i in s if cat[i]["grupo"] == g and res(i)}
            if not fuentes: c[f"sin_dosis_{g}"] += 1
            elif fuentes <= NO_DEMOSTRADA: c[f"aviso_E2a_{g}"] += 1
        if any({i for i in s if cat[i]["grupo"] == g and res(i)} and
               {i for i in s if cat[i]["grupo"] == g and res(i)} <= NO_DEMOSTRADA for g in ("back", "shoulders")):
            c["aviso_E2a_plan"] += 1
    for k in ("con_nordico", "principiante_con_nordico", "casa_peso_corporal_con_nordico", "sin_isquios",
              "sin_target_isquios", "sin_bisagra", "sin_patron_cadera", "sin_patron_rodilla", "rodilla_solo_aislados",
              "con_ejercicio_de_asiento", "casa_con_ejercicio_de_asiento", "sin_dosis_back", "sin_dosis_shoulders",
              "aviso_E2a_back", "aviso_E2a_shoulders", "aviso_E2a_plan"):
        m[k] = c[k]
    return m


def comparar(base, var):
    (fb, pb), (fv, pv) = base, var
    cambiados = sum(1 for k in fb if [e["ejercicio"] for e in pb.get(k, [])] != [e["ejercicio"] for e in pv.get(k, [])])
    perdidos = [k for k in fb if fb[k]["resultado"] == "VALIDO" and fv[k]["resultado"] != "VALIDO"]
    ganados = [k for k in fb if fb[k]["resultado"] != "VALIDO" and fv[k]["resultado"] == "VALIDO"]
    ub, uv = collections.Counter(), collections.Counter()
    for p, u in ((pb, ub), (pv, uv)):
        for k, v in p.items():
            for i in {int(e["ejercicio"]) for e in v}: u[i] += 1
    delta = sorted(((uv[i] - ub[i], i) for i in set(ub) | set(uv) if uv[i] != ub[i]), reverse=True)
    sube = [f"{i}:{cat[i]['name_es']}(+{d})" for d, i in delta if d > 0][:8]
    baja = [f"{i}:{cat[i]['name_es']}({d})" for d, i in sorted(delta) if d < 0][:8]
    sin_alt = []
    for k in fb:
        if fb[k]["resultado"] != "VALIDO" or fv[k]["resultado"] != "VALIDO": continue
        if not ({int(e["ejercicio"]) for e in pb[k]} & NORDICOS): continue
        sv = {int(e["ejercicio"]) for e in pv[k]}
        if sv & NORDICOS: continue
        if not any(isquio(i) for i in sv): sin_alt.append(k)
    return cambiados, perdidos, ganados, sube, baja, sin_alt


def main():
    sal = sys.argv[2]; os.makedirs(sal, exist_ok=True)
    base = {"PLAN-REV-2.0": leer(sys.argv[3]), "PLAN-HIST-1": leer(sys.argv[4])}
    variantes = [("V46", sys.argv[3], sys.argv[4])]
    for a in sys.argv[5:]:
        n, dirs = a.split("=", 1); r, h = dirs.split(","); variantes.append((n, r, h))
    resumen, sin_alt_rows, perd_rows = [], [], []
    for n, r, h in variantes:
        for pol, d in (("PLAN-REV-2.0", r), ("PLAN-HIST-1", h)):
            if not os.path.exists(os.path.join(d, "barrido_generacion.csv")):
                print("FALTA", n, pol, d); continue
            v = base[pol] if n == "V46" else leer(d)
            m = collections.OrderedDict([("variante", n), ("politica", pol)])
            m.update(medir(*v))
            cam, per, gan, sube, baja, sa = comparar(base[pol], v)
            m["perfiles_con_plan_distinto"] = cam; m["validos_perdidos"] = len(per); m["validos_ganados"] = len(gan)
            m["nordico_sustituido_sin_isquios"] = len(sa)
            m["uso_sube"] = " | ".join(sube); m["uso_baja"] = " | ".join(baja)
            resumen.append(m)
            for k in sa: sin_alt_rows.append({"variante": n, "politica": pol, "perfil": k})
            for k in per:
                perd_rows.append({"variante": n, "politica": pol, "perfil": k, "codigos": v[0][k]["codigos"],
                                  "primer_problema": v[0][k]["primer_problema"]})
    def escribir(nombre, filas, campos=None):
        with open(os.path.join(sal, nombre), "w", newline="", encoding="utf-8") as f:
            campos = campos or (list(filas[0].keys()) if filas else ["vacio"])
            w = csv.DictWriter(f, fieldnames=campos); w.writeheader(); w.writerows(filas)
    escribir("RESUMEN.csv", resumen)
    escribir("PERFILES_SIN_ISQUIOS.csv", sin_alt_rows, ["variante", "politica", "perfil"])
    escribir("PERDIDOS.csv", perd_rows, ["variante", "politica", "perfil", "codigos", "primer_problema"])
    for m in resumen:
        print(m["variante"], m["politica"], "validos", m["validos"], "perdidos", m["validos_perdidos"],
              "cambiados", m["perfiles_con_plan_distinto"], "nordico", m["con_nordico"], "princ_nordico",
              m["principiante_con_nordico"], "sin_isquios", m["sin_isquios"], "sin_alt", m["nordico_sustituido_sin_isquios"],
              "rod_solo_iso", m["rodilla_solo_aislados"], "sin_rod", m["sin_patron_rodilla"], "DL", m["dosis_limitada"])


main()
