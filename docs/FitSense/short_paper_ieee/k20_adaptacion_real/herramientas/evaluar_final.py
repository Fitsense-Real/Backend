#!/usr/bin/env python3
"""Evaluacion definitiva K20 (R y R') contra los criterios APROBADOS de 07 §3 / 08 §5, sin modificarlos.

Lee las cuatro carpetas ya pasadas por el oraculo v2 (R_U, Rp_U, R_J, Rp_J) y reutiliza el analisis de
comparar_dev_r.py. Escribe METRICAS_FINALES.csv, CRITERIOS_FINALES.csv, DIFERENCIAS_R_RP.csv y SESIONES_FINALES.csv.
Uso: python3 -I evaluar_final.py <R_U> <Rp_U> <R_J> <Rp_J> <salida_dir>
"""
import csv
import importlib.util
import json
import math
import os
import sys

_spec = importlib.util.spec_from_file_location(
    "comparar_dev_r", os.path.join(os.path.dirname(os.path.abspath(__file__)), "comparar_dev_r.py"))
cdr = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(cdr)


def wilson_inferior(exitos, n, z=1.959964):
    if n == 0:
        return None
    p = exitos / n
    den = 1 + z * z / n
    centro = p + z * z / (2 * n)
    margen = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n))
    return (centro - margen) / den


