#!/usr/bin/env python3
"""LINEA BASE SIN REGLAS · entregables a partir de los conteos de contar_baseline.py (solo lectura).

Uso: entregables_baseline.py <baseline_dir_scratch> <conteo_dir> <gra_pago_dir> <salida_dir>
  baseline_dir_scratch: B-R1/ y B-R2/ (respuestas, predicciones.csv, versiones.txt)
  conteo_dir: planes_generacion.csv, hallazgos_generacion.csv, infracciones_generacion.csv (GRA y BASE juntos),
              adaptacion_BASE_R{1,2}.csv y cambios_BASE_R{1,2}.csv
Escribe los CSV, el Excel y metricas.json (cifras con numerador y denominador) para las tablas.
"""
import csv
import json
import os
import re
import sys
from collections import Counter, defaultdict
from math import comb

csv.field_size_limit(sys.maxsize)
B, C, G, OUT = sys.argv[1:5]
RUNS = [int(x) for x in (sys.argv[5] if len(sys.argv) > 5 else "1,2").split(",")]
os.makedirs(OUT, exist_ok=True)
ORIG = [f"O{i:02d}" for i in range(1, 21)]
FACT = [p for p in ORIG if p != "O11"]          # O11: inviabilidad esperada, aparte


def leer(r):
    with open(r, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def escribir(nombre, filas, campos=None):
    campos = campos or (list(filas[0].keys()) if filas else ["vacio"])
    with open(os.path.join(OUT, nombre), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=campos, lineterminator="\n", extrasaction="ignore")
        w.writeheader()
        w.writerows(filas)


def pct(n, d):
    return f"{100 * n / d:.1f} %" if d else "—"


# ------------------------------------------------------------------ respuestas y predicciones de la linea base
resp = {}
pred_filas = []
for r in RUNS:
    d = os.path.join(B, f"B-R{r}")
    for f in sorted(os.listdir(os.path.join(d, "respuestas"))):
        x = json.load(open(os.path.join(d, "respuestas", f), encoding="utf-8"))
        x["corrida"] = f"BASE-R{r}"
        resp[(f"BASE-R{r}", x["etiqueta"])] = x
        pred_filas.append({"corrida": f"BASE-R{r}", "etiqueta": x["etiqueta"], "experimento": x["experimento"],
                           "perfil": x["id"], "estado": x.get("estado"), "prediction_id": x.get("prediction_id"),
                           "version": x.get("version"), "modo_version": x.get("modo_version"),
                           "intento": x.get("intento"), "tokens_entrada": x.get("tokens_entrada"),
                           "tokens_salida": x.get("tokens_salida"), "latencia_ms": x.get("ms"),
                           "predict_time_s": x.get("predict_time_s"), "usd": x.get("usd"),
                           "error": x.get("error") or ""})
escribir("PREDICCIONES_REPLICATE_BASELINE.csv", pred_filas)

# ------------------------------------------------------------------ generacion
planes = leer(os.path.join(C, "planes_generacion.csv"))
hall = leer(os.path.join(C, "hallazgos_generacion.csv"))
infr = leer(os.path.join(C, "infracciones_generacion.csv"))
P = {(p["corrida"], p["perfil"], p["etapa"]): p for p in planes}
pred_gra = {}
for run in ("GRA_R1", "GRA_R2"):
    for blk in ("G", "P"):
        for l in open(os.path.join(G, f"{run}_{blk}", "trazas_ia.jsonl"), encoding="utf-8"):
            d = json.loads(l)
            if d.get("tipo") == "GENERACION":
                pred_gra[(f"{run}_i{d['intento']}", d["etiqueta"].split("/")[0])] = d.get("prediction_id")


def corr_base(r):
    return f"BASE_R{r}"


def valor_limite(m):
    pares = [(r"se esperaban (\d+) entrenamientos y el plan trae (\d+)", 2, 1),
             (r"descansa (\d+) s y el maximo es (\d+)", 1, 2),
             (r"tiene dificultad (\d+) y el maximo es (\d+)", 1, 2),
             (r"tiene (\d+) colocaciones y el maximo(?: es)? (\d+)", 1, 2),
             (r"volumen semanal es (\d+) y debe caer entre (\d+ y \d+)", 1, 2),
             (r"declarado (\d+) no coincide con el real (\d+)", 1, 2),
             (r"dura unos (\d+) s segun su contenido y el techo es (\d+) s", 1, 2),
             (r"dura (\d+) minutos y el techo es (\d+)", 1, 2),
             (r"dura unos (\d+) s y el minimo es (\d+) s", 1, 2),
             (r"trae (\d+) ejercicios y el minimo es (\d+)", 1, 2),
             (r"suma (\d+) series directas de .+? y el techo (?:por sesion |semanal )?es (\d+)", 1, 2),
             (r"dedica (\d+ de \d+) ejercicios", 1, None),
             (r"lleva (\d+) repeticiones y su tope es (\d+)", 1, 2),
             (r"lleva (\d+) series y su tope es (\d+)", 1, 2),
             (r"lleva (\d+x\d+)", 1, None),
             (r"baja de (\d+) (repeticiones|series|segundos)", None, 1),
             (r"declara (\d+) minutos", 1, None),
             (r"no esta en (available_exercises)", None, 1)]
    for pat, iv, il in pares:
        x = re.search(pat, m)
        if x:
            return (x.group(iv) if iv else ""), (x.group(il) if il else "")
    return "", ""


gen_filas, viol_filas = [], []
for r in RUNS:
    cb, cg = corr_base(r), f"GRA_R{r}_i1"
    for pid in ORIG:
        x = resp.get((f"BASE-R{r}", f"{pid}/generacion"), {})
        c = P.get((cb, pid, "CRUDO"), {})
        n = P.get((cb, pid, "NORMALIZADO"), {})
        gc = P.get((cg, pid, "CRUDO"), {})
        gn = P.get((cg, pid, "NORMALIZADO"), {})
        gen_filas.append({
            "corrida": f"BASE-R{r}", "perfil": pid, "factible_segun_fitsense": "NO (O11, inviabilidad esperada)" if pid == "O11" else "SI",
            "prediction_id": x.get("prediction_id", ""), "estado_proveedor": x.get("estado", "NO_EJECUTADA"),
            "propuesta_generada": "SI" if x.get("salida") else "NO",
            "crudo_resultado": c.get("resultado", "NO_EVALUADA"), "crudo_hallazgos_regla": c.get("n_hallazgos_regla", ""),
            "crudo_infracciones_unicas": c.get("n_infracciones_unicas", ""), "crudo_categorias": c.get("categorias", ""),
            "normalizado_resultado": n.get("resultado", "NO_EVALUADA"),
            "normalizado_infracciones_unicas": n.get("n_infracciones_unicas", ""),
            "correcciones_que_haria_el_normalizador": n.get("correcciones_normalizador", ""),
            "gra_1a_propuesta_crudo_infracciones": gc.get("n_infracciones_unicas", "sin llamada" if pid == "O11" else ""),
            "gra_1a_propuesta_normalizado_resultado": gn.get("resultado", "sin llamada (FUERZA_SIN_TIEMPO)" if pid == "O11" else ""),
            "gra_plan_publicado": "sin plan (limitacion informada)" if pid == "O11" else "VALIDO (publicado desde la IA)",
            "error": c.get("error", "")})
for h in hall:
    cond = "BASE" if h["corrida"].startswith("BASE") else "GRA"
    i = next((z for z in infr if z["corrida"] == h["corrida"] and z["perfil"] == h["perfil"] and z["etapa"] == h["etapa"]
              and f"{z['corrida']}/{z['perfil']}/{z['etapa']}/{z['familia']}/{z['sesion']}/{z['ejercicio'] or '-'}" == h["infraccion_id"]), None)
    v, lim = valor_limite(h["mensaje"])
    if cond == "BASE":
        r = h["corrida"][-1]
        pidx = resp.get((f"BASE-R{r}", f"{h['perfil']}/generacion"), {}).get("prediction_id", "")
        corrida = f"BASE-R{r}"
    else:
        pidx = pred_gra.get((h["corrida"], h["perfil"]), "")
        corrida = h["corrida"].replace("_i", "-intento").replace("_", "-")
    viol_filas.append({"condicion": cond, "corrida": corrida, "perfil": h["perfil"], "etapa_generacion": "generacion",
                       "ciclo": "", "evaluacion": h["etapa"], "prediction_id": pidx, "sesion": h["sesion"],
                       "ejercicio": h["ejercicio"], "codigo_regla": h["codigo"], "categoria": h["categoria"],
                       "descripcion": h["mensaje"], "valor_propuesto": v, "limite_o_condicion": lim,
                       "infraccion_unica": h["infraccion_id"],
                       "conteo": ("UNICA" if not i or i["n_hallazgos"] == "1" else f"SOLAPADA ({i['codigos']}: una infraccion)")})
escribir("RESULTADOS_GENERACION_BASELINE.csv", gen_filas)
escribir("VIOLACIONES_GENERACION.csv", viol_filas)


def met_gen(corridas, perfiles, etapa):
    ev = [P[(c, p, etapa)] for c in corridas for p in perfiles if (c, p, etapa) in P]
    con = [e for e in ev if e["con_violaciones"] == "SI"]
    sin = [e for e in ev if e["con_violaciones"] == "NO"]
    noev = [e for e in ev if e["con_violaciones"] == "NO_EVALUABLE"]
    ii = [z for z in infr if z["corrida"] in corridas and z["perfil"] in perfiles and z["etapa"] == etapa]
    hh = [z for z in hall if z["corrida"] in corridas and z["perfil"] in perfiles and z["etapa"] == etapa]
    return {"evaluadas": len(ev), "con_violaciones": len(con), "sin_violaciones": len(sin), "no_evaluables": len(noev),
            "infracciones_unicas": len(ii), "hallazgos_regla": len(hh),
            "por_categoria": dict(Counter(z["categoria"] for z in ii)),
            "por_codigo": dict(Counter(z["codigo"] for z in hh)),
            "max_por_plan": max([int(e["n_infracciones_unicas"]) for e in ev] or [0])}


M = {"generacion": {}, "adaptacion": {}}
for r in RUNS:
    gra_all = sorted({p["corrida"] for p in planes if p["corrida"].startswith(f"GRA_R{r}_i")})
    for etapa in ("CRUDO", "NORMALIZADO"):
        M["generacion"][f"BASE-R{r}|{etapa}|19"] = met_gen([corr_base(r)], FACT, etapa)
        M["generacion"][f"BASE-R{r}|{etapa}|O11"] = met_gen([corr_base(r)], ["O11"], etapa)
        M["generacion"][f"GRA-R{r}|{etapa}|19|1a"] = met_gen([f"GRA_R{r}_i1"], FACT, etapa)
        M["generacion"][f"GRA-R{r}|{etapa}|19|todas"] = met_gen(gra_all, FACT, etapa)
for etapa in ("CRUDO", "NORMALIZADO") if len(RUNS) == 2 else ():
    M["generacion"][f"BASE-R1+R2|{etapa}|19"] = met_gen([corr_base(1), corr_base(2)], FACT, etapa)
    M["generacion"][f"GRA-R1+R2|{etapa}|19|1a"] = met_gen(["GRA_R1_i1", "GRA_R2_i1"], FACT, etapa)


def mcnemar(b, c):
    """Exacta bilateral sobre pares discordantes (b: solo BASE con violaciones; c: solo GRA)."""
    n = b + c
    if n == 0:
        return 1.0
    k = min(b, c)
    return min(1.0, 2 * sum(comb(n, i) for i in range(k + 1)) / 2 ** n)


par = []
for r in RUNS:
    b = c = both = none = 0
    for pid in FACT:
        x = P.get((corr_base(r), pid, "NORMALIZADO"), {}).get("con_violaciones") == "SI"
        y = P.get((f"GRA_R{r}_i1", pid, "NORMALIZADO"), {}).get("con_violaciones") == "SI"
        b += x and not y; c += y and not x; both += x and y; none += not x and not y
    par.append({"corrida": r, "solo_base": b, "solo_gra": c, "ambos": both, "ninguno": none, "p_mcnemar_exacta": mcnemar(b, c)})
M["pareado_generacion_normalizado"] = par

# ------------------------------------------------------------------ adaptacion
ada, cam = [], []
for r in RUNS:
    ada += leer(os.path.join(C, f"adaptacion_BASE_R{r}.csv"))
    cam += leer(os.path.join(C, f"cambios_BASE_R{r}.csv"))
for a in ada:
    a["corrida"] = a["corrida"].replace("BASE_R", "BASE-R") if a["origen"] == "BASE" else a["corrida"].replace("BASE_R", "GRA-R")
for x in cam:
    x["corrida"] = x["corrida"].replace("BASE_R", "BASE-R") if x["origen"] == "BASE" else x["corrida"].replace("BASE_R", "GRA-R")
escribir("RESULTADOS_ADAPTACION_BASELINE.csv", ada)
escribir("CAMBIOS_NO_AUTORIZADOS.csv", cam)


def met_ada(corridas, origen):
    ev = [a for a in ada if a["corrida"] in corridas and a["origen"] == origen]
    ok = [a for a in ev if a["estado"] == "EVALUADA"]
    cc = [x for x in cam if x["corrida"] in corridas and x["origen"] == origen]
    return {"ciclos": len(ev), "evaluadas": len(ok), "no_evaluables": len(ev) - len(ok),
            "con_cambios": sum(a["con_cambios_no_autorizados"] == "SI" for a in ok),
            "sin_cambios": sum(a["con_cambios_no_autorizados"] == "NO" for a in ok),
            "cambios": len(cc), "por_categoria": dict(Counter(x["categoria"] for x in cc)),
            "sin_efecto": sum(a.get("sin_efecto") == "SI" for a in ok)}


for r in RUNS:
    M["adaptacion"][f"BASE-R{r}"] = met_ada([f"BASE-R{r}"], "BASE")
    M["adaptacion"][f"GRA-R{r}"] = met_ada([f"GRA-R{r}"], "GRA")
if len(RUNS) == 2:
    M["adaptacion"]["BASE-R1+R2"] = met_ada(["BASE-R1", "BASE-R2"], "BASE")
    M["adaptacion"]["GRA-R1+R2"] = met_ada(["GRA-R1", "GRA-R2"], "GRA")
M["predicciones"] = {"n": len(pred_filas), "usd": round(sum(float(p["usd"] or 0) for p in pred_filas), 4),
                     "estados": dict(Counter(p["estado"] for p in pred_filas)),
                     "versiones": dict(Counter(p["version"] for p in pred_filas)),
                     "modo_version": dict(Counter(p["modo_version"] for p in pred_filas))}
json.dump(M, open(os.path.join(OUT, "metricas.json"), "w"), ensure_ascii=False, indent=1)

# ------------------------------------------------------------------ Excel
try:
    from openpyxl import Workbook
    from openpyxl.styles import Font
    wb = Workbook()
    ws = wb.active
    ws.title = "Resumen"
    ws.append(["Metrica", "Condicion", "Corrida", "Etapa", "Numerador", "Denominador", "Porcentaje"])
    for k, v in M["generacion"].items():
        partes = k.split("|")
        d = v["evaluadas"]
        for nom, n in (("Planes con violaciones", v["con_violaciones"]), ("Planes sin violaciones", v["sin_violaciones"]),
                       ("No evaluables", v["no_evaluables"])):
            ws.append([f"Generacion · {nom} ({'+'.join(partes[2:])})", partes[0].split("-")[0], partes[0], partes[1], n, d, pct(n, d)])
        ws.append([f"Generacion · Infracciones unicas ({'+'.join(partes[2:])})", partes[0].split("-")[0], partes[0], partes[1],
                   v["infracciones_unicas"], d, f"{v['infracciones_unicas'] / d:.2f} por plan" if d else "—"])
    for k, v in M["adaptacion"].items():
        d = v["evaluadas"]
        ws.append(["Adaptacion · Con cambios no autorizados", k.split("-")[0], k, "", v["con_cambios"], d, pct(v["con_cambios"], d)])
        ws.append(["Adaptacion · Cambios no autorizados (total)", k.split("-")[0], k, "", v["cambios"], d, ""])
    for c in ws[1]:
        c.font = Font(bold=True)
    for nombre, filas in (("Generacion_por_usuario", gen_filas), ("Violaciones_generacion", viol_filas),
                          ("Adaptacion_por_ciclo", ada), ("Cambios_no_autorizados", cam), ("Predicciones", pred_filas)):
        w = wb.create_sheet(nombre)
        if filas:
            w.append(list(filas[0].keys()))
            for f in filas:
                w.append([f.get(k) for k in filas[0].keys()])
            for c in w[1]:
                c.font = Font(bold=True)
    # tipos por usuario y corrida (generacion CRUDO/NORMALIZADO y adaptacion)
    w = wb.create_sheet("Tipos_por_usuario")
    cats = sorted({z["categoria"] for z in infr})
    w.append(["condicion", "corrida", "perfil", "etapa"] + cats + ["total_infracciones"])
    for (corr, pid, et), p in sorted(P.items()):
        if not (corr.startswith("BASE") or corr.endswith("_i1")):
            continue
        cnt = Counter(z["categoria"] for z in infr if z["corrida"] == corr and z["perfil"] == pid and z["etapa"] == et)
        w.append(["BASE" if corr.startswith("BASE") else "GRA (1a propuesta)", corr, pid, et] + [cnt.get(c, 0) for c in cats]
                 + [sum(cnt.values())])
    w = wb.create_sheet("Adaptacion_tipos_ciclo")
    cc = sorted({x["categoria"] for x in cam}) or ["-"]
    w.append(["corrida", "origen", "etiqueta", "perfil"] + cc + ["total"])
    for a in ada:
        cnt = Counter(x["categoria"] for x in cam if x["corrida"] == a["corrida"] and x["origen"] == a["origen"]
                      and x["etiqueta"] == a["etiqueta"])
        w.append([a["corrida"], a["origen"], a["etiqueta"], a["perfil"]] + [cnt.get(c, 0) for c in cc] + [sum(cnt.values())])
    wb.save(os.path.join(OUT, "COMPARACION_FITSENSE_VS_BASELINE.xlsx"))
except ImportError:
    print("openpyxl no disponible: Excel no generado")
print(json.dumps({k: v for k, v in M.items() if k != "generacion"}, ensure_ascii=False)[:3000])
