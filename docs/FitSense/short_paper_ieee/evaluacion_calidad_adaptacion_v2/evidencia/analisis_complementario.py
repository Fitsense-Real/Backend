"""Fase con Replicate sobre R01-R08 (RC1_r1, RC2_r1): genera 12/13 y la auditoria de sustituciones de las dos
semanas (evidencia/auditoria_sustituciones_RC.csv). Solo lectura. Uso: python3 -I analisis_complementario.py <carpeta 09-16>"""
import csv, json, os, re, sys

B = sys.argv[1]
EV = os.path.join(B, "evidencia")
CAT = {int(r["exercise_id"]): r for r in csv.DictReader(open(os.path.join(EV, "catalogo_exercises_v44_2.csv")))}
PERF = {r["id"]: r for r in csv.DictReader(open(os.path.join(B, "10_PERFILES_SINTETICOS_R01_R08.csv")))}
INTERV = {"TOO_DIFFICULT": "VARIANTE_MAS_SENCILLA_O_CARGA", "FATIGUE": "REVISAR_CARGA_Y_RECUPERACION",
          "LACK_OF_MOTIVATION": "INVESTIGAR_CAMBIO_MINIMO"}
PRECIO_IN, PRECIO_OUT = 0.25e-6, 2.00e-6


def sesion(f):
    return [(int(a), t, int(s), v, int(r)) for a, t, s, v, r in re.findall(r"(\d+):(\w+):(\d+)x([^:]+):r(\d+)", f)]


def motivo(u, ses, semana):
    p = PERF[u]
    if semana == 2:
        m2 = p.get("motivo_omision_s2_semana2") or p["motivo_omision_s2"]
        m3 = p.get("motivo_omision_s3_semana2") or p["motivo_omision_s3"]
    else:
        m2, m3 = p["motivo_omision_s2"], p["motivo_omision_s3"]
    return m2 if ses == "S2" else m3 if ses == "S3" else ""


