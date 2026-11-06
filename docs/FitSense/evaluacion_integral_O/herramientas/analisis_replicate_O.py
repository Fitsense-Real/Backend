#!/usr/bin/env python3
"""Analisis de la evidencia de Replicate REAL en la evaluacion integral O (SOLO LECTURA; no evalua reglas: eso lo
hace el oraculo). Resume predicciones (identificador, tokens, latencia, coste), clasifica cada ciclo con orden de
adaptar que requiere IA (aceptada en el intento 1, aceptada tras reintento, SIN_EFECTO propuesto por la IA,
respaldo determinista, error del proveedor), cruza la pertinencia calculada por el oraculo y compara corridas.

Uso:
  python3 -I analisis_replicate_O.py <salida_dir> <SIM_oraculo_dir> \
      <ETIQ>=<dir_G>,<dir_P>,<dir_F35>,<oraculo_dir>,<oraculo_F35_dir> [<ETIQ2>=...]
La primera corrida ETIQ es la principal (R_O); la segunda, si existe, la de reproducibilidad (R'_O).
"""
import csv
import json
import os
import sys
from collections import Counter, defaultdict
from datetime import date

csv.field_size_limit(sys.maxsize)
S2 = date(2026, 11, 9)
# Igual que H.REQUIERE_IA del oraculo historico (oraculo_k20.py), que usa el oraculo integral.
REQUIERE_IA = {"REVISAR_CARGA_Y_RECUPERACION", "VARIANTE_MAS_SENCILLA_O_CARGA", "INVESTIGAR_CAMBIO_MINIMO",
               "ACORTAR_O_REORGANIZAR", "REPROGRAMAR_DIAS", "SEGURIDAD_ANTE_DOLOR"}


