#!/usr/bin/env python3
"""Impacto de las variantes contrafactuales del catalogo frente a la linea base (SOLO LECTURA).
Uso: python3 -I comparar_variantes.py <catalogo_original.tsv> <dir_barridos> <salida.csv> <BASE> <VARIANTE> [...]
Cada <dir_barridos>/<nombre>_<POLITICA>/ contiene barrido_generacion.csv y planes.csv (o .gz)."""
import csv, gzip, os, sys, collections, json
sys.dont_write_bytecode = True
COLS = "id src act name_en name_es grupo target syn sec equipo req dif presc patron iso".split()
SLOT = {"HORIZONTAL_PUSH": "PUSH", "VERTICAL_PUSH": "PUSH", "HORIZONTAL_PULL": "PULL", "VERTICAL_PULL": "PULL",
        "KNEE_DOMINANT": "KNEE", "HIP_HINGE": "HIP", "HIP_EXTENSION": "HIP"}
NO_RES = {"LOCOMOTION_CARDIO", "MOBILITY", "BALANCE_CONTROL", "OTHER", ""}
AISLADOS = {327, 637, 915, 916, 1007, 1008, 1009, 1010, 1046, 1195, 1196, 1197, 1266}
CAMBIOS = {"CF1_775_973_sin_dosis": {775: "MOBILITY", 973: "MOBILITY"},
           "CF1b_dosis_de_grupo_sin_patron_coherente": {i: "MOBILITY" for i in (775, 973, 1261, 1308, 1259, 1146)},
           "CF2_P15B_aislados_sin_slot": {i: "ANKLE_PLANTAR_FLEXION" for i in AISLADOS}}
cat = {}
for r in csv.reader(open(sys.argv[1], encoding="utf-8"), delimiter="\t", quoting=csv.QUOTE_NONE):
    d = dict(zip(COLS, r)); cat[int(d["id"])] = d
D, SAL, BASE, VARS = sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5:]


def planes(carpeta):
    p = os.path.join(carpeta, "planes.csv")
    f = open(p, encoding="utf-8") if os.path.exists(p) else gzip.open(p + ".gz", "rt")
    out = collections.defaultdict(list)
    for r in csv.DictReader(f): out[r["perfil"]].append(r)
    return out


def medir(nombre, pol):
    c = os.path.join(D, f"{nombre}_{pol}")
    pat = {i: CAMBIOS.get(nombre, {}).get(i, d["patron"]) for i, d in cat.items()}
    filas = list(csv.DictReader(open(os.path.join(c, "barrido_generacion.csv"))))
    pl = planes(c)
    clave = lambda f: "|".join([f["objetivo"], f["nivel"], f["equipo"], f["dias"], f["minutos"]])
    m = collections.OrderedDict()
    val = [f for f in filas if f["resultado"] == "VALIDO"]
    m["validos"] = len(val)
    for o in ("GAIN_MUSCLE", "LOSE_WEIGHT", "INCREASE_STRENGTH", "GENERAL_FITNESS"):
        m[f"validos_{o}"] = sum(f["objetivo"] == o for f in val)
    m["codigos_invalidos"] = json.dumps(collections.Counter(f["codigos"] for f in filas if f["resultado"] != "VALIDO"))
    m["dosis_limitada"] = sum(f["estado_dosis"] == "DOSIS_LIMITADA" for f in val)
    m["dosis_objetivo"] = sum(f["estado_dosis"] == "DOSIS_OBJETIVO" for f in val)
    m["grupos_bajo_objetivo_total"] = sum(int(f["grupos_bajo_objetivo"] or 0) for f in val)
    sin = collections.Counter(); solo_iso = collections.Counter(); sin_grupo = collections.Counter(); uso = collections.Counter()
    for f in val:
        v = pl[clave(f)]
        ids = [int(e["ejercicio"]) for e in v]
        for i in set(ids): uso[i] += 1
        fams = collections.defaultdict(list)
        for i in ids:
            if pat[i] in SLOT: fams[SLOT[pat[i]]].append(i in AISLADOS and pat[i] == "KNEE_DOMINANT")
        if "KNEE" not in fams: sin[f["objetivo"]] += 1
        elif all(fams["KNEE"]): solo_iso[f["objetivo"]] += 1
        for g in ("back", "shoulders"):
            if not any(cat[i]["grupo"] == g and pat[i] not in NO_RES for i in ids): sin_grupo[(g, f["objetivo"])] += 1
    m["sin_patron_rodilla"] = json.dumps(dict(sin)); m["rodilla_solo_aislados"] = json.dumps(dict(solo_iso))
    m["sin_dosis_espalda"] = json.dumps({o: n for (g, o), n in sin_grupo.items() if g == "back"})
    m["sin_dosis_hombro"] = json.dumps({o: n for (g, o), n in sin_grupo.items() if g == "shoulders"})
    for i in (775, 973, 1261, 1308, 1259, 1146): m[f"planes_con_{i}"] = uso[i]
    m["planes_con_aislado_rodilla"] = sum(1 for f in val if set(int(e["ejercicio"]) for e in pl[clave(f)]) & AISLADOS)
    return m, pl, uso, {clave(f): f for f in filas}


res = {}
for pol in ("PLAN-REV-2.0", "PLAN-HIST-1"):
    if not os.path.isdir(os.path.join(D, f"{BASE}_{pol}")): continue
    mb, pb, ub, fb = medir(BASE, pol)
    res[(BASE, pol)] = mb
    for v in VARS:
        if not os.path.isdir(os.path.join(D, f"{v}_{pol}")): continue
        mv, pv, uv, fv = medir(v, pol)
        cambiados = sum(1 for k in set(pb) | set(pv)
                        if sorted((e["fecha"], e["ejercicio"], e["series"], e["reps"], e["segundos"]) for e in pb.get(k, []))
                        != sorted((e["fecha"], e["ejercicio"], e["series"], e["reps"], e["segundos"]) for e in pv.get(k, [])))
        mv["perfiles_con_plan_distinto"] = cambiados
        perdidos = [k for k in fb if fb[k]["resultado"] == "VALIDO" and fv.get(k, {}).get("resultado") != "VALIDO"]
        mv["validos_que_dejan_de_serlo"] = len(perdidos)
        mv["ejemplos_perdidos"] = "; ".join(perdidos[:6])
        mv["perdidos_por_objetivo"] = json.dumps(collections.Counter(k.split("|")[0] for k in perdidos))
        mv["perdidos_codigos"] = json.dumps(collections.Counter(fv[k]["codigos"] for k in perdidos if k in fv))
        suben = sorted(((uv[i] - ub.get(i, 0), i) for i in uv if uv[i] - ub.get(i, 0) > 0), reverse=True)[:8]
        mv["ejercicios_que_mas_suben"] = "; ".join(f"{i} {cat[i]['name_en'][:28]} (+{d})" for d, i in suben)
        res[(v, pol)] = mv
claves = list(dict.fromkeys(k for m in res.values() for k in m))
with open(SAL, "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f, lineterminator="\n")
    w.writerow(["metrica"] + [f"{v}|{p}" for v, p in res])
    for k in claves: w.writerow([k] + [res[x].get(k, "") for x in res])
for (v, p), m in res.items():
    print(f"== {v} {p}"); [print("  ", k, "=", m[k]) for k in m]
