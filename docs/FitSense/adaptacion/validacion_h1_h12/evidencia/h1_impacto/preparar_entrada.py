"""H1: reune TODAS las sesiones cambiadas y publicadas de las corridas conservadas (solo lectura) en una entrada
para H1ImpactoHistoricoTest, que las valida con el ConservationValidator real con las dos reglas.

Uso: python3 -I preparar_entrada.py <raiz del repo>  ->  entrada_sesiones.csv (junto a este script)
Grupos: HISTORICA_U (PR #28 etapa C y PR #29 C1/C2: las 57 sustituciones), HISTORICA_R (PR #30 RC1_r1/RC2_r1, dos
semanas: las 66), LABORATORIO (guiones sin red de PR #30: GUION_COMBINADOS_2S y R_GUION_2S; no son propuestas reales).
"""
import csv, os, sys

RAIZ = sys.argv[1]
SP = os.path.join(RAIZ, "docs", "FitSense", "short_paper_ieee")
F20 = os.path.join(SP, "experimento_final_20_usuarios")
PV2 = os.path.join(SP, "experimento_prompt_v2", "evidencia")
EV2 = os.path.join(SP, "evaluacion_calidad_adaptacion_v2")
PERF = {r["id"]: r for r in csv.DictReader(open(os.path.join(F20, "03_PERFILES_U01_U20.csv")))}
PERF.update({r["id"]: r for r in csv.DictReader(open(os.path.join(EV2, "10_PERFILES_SINTETICOS_R01_R08.csv")))})
INTERV = {"TOO_DIFFICULT": "VARIANTE_MAS_SENCILLA_O_CARGA", "FATIGUE": "REVISAR_CARGA_Y_RECUPERACION",
          "LACK_OF_MOTIVATION": "INVESTIGAR_CAMBIO_MINIMO", "PAIN_OR_DISCOMFORT": "SEGURIDAD_ANTE_DOLOR",
          "LACK_OF_TIME": "ACORTAR_O_REORGANIZAR", "SCHEDULE_CHANGE": "REPROGRAMAR_DIAS"}

CORRIDAS = [("HISTORICA_U", "C (PR #28)", os.path.join(F20, "evidencia", "C"), (1,))]
CORRIDAS += [("HISTORICA_U", c, os.path.join(PV2, c), (1,)) for c in ("C1_r1", "C2_r1", "C2_r2", "C1_r2")]
CORRIDAS += [("HISTORICA_R", c, os.path.join(EV2, "evidencia", c), (1, 2)) for c in ("RC1_r1", "RC2_r1")]
CORRIDAS += [("LABORATORIO", c, os.path.join(EV2, "evidencia", c), (1, 2)) for c in ("GUION_COMBINADOS_2S",
                                                                                     "R_GUION_2S")]


def motivo(u, ses, semana):
    p = PERF[u]
    suf = "_semana2" if semana == 2 else ""
    m = p.get("motivo_omision_" + ses.lower() + suf) or p.get("motivo_omision_" + ses.lower()) or ""
    return m


filas = []
for grupo, corrida, carpeta, semanas in CORRIDAS:
    for semana in semanas:
        fn = os.path.join(carpeta, "plan_siguiente.csv" if semana == 1 else "plan_siguiente_semana2.csv")
        if not os.path.exists(fn):
            continue
        for x in csv.DictReader(open(fn)):
            if x["igual_a_la_medida"] == "true":
                continue
            u, ses = x["id"], x["sesion"]
            m = motivo(u, ses, semana)
            filas.append(dict(grupo=grupo, corrida=corrida, semana=semana, usuario=u, objetivo=PERF[u]["goal_type"],
                              nivel=PERF[u]["nivel"], ubicacion=PERF[u]["ubicacion"], sesion=ses, motivo=m,
                              intervencion=INTERV.get(m, "CAUSA_NO_REGISTRADA"),
                              antes=x["medida"].split("[", 1)[1].rstrip("]"),
                              despues=x["siguiente"].split("[", 1)[1].rstrip("]")))
out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "entrada_sesiones.csv")
with open(out, "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=list(filas[0]))
    w.writeheader()
    w.writerows(filas)
from collections import Counter
print(len(filas), Counter((r["grupo"], r["intervencion"]) for r in filas))