def leer(ruta):
    if not os.path.exists(ruta):
        return []
    with open(ruta, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def escribir(ruta, filas, campos=None):
    if not filas:
        open(ruta, "w").close()
        return
    campos = campos or list(dict.fromkeys(k for f in filas for k in f))
    with open(ruta, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=campos, lineterminator="\n")
        w.writeheader()
        w.writerows(filas)


def lista(v):
    v = (v or "").strip().strip("{}[]")
    return [x.strip().strip('"') for x in v.split(",") if x.strip()]


def corrida(etiq, dirs):
    """Lee una corrida (bloques G, P y F35) y su oraculo."""
    g, p, f35, ora, ora_f = (dirs + [None] * 5)[:5]
    preds, ciclos, trazas = [], [], []
    cfgs = {}
    adapt = {}
    oraculos_leidos = set()
    for d, o in ((g, ora), (p, ora), (f35, ora_f)):
        if not d or not os.path.isdir(d):
            continue
        bloque = os.path.basename(d.rstrip("/"))
        cfgs[bloque] = dict(l.rstrip("\n").split(": ", 1) for l in open(os.path.join(d, "00_configuracion.txt"),
                                                                         encoding="utf-8") if ": " in l)
        usuarios = {r["user_id"]: r["id"] for r in leer(os.path.join(d, "usuarios.csv"))}
        for r in leer(os.path.join(d, "predicciones_metricas.csv")):
            preds.append({"corrida": etiq, "bloque": bloque, **r})
        tr = os.path.join(d, "trazas_ia.jsonl")
        if os.path.exists(tr):
            for l in open(tr, encoding="utf-8"):
                if l.strip():
                    t = json.loads(l)
                    trazas.append({"corrida": etiq, "bloque": bloque, "etiqueta": t.get("etiqueta"),
                                   "tipo": t.get("tipo"), "intento": t.get("intento"), "resultado": t.get("resultado"),
                                   "ms": t.get("ms"), "error": t.get("error", "")})
        intentos = defaultdict(list)
        for r in leer(os.path.join(d, "intentos_ia.csv")):
            if r["intento"] != "0":
                intentos[r["etiqueta"]].append(r)
        pm = defaultdict(list)
        for r in leer(os.path.join(d, "predicciones_metricas.csv")):
            pm[r["etiqueta"]].append(r)
        if o and o not in oraculos_leidos:   # G y P comparten oraculo: se lee una sola vez
            oraculos_leidos.add(o)
            for r in leer(os.path.join(o, "adaptaciones.csv")):
                adapt.setdefault((etiq, r["id"], int(r["ciclo"])), []).append(r)
        for r in leer(os.path.join(d, "tablas", "plan_adaptation_attempts.csv")):
            pid = usuarios.get(r["user_id"])
            if pid is None:
                continue
            k = (date.fromisoformat(r["new_week_start_date"]) - S2).days // 7 + 1
            ints = set(lista(r["targeted_interventions"]))
            if not (int(r["ai_calls"] or 0) > 0 or ints & REQUIERE_IA):
                continue
            et = f"{pid}/ciclo{k}"
            its = sorted(intentos.get(et, []), key=lambda x: int(x["intento"]))
            pub = [x for x in its if x["publicable"] == "true"]
            rech = [x for x in its if x["publicable"] != "true"]
            errores = [t for t in trazas if t["corrida"] == etiq and t["etiqueta"] == et and t["tipo"] == "ADAPTACION"
                       and t["resultado"] != "RESPUESTA"]
            if r["status"] == "APLICADA" and r["generation_source"] == "AI":
                clase = "ACEPTADA_INTENTO_1" if pub and pub[0]["intento"] == "1" else "ACEPTADA_TRAS_REINTENTO"
            elif r["status"] == "SIN_EFECTO" and any(x["motivo"] == "SIN_EFECTO" and x["publicable"] == "true" for x in its):
                clase = "SIN_EFECTO_PROPUESTO_POR_IA"
            elif r["status"] == "SIN_EFECTO":
                clase = "SIN_EFECTO_SIN_PROPUESTA_IA"
            elif r["status"] == "RESPALDO":
                clase = "RESPALDO_TRAS_ERROR_PROVEEDOR" if errores and not its else "RESPALDO_TRAS_RECHAZOS"
            else:
                clase = r["status"]
            ad = adapt.get((etiq, pid, k), [])
            ciclos.append({
                "corrida": etiq, "bloque": bloque, "id": pid, "ciclo": k, "decision": r["decision"],
                "sesiones_senaladas": ";".join(lista(r["targeted_sessions"])),
                "intervenciones": ";".join(sorted(ints)), "llamadas_ia": r["ai_calls"],
                "predicciones": ";".join(x["prediction_id"] for x in pm.get(et, [])),
                "tokens_entrada": sum(int(x["tokens_entrada"] or 0) for x in pm.get(et, []) if x["tokens_entrada"] not in ("", "-1")),
                "tokens_salida": sum(int(x["tokens_salida"] or 0) for x in pm.get(et, []) if x["tokens_salida"] not in ("", "-1")),
                "usd": round(sum(float(x["usd"] or 0) for x in pm.get(et, [])), 6),
                "latencia_ms": ";".join(x["ms"] for x in pm.get(et, [])),
                "intentos": " | ".join(f"i{x['intento']} {'PUBLICABLE' if x['publicable'] == 'true' else 'RECHAZADO'} "
                                       f"{x['motivo']}" + (f" [{x['problemas'][:160]}]" if x['problemas'] else "")
                                       for x in its) or "-",
                "rechazos": len(rech), "errores_proveedor": len(errores),
                "estado_v32": r["status"], "fuente": r["generation_source"], "motivo_rechazo_v32": r["rejection_reason"],
                "clasificacion": clase,
                "pertinencia": ";".join(f"{a['sesion']}:{a['intervencion']}:{a['pertinencia']}" for a in ad) or "-",
                "cambios": " | ".join(f"{a['sesion']}: sus {a['sustituciones']} red {a['reducciones']} ret {a['retirados']} "
                                      f"trabajo {a['trabajo_antes_s']}->{a['trabajo_despues_s']}" for a in ad) or "-",
                "problemas_oraculo": " | ".join(a["problemas"] for a in ad if a["problemas"] != "-") or "-"})
    return {"preds": preds, "ciclos": ciclos, "trazas": trazas, "cfgs": cfgs}


def resumen(etiq, c):
    cl = Counter(x["clasificacion"] for x in c["ciclos"])
    per = Counter(p.split(":")[-1] for x in c["ciclos"] for p in x["pertinencia"].split(";") if p != "-")
    n = len(c["ciclos"])
    acept = cl["ACEPTADA_INTENTO_1"] + cl["ACEPTADA_TRAS_REINTENTO"]
    preds = c["preds"]
    usd = sum(float(p["usd"] or 0) for p in preds)
    lat = sorted(int(p["ms"]) for p in preds if p.get("ms") not in (None, "", "-1"))
    return {"corrida": etiq, "ciclos_con_orden_de_IA": n, "predicciones": len(preds),
            "tokens_entrada": sum(int(p["tokens_entrada"]) for p in preds if p["tokens_entrada"] not in ("", "-1")),
            "tokens_salida": sum(int(p["tokens_salida"]) for p in preds if p["tokens_salida"] not in ("", "-1")),
            "usd_medido": round(usd, 4),
            "latencia_mediana_s": round(lat[len(lat) // 2] / 1000, 1) if lat else "-",
            "latencia_max_s": round(lat[-1] / 1000, 1) if lat else "-",
            "aceptadas_intento_1": cl["ACEPTADA_INTENTO_1"], "aceptadas_tras_reintento": cl["ACEPTADA_TRAS_REINTENTO"],
            "aceptacion_final": f"{acept}/{n}" if n else "-",
            "sin_efecto_propuesto_por_ia": cl["SIN_EFECTO_PROPUESTO_POR_IA"],
            "sin_efecto_sin_propuesta": cl["SIN_EFECTO_SIN_PROPUESTA_IA"],
            "respaldo_tras_rechazos": cl["RESPALDO_TRAS_RECHAZOS"], "respaldo_tras_error": cl["RESPALDO_TRAS_ERROR_PROVEEDOR"],
            "otros": sum(v for k, v in cl.items() if k not in ("ACEPTADA_INTENTO_1", "ACEPTADA_TRAS_REINTENTO",
                     "SIN_EFECTO_PROPUESTO_POR_IA", "SIN_EFECTO_SIN_PROPUESTA_IA", "RESPALDO_TRAS_RECHAZOS",
                     "RESPALDO_TRAS_ERROR_PROVEEDOR")),
            "intentos_rechazados": sum(x["rechazos"] for x in c["ciclos"]),
            "errores_proveedor": sum(1 for t in c["trazas"] if t["resultado"] not in ("RESPUESTA", None)),
            "pertinencia_ACC": per["ACC"], "pertinencia_ALT": per["ALT"], "pertinencia_CONS-J": per["CONS-J"],
            "pertinencia_INC": per["INC"]}


def main():
    salida, sim_ora = sys.argv[1], sys.argv[2]
    os.makedirs(salida, exist_ok=True)
    corridas = {}
    for a in sys.argv[3:]:
        etiq, dirs = a.split("=", 1)
        corridas[etiq] = corrida(etiq, dirs.split(","))
    escribir(os.path.join(salida, "predicciones_replicate.csv"), [p for c in corridas.values() for p in c["preds"]])
    escribir(os.path.join(salida, "ciclos_ia.csv"), [x for c in corridas.values() for x in c["ciclos"]])
    escribir(os.path.join(salida, "llamadas_ia_trazas.csv"), [t for c in corridas.values() for t in c["trazas"]])
    escribir(os.path.join(salida, "resumen_replicate.csv"), [resumen(e, c) for e, c in corridas.items()])
    # Reproducibilidad (dos primeras corridas)
    et = list(corridas)
    if len(et) >= 2:
        a = {(x["id"], x["ciclo"]): x for x in corridas[et[0]]["ciclos"]}
        b = {(x["id"], x["ciclo"]): x for x in corridas[et[1]]["ciclos"]}
        filas = []
        for k in sorted(set(a) | set(b)):
            x, y = a.get(k), b.get(k)
            filas.append({"id": k[0], "ciclo": k[1],
                          f"decision_{et[0]}": x["decision"] if x else "(sin orden de IA)",
                          f"decision_{et[1]}": y["decision"] if y else "(sin orden de IA)",
                          f"intervencion_{et[0]}": x["intervenciones"] if x else "-",
                          f"intervencion_{et[1]}": y["intervenciones"] if y else "-",
                          f"clasificacion_{et[0]}": x["clasificacion"] if x else "-",
                          f"clasificacion_{et[1]}": y["clasificacion"] if y else "-",
                          f"pertinencia_{et[0]}": x["pertinencia"] if x else "-",
                          f"pertinencia_{et[1]}": y["pertinencia"] if y else "-",
                          f"cambio_{et[0]}": x["cambios"] if x else "-", f"cambio_{et[1]}": y["cambios"] if y else "-",
                          "misma_decision": bool(x and y and x["decision"] == y["decision"] and
                                                 x["intervenciones"] == y["intervenciones"]),
                          "misma_clasificacion": bool(x and y and x["clasificacion"] == y["clasificacion"]),
                          "misma_pertinencia": bool(x and y and [p.split(":")[-1] for p in x["pertinencia"].split(";")]
                                                    == [p.split(":")[-1] for p in y["pertinencia"].split(";")]),
                          "mismo_cambio_exacto": bool(x and y and x["cambios"] == y["cambios"])})
        escribir(os.path.join(salida, "reproducibilidad_R_Rp.csv"), filas)
    # Comparacion con la IA simulada (repeticion v1.1)
    sim = {(r["id"], int(r["ciclo"])): r for r in leer(os.path.join(sim_ora, "adaptaciones.csv"))}
    sim_sem = {(r["id"], int(r["semana"])): r for r in leer(os.path.join(sim_ora, "semanas.csv"))}
    real = corridas[et[0]]
    filas = []
    claves = sorted({(x["id"], x["ciclo"]) for x in real["ciclos"]} |
                    {(k[0], k[1]) for k, r in sim.items() if r["intervencion"] in REQUIERE_IA})
    reales = {(x["id"], x["ciclo"]): x for x in real["ciclos"]}
    for k in claves:
        s, rr, ss = sim.get(k), reales.get(k), sim_sem.get(k)
        filas.append({"id": k[0], "ciclo": k[1],
                      "decision_simulada": ss["decision_backend"] if ss else "-",
                      "intervencion_simulada": s["intervencion"] if s else "-",
                      "cambio_simulado": (f"sus {s['sustituciones']} red {s['reducciones']} ret {s['retirados']} "
                                          f"trabajo {s['trabajo_antes_s']}->{s['trabajo_despues_s']}") if s else "-",
                      "pertinencia_simulada": s["pertinencia"] if s else "-",
                      "decision_real": rr["decision"] if rr else "(sin orden de IA en R_O)",
                      "intervencion_real": rr["intervenciones"] if rr else "-",
                      "clasificacion_real": rr["clasificacion"] if rr else "-",
                      "cambio_real": rr["cambios"] if rr else "-",
                      "pertinencia_real": rr["pertinencia"] if rr else "-"})
    escribir(os.path.join(salida, "comparacion_simulada_vs_real.csv"), filas)
    for r in [resumen(e, c) for e, c in corridas.items()]:
        print(r)


if __name__ == "__main__":
    main()
