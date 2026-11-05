#!/usr/bin/env python3
"""Comparacion DEV-R (V2 frente a V2.1) a partir de las carpetas del arnes, ya pasadas por el oraculo v2.

Solo LEE la evidencia (oraculo/sesiones.csv, oraculo/matriz_20.csv, oraculo/detalle.json, intentos_ia.csv,
predicciones_metricas.csv) y escribe la tabla de metricas y el detalle por sesion. No cambia ningun dato.
Uso: python3 -I comparar_dev_r.py <carpeta_v2> <carpeta_v2.1> <salida_dir>
"""
import csv
import json
import os
import statistics
import sys
from collections import Counter, defaultdict

CAUSA = {"VARIANTE_MAS_SENCILLA_O_CARGA": "demasiado dificil", "REVISAR_CARGA_Y_RECUPERACION": "fatiga",
         "INVESTIGAR_CAMBIO_MINIMO": "desmotivacion", "SEGURIDAD_ANTE_DOLOR": "dolor",
         "CAUSA_NO_REGISTRADA": "sin causa"}


def leer(ruta):
    with open(ruta, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def p90(v):
    if not v:
        return None
    v = sorted(v)
    return v[min(len(v) - 1, int(round(0.9 * (len(v) - 1))))]


def analizar(carpeta):
    ses = leer(os.path.join(carpeta, "oraculo", "sesiones.csv"))
    mat = leer(os.path.join(carpeta, "oraculo", "matriz_20.csv"))
    det = json.load(open(os.path.join(carpeta, "oraculo", "detalle.json"), encoding="utf-8"))
    intentos = leer(os.path.join(carpeta, "intentos_ia.csv"))
    met_path = os.path.join(carpeta, "predicciones_metricas.csv")
    met = leer(met_path) if os.path.exists(met_path) else []
    m = {}
    m["recorridos_PASS"] = sum(r["estado"] == "PASS" for r in mat)
    m["recorridos_total"] = len(mat)
    incid = [(d["id"], etapa, x) for d in det for etapa, l in d["comprobaciones"].items() for x in l]
    m["incumplimientos_oraculo"] = len(incid)
    m["incumplimientos_detalle"] = "; ".join(f"{i}/{e}: {x}" for i, e, x in incid)
    m["sesiones_senaladas"] = len(ses)
    fact = [r for r in ses if r["factible_sonda"] == "si"]
    m["sesiones_factibles"] = len(fact)
    m["sesiones_no_factibles"] = len(ses) - len(fact)
    efectiva = lambda r: r["origen"] == "IA" and r["cambio"] == "si" and r["categoria"] in ("ACC", "ALT")
    m["efectivas_pertinentes_IA"] = sum(efectiva(r) for r in ses)
    m["efectivas_pertinentes_IA_en_factibles"] = sum(efectiva(r) for r in fact)
    for c in ("ACC", "ALT", "INC", "CONS-J", "CONS-POL"):
        m["cat_" + c] = sum(r["categoria"] == c for r in ses)
    m["cat_ACC_origen_IA"] = sum(r["categoria"] == "ACC" and r["origen"] == "IA" for r in ses)
    m["cat_ALT_origen_IA"] = sum(r["categoria"] == "ALT" and r["origen"] == "IA" for r in ses)
    m["origenes"] = dict(Counter(r["origen"] for r in ses))
    # dificultad: sesiones "demasiado dificil"
    dif = [r for r in ses if r["intervencion"] == "VARIANTE_MAS_SENCILLA_O_CARGA"]
    baja = sube = solo_dosis = nada = 0
    sust_total = sust_mismo = 0
    for r in ses:
        for s in json.loads(r["sustituciones"] or "[]"):
            sust_total += 1
            sust_mismo += bool(s.get("musculo_de")) and s.get("musculo_de") == s.get("musculo_a")
    for r in dif:
        su = json.loads(r["sustituciones"] or "[]")
        re_ = json.loads(r["reducciones"] or "[]")
        if any(s["dif_a"] is not None and s["dif_de"] is not None and s["dif_a"] > s["dif_de"] for s in su):
            sube += 1
        if any(s["dif_a"] is not None and s["dif_de"] is not None and s["dif_a"] < s["dif_de"] for s in su):
            baja += 1
        elif re_:
            solo_dosis += 1
        elif r["cambio"] != "si":
            nada += 1
    m["dificil_sesiones"] = len(dif)
    m["dificil_con_variante_menos_dificil"] = baja
    m["dificil_solo_reduccion_de_dosis"] = solo_dosis
    m["dificil_sin_cambio"] = nada
    m["dificil_con_sustituto_mas_dificil"] = sube
    m["sustituciones_total"] = sust_total
    m["sustituciones_mismo_musculo"] = sust_mismo
    # desmotivacion: ejercicios cambiados por sesion
    mot = [r for r in ses if r["intervencion"] == "INVESTIGAR_CAMBIO_MINIMO"]
    camb = [len(json.loads(r["sustituciones"] or "[]")) + len(json.loads(r["reducciones"] or "[]")) for r in mot
            if r["cambio"] == "si"]
    m["desmotivacion_sesiones"] = len(mot)
    m["desmotivacion_con_cambio"] = len(camb)
    m["desmotivacion_cambio_de_1"] = sum(c <= 1 for c in camb)
    m["desmotivacion_cambio_de_2"] = sum(c == 2 for c in camb)
    m["desmotivacion_mas_de_2"] = sum(c > 2 for c in camb)
    # propuestas de la IA
    ia = [r for r in intentos if r["intento"] not in ("0", "")]
    m["propuestas_IA_juzgadas"] = len(ia)
    m["propuestas_IA_publicables"] = sum(r["publicable"] == "true" for r in ia)
    m["propuestas_IA_rechazadas"] = sum(r["publicable"] != "true" for r in ia)
    m["rechazos_por_motivo"] = dict(Counter(r["motivo"] for r in ia if r["publicable"] != "true"))
    ciclos = defaultdict(int)
    for r in ia:
        ciclos[r["etiqueta"]] = max(ciclos[r["etiqueta"]], int(r["intento"]))
    m["ciclos_con_IA"] = len(ciclos)
    m["intentos_por_ciclo"] = dict(Counter(ciclos.values()))
    # coste y latencia
    m["predicciones"] = len(met)
    m["tokens_entrada"] = sum(int(r["tokens_entrada"]) for r in met if int(r["tokens_entrada"]) >= 0)
    m["tokens_salida"] = sum(int(r["tokens_salida"]) for r in met if int(r["tokens_salida"]) >= 0)
    m["usd_medido"] = round(sum(float(r["usd"]) for r in met), 4)
    lat = [int(r["ms"]) / 1000 for r in met]
    m["latencia_mediana_s"] = round(statistics.median(lat), 1) if lat else None
    m["latencia_p90_s"] = round(p90(lat), 1) if lat else None
    m["coste_estados"] = dict(Counter(r["estado"] for r in met))
    return m, ses


def main():
    a, b, out = sys.argv[1], sys.argv[2], sys.argv[3]
    os.makedirs(out, exist_ok=True)
    ma, sa = analizar(a)
    mb, sb = analizar(b)
    with open(os.path.join(out, "COMPARACION_DEV_R.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["metrica", "V2", "V2.1"])
        for k in ma:
            w.writerow([k, json.dumps(ma[k], ensure_ascii=False) if isinstance(ma[k], dict) else ma[k],
                        json.dumps(mb[k], ensure_ascii=False) if isinstance(mb[k], dict) else mb[k]])
    idx = {(r["id"], r["sesion"]): r for r in sb}
    with open(os.path.join(out, "DEV_R_SESIONES.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["id", "sesion", "causa", "intervencion", "factible_sonda", "V2_origen", "V2_cambio", "V2_categoria",
                    "V2_sustituciones", "V2_reducciones", "V21_origen", "V21_cambio", "V21_categoria",
                    "V21_sustituciones", "V21_reducciones"])
        for r in sa:
            q = idx.get((r["id"], r["sesion"]), {})
            w.writerow([r["id"], r["sesion"], CAUSA.get(r["intervencion"], r["intervencion"]), r["intervencion"],
                        r["factible_sonda"], r["origen"], r["cambio"], r["categoria"], r["sustituciones"],
                        r["reducciones"], q.get("origen"), q.get("cambio"), q.get("categoria"),
                        q.get("sustituciones"), q.get("reducciones")])
    for k in ma:
        print(f"{k:42} {ma[k]!s:40.40} {mb[k]!s:40.40}")


if __name__ == "__main__":
    main()
