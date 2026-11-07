#!/usr/bin/env python3
"""Tablas del articulo para la evaluacion con Replicate REAL (SOLO LECTURA: presenta, no evalua).

Uso:
  python3 -I tablas_replicate_O.py <analisis_dir> <entregables_dir> <oraculo_sim_dir> \
      <R_oraculo_v1_2> <R_oraculo_v1_3> <Rp_oraculo_v1_2> <Rp_oraculo_v1_3> <R_G> <R_P> <Rp_G> <Rp_P>
Escribe en <entregables_dir>: Tabla6..Tabla10 (CSV), TABLAS_REPLICATE.md y EVALUACION_INTEGRAL_O_REPLICATE_REAL.xlsx
(libro de R_O de tablas_O.py + hojas de R'_O y de Replicate).
"""
import csv
import os
import sys
from collections import Counter

csv.field_size_limit(sys.maxsize)
ASP = ["A1_generacion", "A2_personalizacion_restricciones", "A3_registro_ejecucion", "A4_adherencia",
       "A5_adaptacion_ia", "A6_mantenimiento", "A7_progresion", "A8_reincorporacion", "A9_continuidad",
       "A10_seguridad_integridad"]


def leer(r):
    with open(r, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def escribir(r, filas):
    with open(r, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(filas[0]), lineterminator="\n")
        w.writeheader()
        w.writerows(filas)


def md(filas):
    c = list(filas[0])
    return "\n".join(["| " + " | ".join(c) + " |", "|" + "---|" * len(c)] +
                     ["| " + " | ".join(str(f[k]) for k in c) + " |" for f in filas])


def totales(ora):
    m = leer(os.path.join(ora, "matriz_10_aspectos.csv"))
    c = Counter(r[a] for r in m for a in ASP)
    ap, fa = c["APROBADO"], c["FALLO"]
    return {"APROBADO": ap, "FALLO": fa, "NO APLICA": c["NO APLICA"], "REQUIERE REVISIÓN": c["REQUIERE REVISION"],
            "Aprobación técnica": f"{ap}/{ap + fa} = {100 * ap / (ap + fa):.1f} %"}


def sin_efecto_evitable(dirs, etiqueta):
    for d in dirs:
        for r in leer(os.path.join(d, "factibilidad.csv")) if os.path.exists(os.path.join(d, "factibilidad.csv")) else []:
            if r["etiqueta"] == etiqueta and int(r["validas"] or 0) > 0:
                return f"sí ({r['validas']} modificación(es) válida(s): {r['validas_detalle'][:80]})"
    return "no (ninguna modificación válida)"


def main():
    an, out, sim, r12, r13, rp12, rp13, rg, rp_, rpg, rpp = sys.argv[1:12]
    os.makedirs(out, exist_ok=True)
    res = {r["corrida"]: r for r in leer(os.path.join(an, "resumen_replicate.csv"))}
    ciclos = leer(os.path.join(an, "ciclos_ia.csv"))
    rep = leer(os.path.join(an, "reproducibilidad_R_Rp.csv"))
    dirs = {"R_O": [rg, rp_], "Rp_O": [rpg, rpp]}
    nombre = {"R_O": "R_O", "Rp_O": "R′_O"}
    # Tabla 6: aceptacion de Replicate
    t6 = []
    for k, r in res.items():
        n = int(r["ciclos_con_orden_de_IA"])
        se = [c for c in ciclos if c["corrida"] == k and c["clasificacion"] == "SIN_EFECTO_PROPUESTO_POR_IA"]
        t6.append({"Corrida": nombre[k], "Ciclos con orden de IA": n, "Predicciones": r["predicciones"],
                   "Tokens entrada / salida": f"{int(r['tokens_entrada']):,} / {int(r['tokens_salida']):,}".replace(",", "."),
                   "USD medido": r["usd_medido"].replace(".", ","),
                   "Latencia mediana / máx. (s)": f"{r['latencia_mediana_s']} / {r['latencia_max_s']}".replace(".", ","),
                   "Aceptadas en el intento 1": r["aceptadas_intento_1"],
                   "Aceptadas tras reintento": r["aceptadas_tras_reintento"],
                   "Intentos rechazados por los validadores": r["intentos_rechazados"],
                   "SIN_EFECTO propuesto por la IA": r["sin_efecto_propuesto_por_ia"] +
                   (f" ({', '.join(c['id'] for c in se)}; evitable: {', '.join(sin_efecto_evitable(dirs[k], c['id'] + '/ciclo' + c['ciclo']).split(' (')[0] for c in se)})" if se else ""),
                   "Respaldo determinista": f"{int(r['respaldo_tras_rechazos']) + int(r['respaldo_tras_error'])}"
                   + (f" (tras error del proveedor: {r['respaldo_tras_error']})" if int(r['respaldo_tras_error']) else ""),
                   "Errores del proveedor": r["errores_proveedor"],
                   "Adaptación de la IA publicada": f"{int(r['aceptadas_intento_1']) + int(r['aceptadas_tras_reintento'])}/{n}"})
    escribir(os.path.join(out, "Tabla6_aceptacion_replicate.csv"), t6)
    # Tabla 7: pertinencia por sesion adaptada
    t7 = []
    for k in res:
        per = Counter(p.split(":")[-1] for c in ciclos if c["corrida"] == k for p in c["pertinencia"].split(";") if p != "-")
        n = sum(per.values())
        t7.append({"Corrida": nombre[k], "Sesiones con orden de adaptar": n, "ACC (pertinente)": per["ACC"],
                   "ALT (alternativa válida)": per["ALT"], "CONS-J (conservación justificada por la política)": per["CONS-J"],
                   "INC (inadecuada)": per["INC"]})
    escribir(os.path.join(out, "Tabla7_pertinencia.csv"), t7)
    # Tabla 8: reproducibilidad
    n = len(rep)
    t8 = [{"Comparación R_O frente a R′_O (ciclos con orden de IA)": n,
           "Misma decisión e intervención": f"{sum(r['misma_decision'] == 'True' for r in rep)}/{n}",
           "Misma clasificación del resultado": f"{sum(r['misma_clasificacion'] == 'True' for r in rep)}/{n}",
           "Misma pertinencia": f"{sum(r['misma_pertinencia'] == 'True' for r in rep)}/{n}",
           "Mismo cambio exacto": f"{sum(r['mismo_cambio_exacto'] == 'True' for r in rep)}/{n}"}]
    escribir(os.path.join(out, "Tabla8_reproducibilidad.csv"), t8)
    # Tabla 9: diez aspectos, simulada frente a real
    t9 = []
    for etiq, ora in (("IA simulada · repetición S_O · oráculo v1.1 (9ae4c49)", sim),
                      ("Replicate real · R_O · oráculo v1.2 (congelado)", r12), ("Replicate real · R_O · oráculo v1.3", r13),
                      ("Replicate real · R′_O · oráculo v1.2 (congelado)", rp12), ("Replicate real · R′_O · oráculo v1.3", rp13)):
        t9.append({"Evaluación": etiq, **totales(ora)})
    escribir(os.path.join(out, "Tabla9_diez_aspectos_simulada_vs_real.csv"), t9)
    # Tabla 10: por aspecto, R_O y R'_O (v1.3)
    t10 = []
    ms = {e: leer(os.path.join(o, "matriz_10_aspectos.csv")) for e, o in (("sim", sim), ("R", r13), ("Rp", rp13))}
    for a in ASP:
        fila = {"Aspecto": a}
        for e, et in (("sim", "IA simulada"), ("R", "R_O"), ("Rp", "R′_O")):
            c = Counter(r[a] for r in ms[e])
            fila[f"{et} AP/FA/NA/RR"] = f"{c['APROBADO']}/{c['FALLO']}/{c['NO APLICA']}/{c['REQUIERE REVISION']}"
        t10.append(fila)
    escribir(os.path.join(out, "Tabla10_aspectos_por_corrida.csv"), t10)
    with open(os.path.join(out, "TABLAS_REPLICATE.md"), "w", encoding="utf-8") as f:
        f.write("# Tablas de la evaluación con Replicate REAL (R_O y R′_O)\n\n")
        f.write("**Origen:**\n")
        f.write("- Respuestas reales del modelo; adherencia simulada por guion.\n")
        f.write("- Valores obtenidos de `analisis/` y de los oráculos.\n")
        f.write("- Las tablas 1–5 de cada corrida están en `R_O/TABLAS_SHORT_PAPER.md` y `Rp_O/TABLAS_SHORT_PAPER.md`.\n\n")
        for t, filas in (("Tabla 6. Aceptación de las propuestas de Replicate", t6),
                         ("Tabla 7. Pertinencia de las sesiones adaptadas (oráculo v1.3)", t7),
                         ("Tabla 8. Reproducibilidad R_O frente a R′_O", t8),
                         ("Tabla 9. Diez aspectos: IA simulada frente a Replicate real", t9),
                         ("Tabla 10. Estados por aspecto (AP/FA/NA/RR)", t10)):
            f.write(f"## {t}\n\n{md(filas)}\n\n")
    # Excel: libro de R_O + hojas de R'_O y Replicate
    from openpyxl import load_workbook
    from openpyxl.styles import Font
    wb = load_workbook(os.path.join(out, "R_O", "EVALUACION_INTEGRAL_O_20_PERFILES.xlsx"))
    for ws in wb.worksheets:
        ws.title = ("R_O " + ws.title)[:31]
    wbp = load_workbook(os.path.join(out, "Rp_O", "EVALUACION_INTEGRAL_O_20_PERFILES.xlsx"))
    for ws in wbp.worksheets:
        if ws.title in ("Tabla3_adherencia", "Tabla4_adaptacion", "Tabla5_aspectos", "Tabla5b_totales", "Progresion",
                        "Adaptaciones"):
            n = wb.create_sheet(("Rp_O " + ws.title)[:31])
            for row in ws.iter_rows(values_only=True):
                n.append(row)
    for titulo, ruta in (("Rep Resumen", os.path.join(an, "resumen_replicate.csv")),
                         ("Rep Ciclos IA", os.path.join(an, "ciclos_ia.csv")),
                         ("Rep Predicciones", os.path.join(an, "predicciones_replicate.csv")),
                         ("Rep Reproducibilidad", os.path.join(an, "reproducibilidad_R_Rp.csv")),
                         ("Rep Sim vs real", os.path.join(an, "comparacion_simulada_vs_real.csv")),
                         ("T6 Aceptacion", os.path.join(out, "Tabla6_aceptacion_replicate.csv")),
                         ("T7 Pertinencia", os.path.join(out, "Tabla7_pertinencia.csv")),
                         ("T8 Reproducibilidad", os.path.join(out, "Tabla8_reproducibilidad.csv")),
                         ("T9 Sim vs real", os.path.join(out, "Tabla9_diez_aspectos_simulada_vs_real.csv")),
                         ("T10 Aspectos", os.path.join(out, "Tabla10_aspectos_por_corrida.csv"))):
        filas = leer(ruta)
        ws = wb.create_sheet(titulo[:31])
        if filas:
            ws.append(list(filas[0]))
            for c in ws[1]:
                c.font = Font(bold=True)
            for r in filas:
                ws.append([r[k] for k in filas[0]])
    wb.save(os.path.join(out, "EVALUACION_INTEGRAL_O_REPLICATE_REAL.xlsx"))
    for t in (t6, t7, t8, t9):
        print(md(t), "\n")


if __name__ == "__main__":
    main()
