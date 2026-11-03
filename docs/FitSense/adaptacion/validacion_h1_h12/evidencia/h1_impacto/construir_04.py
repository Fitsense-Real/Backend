"""H1: construye 04_IMPACTO_H1_SUSTITUCIONES_HISTORICAS.csv (una fila por sustitucion historica: 57 + 66 = 123) con el
resultado de su sesion en salida_sesiones.csv (V8C real, conjunto frente a multiconjunto). Solo lectura.
Uso: python3 -I construir_04.py <raiz del repo>"""
import csv, os, sys
from collections import Counter

RAIZ = sys.argv[1]
EV2 = os.path.join(RAIZ, "docs", "FitSense", "short_paper_ieee", "evaluacion_calidad_adaptacion_v2")
AQUI = os.path.dirname(os.path.abspath(__file__))
SAL = {}
for r in csv.DictReader(open(os.path.join(AQUI, "salida_sesiones.csv"))):
    SAL[(r["corrida"], int(r["semana"]), r["usuario"], r["sesion"])] = r
PERF = {r["id"]: r for r in csv.DictReader(open(os.path.join(EV2, "10_PERFILES_SINTETICOS_R01_R08.csv")))}

filas = []
for x in csv.DictReader(open(os.path.join(EV2, "02_AUDITORIA_SUSTITUCIONES.csv"))):
    filas.append(dict(origen="PR #28/#29 (02_AUDITORIA_SUSTITUCIONES.csv)", corrida=x["corrida"], semana=1,
                      usuario=x["usuario"], objetivo=x["objetivo"], sesion=x["sesion"], motivo=x["motivo"],
                      intervencion=x["intervencion"], original=x["id_original"], sustituto=x["id_sustituto"],
                      patron_original=x["patron_original"], patron_sustituto=x["patron_nuevo"],
                      musculo_original=x["musculo_original"], musculo_sustituto=x["musculo_nuevo"],
                      dificultad_original=x["dificultad_original"], dificultad_sustituto=x["dificultad_nueva"]))
for x in csv.DictReader(open(os.path.join(EV2, "evidencia", "auditoria_sustituciones_RC.csv"))):
    if x["tipo"] != "SUSTITUCION":
        continue
    filas.append(dict(origen="PR #30 (auditoria_sustituciones_RC.csv)", corrida=x["corrida"], semana=int(x["semana"]),
                      usuario=x["usuario"], objetivo=PERF[x["usuario"]]["goal_type"], sesion=x["sesion"],
                      motivo=x["motivo"], intervencion=x["intervencion"], original=x["original"], sustituto=x["nuevo"],
                      patron_original=x["patron_o"], patron_sustituto=x["patron_n"], musculo_original=x["musculo_o"],
                      musculo_sustituto=x["musculo_n"], dificultad_original=x["dif_o"], dificultad_sustituto=x["dif_n"]))
for f in filas:
    s = SAL[(f["corrida"], f["semana"], f["usuario"], f["sesion"])]
    f["sesion_retirados"] = s["retirados"]
    f["sesion_anadidos"] = s["anadidos"]
    f["v8c_conjunto"] = "aceptada" if s["aceptada_conjunto"] == "true" else "rechazada"
    f["v8c_multiconjunto"] = "aceptada" if s["aceptada_multiconjunto"] == "true" else "rechazada"
    f["cambia_resultado"] = "si" if s["cambia"] == "true" else "no"
    f["necesita_excepcion"] = "no"
    f["nota"] = ("mismo patron; la sesion conserva su distribucion de patrones"
                 if f["patron_original"] == f["patron_sustituto"] else "OTRO PATRON (revisar)")
out = os.path.join(RAIZ, "docs", "FitSense", "adaptacion", "validacion_h1_h12", "04_IMPACTO_H1_SUSTITUCIONES_HISTORICAS.csv")
with open(out, "w", newline="", encoding="utf-8") as fh:
    w = csv.DictWriter(fh, fieldnames=list(filas[0]))
    w.writeheader()
    w.writerows(filas)
print(len(filas), Counter(f["origen"][:7] for f in filas))
print(Counter((f["objetivo"], f["v8c_conjunto"], f["v8c_multiconjunto"]) for f in filas))
print(Counter(f["nota"] for f in filas))
