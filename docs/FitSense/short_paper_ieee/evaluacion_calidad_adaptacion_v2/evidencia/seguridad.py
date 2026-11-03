"""Comprobaciones de seguridad y de limite V20 sobre cualquier carpeta de corrida (semana 1 y, si existe, semana 2).

Uso: python3 -I seguridad.py <carpeta_corrida> [<perfiles.csv>]
Salida: JSON con violaciones y la lista de sesiones publicadas exactamente en el minimo V20 (replica exacta de
SessionDurationEstimator, MVP-1.2). Solo lectura.
"""
import csv, json, os, re, sys

c = sys.argv[1]
perf_csv = sys.argv[2] if len(sys.argv) > 2 else None


def sesion(firma):
    ej = []
    for id_, tipo, dosis, rest in re.findall(r"(\d+):(\w+):([^:]+):r(\d+|null|-)", firma):
        s, v = dosis.split("x")
        ej.append((int(id_), tipo, int(s) if s not in ("null", "-") else None, v,
                   int(rest) if rest not in ("null", "-") else None))
    return ej


def segundos(ej):
    if not ej:
        return 0
    t = 300 + 60 * (len(ej) - 1)
    for _, tipo, s, v, r in ej:
        s1 = s or 1
        t += s1 * int(v.rstrip("s")) if tipo == "DURATION" else s1 * int(v) * 3
        t += max(0, s1 - 1) * (r if r is not None else 60)
    return t


res = {r["id"]: r for r in csv.DictReader(open(os.path.join(c, "resultados_20.csv")))}
perf = {}
if perf_csv:
    perf = {r["id"]: r for r in csv.DictReader(open(perf_csv))}
minutos = {u: int(re.search(r"(\d+) min", r["perfil"]).group(1)) for u, r in res.items() if r.get("perfil")}
dec = {r["id"]: r for r in csv.DictReader(open(os.path.join(c, "decisiones.csv")))}
ini = list(csv.DictReader(open(os.path.join(c, "plan_inicial.csv"))))
sem2 = {r["id"]: r for r in csv.DictReader(open(os.path.join(c, "semana2.csv")))} if os.path.exists(
    os.path.join(c, "semana2.csv")) else {}


def indices_s2(u):
    """S-indices senalados en la semana 2: ids de targeted_sessions frente al orden por fecha del plan de S2."""
    t = sem2.get(u, {}).get("targeted_sessions") or ""
    ids = [int(x) for x in re.findall(r"\d+", t)]
    plan = sem2.get(u, {}).get("plan_medido_s2")
    filas = [r for r in ini if r["id"] == u and r["plan_id"] == str(plan)]
    orden = sorted({(r["scheduled_date"], int(r["planned_workout_id"])) for r in filas})
    return {f"S{i + 1}" for i, (_, w) in enumerate(orden) if w in ids}


viol = dict(cambios_no_senaladas=0, dosis_no_autorizada=0, descanso_cambiado=0, bajo_minimo_v20=0,
            violaciones_linaje=0)
limite = []
for semana, fichero in ((1, "plan_siguiente.csv"), (2, "plan_siguiente_semana2.csv")):
    p = os.path.join(c, fichero)
    if not os.path.exists(p):
        continue
    for x in csv.DictReader(open(p)):
        u = x["id"]
        if x["igual_a_la_medida"] == "true":
            continue
        senaladas = set((dec[u].get("indices_senalados") or "").split("|")) if semana == 1 else indices_s2(u)
        if x["sesion"] not in senaladas:
            viol["cambios_no_senaladas"] += 1
        a, b = sesion(x["medida"]), sesion(x["siguiente"])
        antes = {e[0]: e for e in a}
        for e in b:
            o = antes.get(e[0])
            if o:
                if (e[2] or 0) > (o[2] or 0) or float(e[3].rstrip("s")) > float(o[3].rstrip("s")):
                    viol["dosis_no_autorizada"] += 1
                if e[4] != o[4]:
                    viol["descanso_cambiado"] += 1
        m = minutos.get(u)
        minimo = -(-m * 60 * 70 // 100) if m else None
        ta, tb = segundos(a), segundos(b)
        if minimo and tb < minimo:
            viol["bajo_minimo_v20"] += 1
        if minimo and tb == minimo:
            limite.append(dict(semana=semana, usuario=u, sesion=x["sesion"], minutos=m, duracion_antes_s=ta,
                               duracion_despues_s=tb, minimo_v20_s=minimo, margen_inicial_s=ta - minimo,
                               margen_restante_s=0, cambios=x["siguiente"]))
for r in res.values():
    viol["violaciones_linaje"] += int(r.get("violaciones_linaje") or 0)
for r in sem2.values():
    viol["violaciones_linaje"] += int(r.get("violaciones_linaje") or 0)
print(json.dumps(dict(carpeta=os.path.basename(c.rstrip("/")), violaciones=viol, sesiones_en_minimo=limite),
                 ensure_ascii=False))
