"""H12: analiza las corridas de escenarios (P0_HISTORICA, P1_H1, P2_H1_H12A, P3_H1_H12C2), 3 semanas, guion controlado,
sin red. Solo lectura. Una fila por politica x usuario x semana x sesion senalada, mas comprobaciones de seguridad
(sesiones no senaladas, dosis que sube, descanso, minimo V20, linaje) en las tres semanas.
Uso: python3 -I analizar_escenarios.py <carpeta evidencia/h12>  ->  escenarios_resultado.csv (y resumen en pantalla)"""
import csv, json, math, os, re, sys
from collections import Counter, defaultdict

B = sys.argv[1]
RAIZ = os.path.abspath(os.path.join(B, *[".."] * 6))
CAT = {int(r["exercise_id"]): r for r in csv.DictReader(open(os.path.join(
    RAIZ, "docs/FitSense/short_paper_ieee/evaluacion_calidad_adaptacion_v2/evidencia/catalogo_exercises_v44_2.csv")))}
PERF = {r["id"]: r for r in csv.DictReader(open(os.path.join(B, "PERFILES_ESCENARIOS_H12.csv")))}
POL = ["P0_HISTORICA", "P1_H1", "P2_H1_H12A", "P3_H1_H12C2"]
EJ = re.compile(r"(\d+):(\w+):(\d+)x(\d+)(s?):r(\d+)")


def ej(f):
    return [dict(id=int(a), tipo=t, s=int(x), v=int(y), r=int(r)) for a, t, x, y, _, r in EJ.findall(f)]


def dur(e):
    t = 300 + 60 * (len(e) - 1)
    for x in e:
        t += x["s"] * x["v"] * (1 if x["tipo"] == "DURATION" else 3) + (x["s"] - 1) * x["r"]
    return t


def de_semana(p, col, n):
    for k in range(n, 1, -1):
        v = p.get(f"{col}_semana{k}")
        if v:
            return v
    return p.get(col) or ""