subs_rows = []
for corrida, cond, fichero in (("RC1_r1", "C1", "12_RESULTADOS_C1_COMPLEMENTARIOS.csv"),
                               ("RC2_r1", "C2", "13_RESULTADOS_C2_COMPLEMENTARIOS.csv")):
    c = os.path.join(EV, corrida)
    res = {r["id"]: r for r in csv.DictReader(open(os.path.join(c, "resultados_20.csv")))}
    dec = {r["id"]: r for r in csv.DictReader(open(os.path.join(c, "decisiones.csv")))}
    s2 = {r["id"]: r for r in csv.DictReader(open(os.path.join(c, "semana2.csv")))}
    uid = {r["user_id"]: r["id"] for r in res.values()}
    log = open(os.path.join(c, "ejecucion_maven.log"), encoding="utf-8", errors="replace").read()
    idx = {p["id"]: p for p in json.load(open(os.path.join(c, "predicciones_replicate", "indice.json")))["predicciones"]}
    tim = {}
    for m in re.finditer(r"REPLICATE_TIMING user=(\d+) week=(\d+) attempt=(\d+) prediction_id=(\w+) .*?total_ms=(\d+)", log):
        u = uid[m.group(1)]
        sem = 1 if m.group(2) == "2" else 2
        tim.setdefault((u, sem), []).append((int(m.group(3)), m.group(4), int(m.group(5))))
    rech = {}
    for m in re.finditer(r"Intento (\d) para el usuario (\d+): la semana propuesta no pasa \S* \[(.*)\]\n", log):
        fecha = re.search(r"2026-11-(\d\d)", m.group(3))
        sem = 2 if fecha and int(fecha.group(1)) >= 16 else 1
        rech.setdefault((uid[m.group(2)], sem), []).append(
            ",".join(sorted(set(re.findall(r"\b(V\d+[A-Z]?):", m.group(3))))))
    filas = []
    for u in sorted(res):
        for sem in (1, 2):
            if sem == 1:
                r, d = res[u], dec.get(u, {})
                estado, decision, interv = r.get("estado_v32"), r.get("decision"), d.get("targeted_interventions")
                cambiadas = r.get("sesiones_cambiadas")
            else:
                y = s2.get(u, {})
                estado, decision, interv, cambiadas = y.get("status"), y.get("decision"), y.get("targeted_interventions"), \
                    y.get("sesiones_cambiadas_s3")
            pr = tim.get((u, sem), [])
            ti = sum(idx.get(p[1], {}).get("tokens_entrada") or 0 for p in pr)
            to = sum(idx.get(p[1], {}).get("tokens_salida") or 0 for p in pr)
            filas.append(dict(corrida=corrida, condicion=cond, id=u, semana=sem, generacion_inicial=res[u].get(
                "generacion_inicial"), decision=decision, intervenciones=interv, estado_v32=estado,
                llamadas=len(pr), rechazos=len(rech.get((u, sem), [])), codigos_rechazo=" | ".join(rech.get((u, sem), [])),
                sesiones_cambiadas=cambiadas, prediccion_ids=" ".join(p[1] for p in pr), ms_total=sum(p[2] for p in pr),
                tokens_entrada=ti, tokens_salida=to, usd=round(ti * PRECIO_IN + to * PRECIO_OUT, 4)))
    with open(os.path.join(B, fichero), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(filas[0]))
        w.writeheader()
        w.writerows(filas)
    for sem, fn in ((1, "plan_siguiente.csv"), (2, "plan_siguiente_semana2.csv")):
        for x in csv.DictReader(open(os.path.join(c, fn))):
            if x["igual_a_la_medida"] == "true":
                continue
            a, b = sesion(x["medida"]), sesion(x["siguiente"])
            mot = motivo(x["id"], x["sesion"], sem)
            for i in range(max(len(a), len(b))):
                ea, eb = (a[i] if i < len(a) else None), (b[i] if i < len(b) else None)
                if ea and eb and ea[0] == eb[0]:
                    if (ea[2], ea[3]) != (eb[2], eb[3]):
                        subs_rows.append(dict(corrida=corrida, semana=sem, usuario=x["id"], sesion=x["sesion"],
                                              motivo=mot, intervencion=INTERV.get(mot, ""), tipo="DOSIS",
                                              original=ea[0], nuevo=eb[0], dosis=f"{ea[2]}x{ea[3]}->{eb[2]}x{eb[3]}",
                                              patron_o="", patron_n="", musculo_o="", musculo_n="", dif_o="", dif_n="",
                                              clase=""))
                    continue
                o, n = CAT.get(ea[0]) if ea else None, CAT.get(eb[0]) if eb else None
                mismo_m = o and n and o["target_muscle"] == n["target_muscle"]
                clase = ("Q4" if not (o and n) or o["movement_pattern"] != n["movement_pattern"]
                         or int(n["difficulty_level"]) > int(o["difficulty_level"]) else
                         "Q1" if mismo_m and int(n["difficulty_level"]) < int(o["difficulty_level"]) else "Q2")
                subs_rows.append(dict(corrida=corrida, semana=sem, usuario=x["id"], sesion=x["sesion"], motivo=mot,
                                      intervencion=INTERV.get(mot, ""), tipo="SUSTITUCION", original=ea[0] if ea else "",
                                      nuevo=eb[0] if eb else "",
                                      dosis=f"{ea[2]}x{ea[3]}->{eb[2]}x{eb[3]}" if ea and eb else "",
                                      patron_o=o["movement_pattern"] if o else "", patron_n=n["movement_pattern"] if n else "",
                                      musculo_o=o["target_muscle"] if o else "", musculo_n=n["target_muscle"] if n else "",
                                      dif_o=o["difficulty_level"] if o else "", dif_n=n["difficulty_level"] if n else "",
                                      clase=clase))
with open(os.path.join(EV, "auditoria_sustituciones_RC.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=list(subs_rows[0]))
    w.writeheader()
    w.writerows(subs_rows)
from collections import Counter
print(Counter((r["corrida"], r["tipo"], r["clase"]) for r in subs_rows))
for r in subs_rows:
    if r["tipo"] == "SUSTITUCION" and r["clase"] != "Q1":
        print(r["corrida"], "s" + str(r["semana"]), r["usuario"], r["sesion"], r["original"], "->", r["nuevo"],
              r["patron_o"], r["musculo_o"], "->", r["musculo_n"], r["dif_o"], "->", r["dif_n"], r["clase"])
