#!/usr/bin/env python3
"""Calidad de las adaptaciones de origen IA (auditoria post R/R'). Uso: python3 -I calidad_56.py SESIONES_FINALES.csv DIFERENCIAS_R_RP.csv"""
import csv, sys, collections, json, statistics as st
r = list(csv.DictReader(open(sys.argv[1], encoding="utf-8")))
ia = [x for x in r if x["origen"] == "IA"]
print("sesiones IA:", len(ia), "categorias:", dict(collections.Counter(x["categoria"] for x in ia)))
for k in sorted({x["intervencion"] for x in ia}):
    xs = [x for x in ia if x["intervencion"] == k]
    red = [100 * (1 - int(x["trabajo_despues_s"]) / int(x["trabajo_antes_s"])) for x in xs]
    ns = [len(json.loads(x["sustituciones"])) for x in xs]
    nr = [len(json.loads(x["reducciones"])) for x in xs]
    print(k, len(xs), "reduccion % min/mediana/max", round(min(red), 1), round(st.median(red), 1), round(max(red), 1),
          "sustituciones", dict(sorted(collections.Counter(ns).items())), "reducciones", dict(sorted(collections.Counter(nr).items())))
print("no IA:")
for x in r:
    if x["origen"] != "IA":
        print(" ", x["corrida"], x["id"], x["sesion"], x["intervencion"], x["origen"], x["factible_sonda"])
d = list(csv.DictReader(open(sys.argv[2], encoding="utf-8")))
print("R vs R':", len(d), dict(collections.Counter((x["igual_categoria"], x["igual_cambio"]) for x in d)))
for x in d:
    if x["igual_cambio"] == "False":
        print(" ", x["id"], x["sesion"], x["intervencion"], x["R_categoria"], x["Rp_categoria"])
