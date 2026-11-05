#!/usr/bin/env python3
"""Auditoria del catalogo v46 (SOLO LECTURA): detecciones sistematicas sobre el catalogo exportado (exportar_catalogo.sql)
con su uso real en los barridos finales (REV2_cierre, HIST_cierre). Cada deteccion es una CONTRADICCION ENTRE DATOS del
propio catalogo; no identifica el movimiento real (eso requiere el GIF o una revision profesional).
Uso: python3 -I escanear_catalogo.py <catalogo.tsv> <dir_barridos> <salida.csv>"""
import csv, gzip, re, sys, collections
sys.dont_write_bytecode = True
COLS = "id src act name_en name_es grupo target syn sec equipo req dif presc patron iso hi fl ax kn hg oh inv gif ins_en ins_es".split()
SLOT = {"HORIZONTAL_PUSH", "VERTICAL_PUSH", "HORIZONTAL_PULL", "VERTICAL_PULL", "KNEE_DOMINANT", "HIP_HINGE", "HIP_EXTENSION"}
NO_RES = {"LOCOMOTION_CARDIO", "MOBILITY", "BALANCE_CONTROL", "OTHER", ""}
# termino de las instrucciones (EN) -> codigos de equipamiento que lo satisfacen
# APOYO real descrito en las instrucciones (no menciones de paso: «take a step», «as if sitting in a chair»)
APOYO = re.compile(r"\b(sit|sitting|stand|standing|lie|lying|kneel|kneeling|place|placing|rest|resting|hold|holding|grasp|grip|"
                   r"hook|secure|step up)\b[^.]{0,40}?\b(on|onto|edge of|against|under|to)\b(?: the| a| an| your)? "
                   r"(?:(?:sturdy|flat|incline|decline|stable|elevated|low|high|preacher)\s)?(chair|bench|box|step|platform|machine|cable|pull-?up bar|bar)\b")
SATISFACE = {"chair": {"bench", "chair"}, "bench": {"bench", "chair"}, "box": {"bench", "box", "step platform"},
             "step": {"step platform", "bench", "box"}, "platform": {"step platform", "bench", "box"},
             "machine": {"leverage machine", "smith machine", "cable", "sled machine"}, "cable": {"cable"},
             "pull-up bar": {"pull-up bar", "assisted"}, "pullup bar": {"pull-up bar", "assisted"}, "bar": {"pull-up bar", "barbell", "smith machine", "assisted"}}
cat = {}
for r in csv.reader(open(sys.argv[1], encoding="utf-8"), delimiter="\t", quoting=csv.QUOTE_NONE):
    d = dict(zip(COLS, r)); cat[int(d["id"])] = d
uso = {}
for pol in ("REV2_cierre", "HIST_cierre"):
    c = collections.defaultdict(set)
    with gzip.open(f"{sys.argv[2]}/{pol}/planes.csv.gz", "rt") as f:
        for r in csv.DictReader(f): c[int(r["ejercicio"])].add(r["perfil"])
    uso[pol] = {k: len(v) for k, v in c.items()}
hall = []
def add(i, tipo, detalle):
    d = cat[i]
    hall.append([i, d["name_en"], d["name_es"], d["grupo"], d["target"], d["patron"], d["iso"], d["dif"], d["equipo"], d["req"],
                 tipo, detalle, uso["REV2_cierre"].get(i, 0), uso["HIST_cierre"].get(i, 0)])
for i, d in cat.items():
    if d["act"] != "t": continue
    disp = {d["equipo"]} | {x.split(":", 1)[1] for x in d["req"].split(",") if ":" in x}
    texto = d["ins_en"].lower()
    m = APOYO.search(texto)
    if m and not (disp & SATISFACE[m.group(3)]):
        add(i, "S1_INSTRUCCIONES_EQUIPO", f"las instrucciones piden apoyo en «{m.group(3)}» («{m.group(0)[:60]}») y el equipamiento es {sorted(disp)}")
    en, es = d["name_en"].lower(), d["name_es"].lower()
    if "curl" in es and "curl" not in en:
        add(i, "S2_TRADUCCION_CURL", f"«{d['name_es']}» traduce «{d['name_en']}», que no es un curl")
    if re.search(r"\bfly\b|\bflye\b|breeding", en) and "reverse" not in en and (d["patron"] != "HORIZONTAL_ADDUCTION" or d["iso"] != "true"):
        add(i, "S3_APERTURA", f"apertura con patron {d['patron']} e is_isolation={d['iso']}")
    if "inverse leg curl" in en or "nordic" in en:
        add(i, "S4_NORDICO", f"curl nordico (excentrico) con dificultad {d['dif']}")
    if d["iso"] == "true" and d["patron"] in SLOT:
        add(i, "S5_AISLADO_CUBRE_SLOT", f"aislado con patron de slot {d['patron']}")
    if d["grupo"] == "back" and d["patron"] not in ("HORIZONTAL_PULL", "VERTICAL_PULL", "HIP_HINGE", "TRUNK_EXTENSION", "SCAPULAR_ELEVATION", "SHOULDER_EXTENSION") and d["patron"] not in NO_RES:
        add(i, "S6_ESPALDA_SIN_TRACCION", f"grupo espalda con patron {d['patron']}: sus series cuentan como dosis de espalda")
    if d["grupo"] == "shoulders" and d["patron"] in ("TRUNK_ROTATION_LATERAL_FLEXION", "TRUNK_FLEXION", "CORE_STABILITY"):
        add(i, "S7_HOMBRO_TRONCO", f"grupo hombro con patron de tronco {d['patron']}: cuenta como dosis de hombro")
    if d["grupo"] == "back" and d["patron"] in ("TRUNK_FLEXION",):
        add(i, "S8_ESPALDA_FLEXION_TRONCO", "flexion de tronco contada como dosis de espalda")
with open(sys.argv[3], "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f, lineterminator="\n")
    w.writerow(["exercise_id", "name_en", "name_es", "grupo", "target", "patron", "is_isolation", "dificultad", "equipo", "requisitos",
                "deteccion", "detalle", "planes_REV2_cierre", "planes_HIST_cierre"])
    w.writerows(sorted(hall, key=lambda x: (x[10], -x[12])))
c = collections.Counter(h[10] for h in hall)
u = collections.Counter(h[10] for h in hall if h[12] > 0)
for k in sorted(c): print(k, c[k], "activos; usados en REV2:", u[k])