def main():
    r_u, rp_u, r_j, rp_j, out = sys.argv[1:6]
    os.makedirs(out, exist_ok=True)
    corridas = {"R_U": r_u, "Rp_U": rp_u, "R_J": r_j, "Rp_J": rp_j}
    met, ses = {}, {}
    for k, d in corridas.items():
        met[k], ses[k] = cdr.analizar(d)
        # Clasificacion descriptiva (no cambia ningun criterio): un perfil SIN PLAN INICIAL ("0 planes vigentes en S1")
        # es un fallo de generacion y cuenta en A-1; no es un incumplimiento de seguridad (no publica ningun plan).
        det = json.load(open(os.path.join(d, "oraculo", "detalle.json"), encoding="utf-8"))
        gen = [(x["id"], e, i) for x in det for e, l in x["comprobaciones"].items() for i in l
               if e == "A_generacion" and i.startswith("0 planes vigentes")]
        seg = [(x["id"], e, i) for x in det for e, l in x["comprobaciones"].items() for i in l
               if not (e == "A_generacion" and i.startswith("0 planes vigentes"))]
        met[k]["fallos_generacion_sin_plan"] = len(gen)
        met[k]["fallos_generacion_ids"] = " ".join(sorted({g[0] for g in gen}))
        met[k]["incumplimientos_seguridad"] = len(seg)
        met[k]["incumplimientos_seguridad_detalle"] = "; ".join(f"{a}/{b}: {c}" for a, b, c in seg)

    with open(os.path.join(out, "METRICAS_FINALES.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["metrica"] + list(corridas))
        for m in met["R_U"]:
            w.writerow([m] + [json.dumps(met[k][m], ensure_ascii=False) if isinstance(met[k][m], dict) else met[k][m]
                              for k in corridas])

    crit = []

    def add(cid, cohorte, texto, valor, cumple):
        crit.append([cid, cohorte, texto, valor, "CUMPLE" if cumple else "NO CUMPLE"])

    for coh, a, b, total in (("U01-U20", "R_U", "Rp_U", 20), ("J01-J16", "R_J", "Rp_J", 16)):
        for k in (a, b):
            add("A-1", coh, f"{k}: recorridos PASS = {total}/{total}",
                f'{met[k]["recorridos_PASS"]}/{met[k]["recorridos_total"]}', met[k]["recorridos_PASS"] == total)
            add("A-2/C", coh, f"{k}: 0 incumplimientos de seguridad (oraculo v2; los fallos de generacion sin plan"
                f" cuentan en A-1: {met[k]['fallos_generacion_ids'] or 'ninguno'})", met[k]["incumplimientos_seguridad"],
                met[k]["incumplimientos_seguridad"] == 0)
            add("B-4", coh, f"{k}: 0 sesiones de desmotivacion con mas de 2 ejercicios cambiados",
                met[k]["desmotivacion_mas_de_2"], met[k]["desmotivacion_mas_de_2"] == 0)
        e = [met[k]["efectivas_pertinentes_IA_en_factibles"] for k in (a, b)]
        n = [met[k]["sesiones_factibles"] for k in (a, b)]
        acc = [met[k]["cat_ACC_origen_IA"] for k in (a, b)]
        if coh == "U01-U20":
            add("B-1", coh, "R + R': efectivas y pertinentes >= 18/20 sobre las factibles",
                f"{sum(e)}/{sum(n)}", sum(e) >= 18)
            for k, ei, ni in zip((a, b), e, n):
                add("B-1", coh, f"{k}: ninguna corrida por debajo de 9/10", f"{ei}/{ni}", ei >= 9)
        else:
            frac = sum(e) / sum(n) if sum(n) else 0
            wl = wilson_inferior(sum(e), sum(n))
            add("B-2", coh, "R + R': efectivas y pertinentes >= 80 % de las factibles",
                f"{sum(e)}/{sum(n)} = {frac:.1%}", frac >= 0.80)
            add("B-2", coh, "R + R': limite inferior de Wilson al 95 % >= 63 %",
                "n/d" if wl is None else f"{wl:.1%}", wl is not None and wl >= 0.63)
            for k, ei, ni in zip((a, b), e, n):
                add("B-2", coh, f"{k}: ninguna corrida por debajo del 75 % de sus factibles",
                    f"{ei}/{ni} = {ei / ni:.1%}" if ni else "0/0", ni > 0 and ei / ni >= 0.75)
        add("B-3", coh, "R + R': proporcion de ACC entre las efectivas (informativa, sin umbral)",
            f"{sum(acc)}/{sum(e)}" if sum(e) else "0/0", True)

    with open(os.path.join(out, "CRITERIOS_FINALES.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["criterio", "cohorte", "comprobacion", "valor", "resultado"])
        w.writerows(crit)

    with open(os.path.join(out, "DIFERENCIAS_R_RP.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["cohorte", "id", "sesion", "intervencion", "factible_R", "factible_Rp", "R_origen", "R_categoria",
                    "Rp_origen", "Rp_categoria", "R_cambios", "Rp_cambios", "igual_categoria", "igual_cambio"])
        for coh, a, b in (("U", "R_U", "Rp_U"), ("J", "R_J", "Rp_J")):
            idx = {(r["id"], r["sesion"]): r for r in ses[b]}
            claves = list(dict.fromkeys([(r["id"], r["sesion"]) for r in ses[a]] + list(idx)))
            ia = {(r["id"], r["sesion"]): r for r in ses[a]}
            for c in claves:
                x, y = ia.get(c, {}), idx.get(c, {})
                cx = (x.get("sustituciones", ""), x.get("reducciones", ""))
                cy = (y.get("sustituciones", ""), y.get("reducciones", ""))
                w.writerow([coh, c[0], c[1], x.get("intervencion") or y.get("intervencion"), x.get("factible_sonda"),
                            y.get("factible_sonda"), x.get("origen"), x.get("categoria"), y.get("origen"),
                            y.get("categoria"), json.dumps(cx, ensure_ascii=False), json.dumps(cy, ensure_ascii=False),
                            x.get("categoria") == y.get("categoria"), cx == cy])

    with open(os.path.join(out, "SESIONES_FINALES.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        cab = None
        for k in corridas:
            for r in ses[k]:
                if cab is None:
                    cab = ["corrida"] + list(r)
                    w.writerow(cab)
                w.writerow([k] + [r[c] for c in cab[1:]])

    for c in crit:
        print(" | ".join(str(x) for x in c))


if __name__ == "__main__":
    main()