filas, seguridad = [], Counter()
for pol in POL:
    c = os.path.join(B, pol)
    res = {r["id"]: r for r in csv.DictReader(open(os.path.join(c, "resultados_20.csv")))}
    uid = {r["user_id"]: r["id"] for r in res.values() if r.get("user_id")}
    log = open(os.path.join(c, "ejecucion_maven.log"), encoding="utf-8", errors="replace").read()
    trazas = defaultdict(list)
    semana_de_sesion = {}
    for l in open(os.path.join(c, "trazas_ia.jsonl")):
        t = json.loads(l)
        if t.get("tipo") != "ADAPTACION_GUION":
            continue
        u, et = t["etiqueta"].split("/")
        sem = 1 if et == "adaptacion" else int(et[-1])
        trazas[(u, sem)].append((t["intento"], t.get("acciones", "")))
        for sid in re.findall(r"(?:^|\| )(\d+)[ :]", t.get("acciones", "")):
            semana_de_sesion[int(sid)] = sem
    rech = defaultdict(list)
    for m in re.finditer(r"Intento (\d) para el usuario (\d+): la semana propuesta no pasa \S* \[(.*)\]\n", log):
        s_id = re.search(r"la sesion (\d+)", m.group(3))
        f = re.search(r"2026-11-(\d\d)", m.group(3))
        sem = semana_de_sesion.get(int(s_id.group(1))) if s_id else None
        if sem is None:
            sem = (int(f.group(1)) - 9) // 7 + 1 if f else 0
        rech[(uid.get(m.group(2)), sem)].append(",".join(sorted(set(re.findall(r"\b(V\d+[A-Z]?):", m.group(3))))))
    estado = {}
    for r in res.values():
        estado[(r["id"], 1)] = r.get("estado_v32", "")
    for n in (2, 3):
        fn = os.path.join(c, f"semana{n}.csv")
        if os.path.exists(fn):
            for r in csv.DictReader(open(fn)):
                estado[(r["id"], n)] = r.get("status", "")
                seguridad[(pol, "violaciones_linaje")] += int(r.get("violaciones_linaje") or 0)
    for n, fn in ((1, "plan_siguiente.csv"), (2, "plan_siguiente_semana2.csv"), (3, "plan_siguiente_semana3.csv")):
        for x in csv.DictReader(open(os.path.join(c, fn))):
            u, ses = x["id"], x["sesion"]
            p = PERF[u]
            sec = de_semana(p, "secuencia", n).split("/")
            i = int(ses[1]) - 1
            senalada = i < len(sec) and sec[i] == "O"
            a, b = ej(x["medida"]), ej(x["siguiente"])
            minimo = math.ceil(int(p["minutos_sesion"]) * 60 * 70 / 100)
            # seguridad
            if not senalada and x["igual_a_la_medida"] == "false":
                seguridad[(pol, "cambios_no_senaladas")] += 1
            for ea, eb in zip(a, b):
                if ea["id"] == eb["id"] and (eb["s"] > ea["s"] or eb["v"] > ea["v"]):
                    seguridad[(pol, "dosis_subida")] += 1
                if ea["r"] != eb["r"]:
                    seguridad[(pol, "descanso_cambiado")] += 1
            if x["igual_a_la_medida"] == "false" and dur(b) < minimo:
                seguridad[(pol, "bajo_minimo_v20")] += 1
            if not senalada:
                continue
            motivo = de_semana(p, "motivo_omision_s" + str(i + 1), n)
            subs = [(ea["id"], eb["id"]) for ea, eb in zip(a, b) if ea["id"] != eb["id"]]
            difs = [(int(CAT[o]["difficulty_level"]), int(CAT[nn]["difficulty_level"])) for o, nn in subs]
            pat_o = Counter(CAT[o]["movement_pattern"] for o, _ in subs)
            pat_n = Counter(CAT[nn]["movement_pattern"] for _, nn in subs)
            dosis = [ea["id"] for ea, eb in zip(a, b) if ea["id"] == eb["id"] and (ea["s"], ea["v"]) != (eb["s"], eb["v"])]
            acc = " || ".join(f"i{k}: " + v for k, v in trazas.get((u, n), []))
            filas.append(dict(
                politica=pol, usuario=u, objetivo=p["goal_type"], nivel=p["nivel"], semana=n, sesion=ses, motivo=motivo,
                escenarios=p.get("escenarios_h12", ""), estrategia_intento1=dict(
                    kv.split("=") for kv in de_semana(p, "guion_h12", n).split(";") if "=" in kv).get(ses, "guion V2"),
                intentos=len(trazas.get((u, n), [])), rechazos=" | ".join(rech.get((u, n), [])), estado_v32=estado.get((u, n), ""),
                publicada_cambiada=x["igual_a_la_medida"] == "false", sustituciones=len(subs),
                laterales=sum(d == o for o, d in difs), a_menor=sum(d < o for o, d in difs),
                cambio_musculo=sum(CAT[o]["target_muscle"] != CAT[nn]["target_muscle"] for o, nn in subs),
                distribucion_patrones_conservada=pat_o == pat_n, cambios_dosis=len(dosis),
                duracion_s=dur(a), duracion_nueva_s=dur(b), minimo_v20_s=minimo, margen_s=dur(a) - minimo,
                acciones_guion=acc[:900]))
out = os.path.join(B, "escenarios_resultado.csv")
with open(out, "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=list(filas[0]))
    w.writeheader()
    w.writerows(filas)
for pol in POL:
    fs = [x for x in filas if x["politica"] == pol]
    print(pol, "senaladas", len(fs), "cambiadas", sum(x["publicada_cambiada"] for x in fs),
          "subs", sum(x["sustituciones"] for x in fs), "laterales", sum(x["laterales"] for x in fs),
          "a_menor", sum(x["a_menor"] for x in fs), "dosis", sum(x["cambios_dosis"] for x in fs),
          "distrib_rota", sum(not x["distribucion_patrones_conservada"] for x in fs),
          "intentos", sum(x["intentos"] for x in fs), "rechazos", sum(len(x["rechazos"].split(" | ")) if x["rechazos"] else 0 for x in fs),
          "SIN_EFECTO(estado)", sum(x["estado_v32"] == "SIN_EFECTO" for x in fs),
          {k: seguridad[(pol, k)] for k in ("cambios_no_senaladas", "dosis_subida", "descanso_cambiado", "bajo_minimo_v20",
                                            "violaciones_linaje")})
