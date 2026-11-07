#!/usr/bin/env python3
"""Reproducibilidad R frente a R' por sesion senalada (descriptivo, sin umbral; 23 §5 «Comparaciones»). Solo lectura.
Uso: python3 -I reproducibilidad_r_rp.py <salida.csv> <R> <Rp> [<R> <Rp> ...]"""
import csv, json, os, sys
sys.dont_write_bytecode = True
O = "oraculo_PLAN-REV-2.0+DIAS-1"
filas, res = [], []
args = sys.argv[2:]
for a, b in zip(args[0::2], args[1::2]):
    lr = {(r["id"], r["sesion"]): r for r in csv.DictReader(open(os.path.join(a, O, "sesiones.csv"), encoding="utf-8"))}
    lp = {(r["id"], r["sesion"]): r for r in csv.DictReader(open(os.path.join(b, O, "sesiones.csv"), encoding="utf-8"))}
    claves = sorted(set(lr) | set(lp))
    n = ig_cat = ig_cambio = ig_dec = 0
    for k in claves:
        x, y = lr.get(k, {}), lp.get(k, {})
        cx = (sorted(json.dumps(s, sort_keys=True) for s in json.loads(x.get("sustituciones") or "[]")), x.get("reducciones"))
        cy = (sorted(json.dumps(s, sort_keys=True) for s in json.loads(y.get("sustituciones") or "[]")), y.get("reducciones"))
        dec = (x.get("intervencion"), x.get("origen"), x.get("cambio")) == (y.get("intervencion"), y.get("origen"), y.get("cambio"))
        n += 1; ig_cat += x.get("categoria") == y.get("categoria"); ig_cambio += cx == cy; ig_dec += dec
        filas.append([os.path.basename(a), k[0], k[1], x.get("intervencion") or y.get("intervencion"), x.get("origen"), y.get("origen"),
                      x.get("categoria"), y.get("categoria"), dec, x.get("categoria") == y.get("categoria"), cx == cy])
    res.append((os.path.basename(a), os.path.basename(b), n, ig_dec, ig_cat, ig_cambio))
with open(sys.argv[1], "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f, lineterminator="\n")
    w.writerow(["corrida_R", "id", "sesion", "intervencion", "origen_R", "origen_Rp", "categoria_R", "categoria_Rp",
                "misma_decision", "misma_categoria", "mismo_cambio"]); w.writerows(filas)
for r in res:
    print(f"{r[0]} vs {r[1]}: sesiones {r[2]} | misma decision {r[3]}/{r[2]} | misma categoria {r[4]}/{r[2]} | mismo cambio exacto {r[5]}/{r[2]}")
