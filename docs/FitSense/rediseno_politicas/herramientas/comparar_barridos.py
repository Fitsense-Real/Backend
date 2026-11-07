#!/usr/bin/env python3
"""Compara barridos de generacion inicial (BarridoGeneracionInicialIT) de varias politicas: generacion, causas,
metricas de calidad por objetivo y nivel, y diferencias PLAN A PLAN frente a la primera (referencia).
Uso: python3 -I comparar_barridos.py <salida_dir> <etiqueta>=<dir_barrido> [<etiqueta>=<dir_barrido> ...]
Cada dir_barrido contiene barrido_generacion.csv y planes.csv."""
import csv, os, statistics as st, sys, collections, json


def cargar(d):
    filas = list(csv.DictReader(open(os.path.join(d, "barrido_generacion.csv"), encoding="utf-8")))
    planes = collections.defaultdict(list)
    for r in csv.DictReader(open(os.path.join(d, "planes.csv"), encoding="utf-8")):
        planes[r["perfil"]].append((r["fecha"], r["enfoque"], r["ejercicio"], r["series"], r["reps"], r["segundos"],
                                    r["descanso"]))
    return filas, planes


def clave(x):
    return "|".join([x["objetivo"], x["nivel"], x["equipo"], x["dias"], x["minutos"]])


def num(x, k, f=float):
    try:
        return f(x[k])
    except (ValueError, KeyError):
        return None


def main():
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    corridas = [a.split("=", 1) for a in sys.argv[2:]]
    datos = {et: cargar(d) for et, d in corridas}
    ref = corridas[0][0]
    resumen = []
    for et, _ in corridas:
        filas, planes = datos[et]
        val = [x for x in filas if x["resultado"] == "VALIDO"]
        inval = [x for x in filas if x["resultado"] != "VALIDO"]
        fam = collections.Counter((x["objetivo"], x["nivel"], x["equipo"], x["codigos"]) for x in inval)
        rf, rp = datos[ref]
        refidx = {clave(x): x for x in rf}
        iguales = sum(1 for x in filas if x["resultado"] == "VALIDO" and refidx[clave(x)]["resultado"] == "VALIDO"
                      and sorted(planes[clave(x)]) == sorted(rp[clave(x)]))
        ambos_validos = sum(1 for x in filas if x["resultado"] == "VALIDO" and refidx[clave(x)]["resultado"] == "VALIDO")
        r = {"politica": et, "perfiles": len(filas), "validos": len(val), "sin_plan": len(inval),
             "familias_sin_plan": {" ".join(k): v for k, v in fam.most_common()},
             "validos_en_ambos": ambos_validos, "planes_identicos_a_ref": iguales,
             "rescatados_vs_ref": sum(1 for x in filas if x["resultado"] == "VALIDO" and refidx[clave(x)]["resultado"] != "VALIDO"),
             "perdidos_vs_ref": sum(1 for x in filas if x["resultado"] != "VALIDO" and refidx[clave(x)]["resultado"] == "VALIDO")}
        # Calidad por objetivo y nivel (solo validos)
        cal = {}
        for g in sorted({(x["objetivo"], x["nivel"]) for x in val}):
            xs = [x for x in val if (x["objetivo"], x["nivel"]) == g]
            def med(k):
                v = [num(x, k) for x in xs if num(x, k) is not None]
                return round(st.median(v), 1) if v else None
            def mx(k):
                v = [num(x, k) for x in xs if num(x, k) is not None]
                return max(v) if v else None
            cal[" ".join(g)] = {"n": len(xs), "pct_tiempo_mediana": med("pct_tiempo_medio"),
                                "seg_medio_mediana": med("seg_medio"), "ej_min_mediana": med("ej_min"),
                                "series_grupo_max_mediana": med("series_grupo_max"),
                                "series_grupo_max_maximo": mx("series_grupo_max"),
                                "aerobico_min_mediana": med("aerobico_trabajo_min"),
                                "grupos_bajo_objetivo_mediana": med("grupos_bajo_objetivo"),
                                "alto_impacto_total": sum(int(num(x, "alto_impacto", int) or 0) for x in xs),
                                "pull_sin_traccion_total": sum(int(num(x, "pull_sin_traccion", int) or 0) for x in xs)}
        r["calidad"] = cal
        r["series_grupo_max_sobre_20"] = sum(1 for x in val if (num(x, "series_grupo_max") or 0) > 20)
        r["series_grupo_max_sobre_24"] = sum(1 for x in val if (num(x, "series_grupo_max") or 0) > 24)
        r["pull_sin_traccion_planes"] = sum(1 for x in val if (num(x, "pull_sin_traccion") or 0) > 0)
        r["sesiones_con_pocos_ejercicios_(ej_min<3)"] = sum(1 for x in val if (num(x, "ej_min") or 0) < 3)
        r["planes_con_sesion_insuficiente"] = sum(1 for x in val if (num(x, "sesiones_insuficientes") or 0) > 0)
        r["planes_con_sesion_corta_razonable"] = sum(1 for x in val if (num(x, "sesiones_cortas_razonables") or 0) > 0)
        r["estado_dosis"] = dict(collections.Counter(x.get("estado_dosis", "") for x in val))
        r["dg3_en_invalidos"] = dict(collections.Counter(x.get("dg3", "") for x in inval))
        resumen.append(r)
    json.dump(resumen, open(os.path.join(out, "comparacion_barridos.json"), "w", encoding="utf-8"), ensure_ascii=False,
              indent=1)
    with open(os.path.join(out, "comparacion_barridos.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["politica", "objetivo_nivel", "n", "pct_tiempo_mediana", "seg_medio_mediana", "ej_min_mediana",
                    "series_grupo_max_mediana", "series_grupo_max_maximo", "aerobico_min_mediana",
                    "grupos_bajo_objetivo_mediana", "alto_impacto_total", "pull_sin_traccion_total"])
        for r in resumen:
            for g, c in r["calidad"].items():
                w.writerow([r["politica"], g] + [c[k] for k in ["n", "pct_tiempo_mediana", "seg_medio_mediana",
                                                                 "ej_min_mediana", "series_grupo_max_mediana",
                                                                 "series_grupo_max_maximo", "aerobico_min_mediana",
                                                                 "grupos_bajo_objetivo_mediana", "alto_impacto_total",
                                                                 "pull_sin_traccion_total"]])
    for r in resumen:
        print(r["politica"], "validos", r["validos"], "sin_plan", r["sin_plan"], "rescatados", r["rescatados_vs_ref"],
              "perdidos", r["perdidos_vs_ref"], "identicos_a_ref", r["planes_identicos_a_ref"], "/", r["validos_en_ambos"],
              ">20:", r["series_grupo_max_sobre_20"], ">24:", r["series_grupo_max_sobre_24"],
              "PULL_sin_traccion:", r["pull_sin_traccion_planes"], "ej_min<3:", r["sesiones_con_pocos_ejercicios_(ej_min<3)"],
              "con_sesion_insuficiente:", r["planes_con_sesion_insuficiente"], "con_corta_razonable:",
              r["planes_con_sesion_corta_razonable"], "dosis:", r["estado_dosis"], "dg3:", r["dg3_en_invalidos"])
        print("   familias sin plan:", list(r["familias_sin_plan"].items())[:8])


if __name__ == "__main__":
    main()
