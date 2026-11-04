#!/usr/bin/env python3
"""Construye 04_MATRIZ_20_USUARIOS.csv y las cifras de los entregables a partir de las corridas finales ya juzgadas
por el oraculo (evidencia/final/<corrida>/oraculo). No recalcula nada: solo agrega.

Uso: python3 -I herramientas/informe_k20.py   (desde evaluacion_final_k20/)
"""
import csv
import json
import os
import sys

csv.field_size_limit(sys.maxsize)
RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
F = os.path.join(RAIZ, "evidencia", "final")


def leer(r):
    with open(r, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def det(corrida):
    with open(os.path.join(F, corrida, "oraculo", "detalle.json"), encoding="utf-8") as f:
        return {d["id"]: d for d in json.load(f)}


def ses(corrida):
    out = {}
    for r in leer(os.path.join(F, corrida, "oraculo", "sesiones.csv")):
        out.setdefault(r["id"], []).append(r)
    return out


def estado(problemas):
    return "OK" if not problemas else "FAIL"


def main():
    D, S = det("D"), det("S_control")
    sD, sS = ses("D"), ses("S_control")
    teorico = {r["id"]: r for r in leer(os.path.join(RAIZ, "entrada", "04_ORACULO_TEORICO_HISTORICO.csv"))}
    perfiles = leer(os.path.join(RAIZ, "entrada", "03_PERFILES_U01_U20.csv"))
    hashes = {c: open(os.path.join(F, c, "oraculo", "hash_normalizado.txt")).read().strip()
              for c in ("D", "D_prima", "S_control")}
    filas = []
    for p in perfiles:
        u = p["id"]
        d, s = D[u], S[u]
        ch = d["comprobaciones"]
        seguridad = [x for k in ("E_adaptacion", "G_continuidad") for x in ch.get(k, [])
                     if "SEGURIDAD" in x or "NO senalada" in x]
        filas.append({
            "usuario": u, "objetivo": p["goal_type"], "patron": p["patron"] + " " + p["secuencia"],
            "generacion": estado(ch.get("A_generacion")), "registro": estado(ch.get("B_registro")),
            "adherencia": estado(ch.get("C_adherencia")), "decision": estado(ch.get("D_decision")),
            "adaptacion": estado(ch.get("E_adaptacion")), "seguridad": estado(seguridad),
            "publicacion": estado(ch.get("F_publicacion")), "continuidad": estado(ch.get("G_continuidad")),
            "resultado_D": d["estado"], "resultado_D_prima": det("D_prima")[u]["estado"],
            "Aw": d["adherencia"]["Aw"], "Aobs": d["adherencia"]["Aobs"], "C": d["adherencia"]["C"],
            "decision_politica": d["decision"]["backend"],
            "decision_teorica_historica": teorico[u]["decision_esperada_backend"],
            "senaladas": "|".join(d["decision"]["senaladas"]) or "-",
            "desenlace_D": d["v32"]["status"],
            "origen_D": "|".join(f"{r['sesion']}:{r['origen']}/{r['categoria']}" for r in sD.get(u, [])) or "-",
            "control_IA_simulada": s["estado"] + " " + s["v32"]["status"] + " "
            + ("|".join(f"{r['sesion']}:{r['origen']}/{r['categoria']}/factible={r['factible_sonda'] or '-'}"
                        for r in sS.get(u, [])) or "-"),
            "adaptacion_efectiva_IA_real": "PENDIENTE (R/R' no ejecutadas)" if sD.get(u) and any(
                r["intervencion"] not in ("CAUSA_NO_REGISTRADA",) for r in sD[u]) else "no aplica",
        })
    with open(os.path.join(RAIZ, "04_MATRIZ_20_USUARIOS.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(filas[0].keys()))
        w.writeheader()
        w.writerows(filas)
    cuenta = lambda col, val: sum(1 for x in filas if x[col] == val)
    print("hashes", json.dumps(hashes))
    print("PASS D", cuenta("resultado_D", "PASS"), "PASS D'", cuenta("resultado_D_prima", "PASS"),
          "PASS S", sum(1 for u in S.values() if u["estado"] == "PASS"))
    print("coincide teorico", sum(1 for x in filas if x["decision_politica"] == x["decision_teorica_historica"]))
    todas = [r for v in sD.values() for r in v]
    print("sesiones senaladas D", len(todas), {o: sum(1 for r in todas if r["origen"] == o) for o in {r["origen"] for r in todas}})
    todasS = [r for v in sS.values() for r in v]
    print("sesiones senaladas S", len(todasS), {o: sum(1 for r in todasS if r["origen"] == o) for o in {r["origen"] for r in todasS}},
          {c: sum(1 for r in todasS if r["categoria"] == c) for c in {r["categoria"] for r in todasS}},
          "factibles", sum(1 for r in todasS if r["factible_sonda"] == "si"))


if __name__ == "__main__":
    main()
