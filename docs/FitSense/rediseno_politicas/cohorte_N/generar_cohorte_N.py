#!/usr/bin/env python3
"""Cohorte INDEPENDIENTE N01-N20 para la evaluacion con IA real (opcion A: U + N). Regla FIJADA Y PUBLICADA (commit)
DESPUES del ultimo cambio de codigo (src sin cambios desde cc76bd1) y ANTES de cualquier ejecucion sobre N. Sus
resultados no se usan para cambiar el sistema, las politicas, el prompt, los oraculos ni los criterios: se informan
tal cual, fallos incluidos.

Diferencias deliberadas con U, J, K, L y M (ninguna cohorte anterior las usa):
  * minutos MINUTOS_N = [34, 41, 49, 54, 62, 68, 72] (U 45/60; H 30/45/60/75; J 36/47/59/70; K 33/44/56/67;
    L 31/42/53/64; M 30/31/38/45/46/57/66/75);
  * dias DISPONIBLES > dias PEDIDOS en 2 de cada 3 perfiles (en todas las anteriores pedidos = disponibles) y
    conjuntos de dias que empiezan en dias distintos de la semana;
  * el suceso de la semana medida (omision o sesion parcial) no siempre en la ultima sesion.
Las causas son las mismas de U/J/K/L/M (fatiga, demasiado dificil, desmotivacion, dificil + desmotivacion, parcial
P40) para que U y N midan lo mismo. Dolor, falta de tiempo y cambio de horario quedan FUERA (no evaluados en K20).

Rejilla N01-N18 (i = 0..17):
  objetivo  g = i % 4                        GOALS[g]
  nivel     NIVELES[(i + i // 4) % 3]        (N01-N12 cubren las 12 combinaciones objetivo x nivel)
  ubicacion UBICACIONES[(i // 4 + 2 * g) % 3]  GYM | HOME mancuernas y bandas | HOME peso corporal
  pedidos   d = DIAS_N[i % 7]                DIAS_N = [2, 5, 3, 6, 4, 1, 7]
  disponib. a = min(7, d + i % 3); empiezan en s = (3 i) % 7 + 1 y se reparten: {(s - 1 + round(k 7 / a)) % 7 + 1}
  minutos   MINUTOS_N[(g + 2 i) % 7]
  causa     CAUSAS[i % 5]
  SESIONES PROGRAMADAS n = min(d, tope del nivel, a) (S-MAX: principiante 4, resto 6). La secuencia se escribe
  sobre las n sesiones programadas; el suceso va en la posicion p = (2 i) % n + 1:
     F/D/M: H salvo O(causa) en p;  P: H salvo P40 en p;
     DM: O TOO_DIFFICULT en p y O LACK_OF_MOTIVATION en p % n + 1 (con n = 1, solo O TOO_DIFFICULT).
Casos frontera N19-N20 (contrato de 11_FUERZA_1_DIA_30_MIN.md y FuerzaSinTiempoIT: minimo de 32 min para los 4
patrones de fuerza en 1 dia; no derivados de ninguna ejecucion sobre N):
  N19 INCREASE_STRENGTH ADVANCED HOME mancuernas y bandas, 1 dia de 31 min, causa F: ESPERADO con REV2 = sin plan
      (FUERZA_SIN_TIEMPO); con HIST, plan (1 sesion).
  N20 INCREASE_STRENGTH INTERMEDIATE GYM, 1 dia de 32 min, causa D: ESPERADO 1 sesion programada (plan).
Esfuerzo y datos personales: los del perfil E1 del mismo objetivo en U (convenio de J, K, L y M). Semilla del motor:
la de siempre (userId x 1.000.003 + epochDay(2026-11-02)).
Uso: python3 -I generar_cohorte_N.py <03_PERFILES_U01_U20.csv> <M_PERFILES_INDEPENDIENTES.csv> <salida.csv>
"""
import csv
import sys

GOALS = ["GAIN_MUSCLE", "INCREASE_STRENGTH", "LOSE_WEIGHT", "GENERAL_FITNESS"]
NIVELES = ["BEGINNER", "INTERMEDIATE", "ADVANCED"]
NOMBRES = {1: "lunes", 2: "martes", 3: "miercoles", 4: "jueves", 5: "viernes", 6: "sabado", 7: "domingo"}
DIAS_N = [2, 5, 3, 6, 4, 1, 7]
MINUTOS_N = [34, 41, 49, 54, 62, 68, 72]
CAUSAS = [("F", "FATIGUE"), ("D", "TOO_DIFFICULT"), ("M", "LACK_OF_MOTIVATION"), ("DM", None), ("P", None)]
TOPE = {"BEGINNER": 4}


def disponibles(i, d):
    a = min(7, d + i % 3)
    s = (3 * i) % 7 + 1
    return sorted({(s - 1 + round(k * 7 / a)) % 7 + 1 for k in range(a)})


def dias_txt(ds):
    return "|".join(str(x) for x in ds) + " (" + ", ".join(NOMBRES[x] for x in ds) + ")"


def secuencia(etiqueta, motivo, n, p):
    pasos, motivos = ["H"] * n, {}
    if etiqueta == "P":
        pasos[p - 1] = "P40"
    elif etiqueta == "DM":
        pasos[p - 1] = "O"; motivos[p] = "TOO_DIFFICULT"
        if n > 1:
            q = p % n + 1
            pasos[q - 1] = "O"; motivos[q] = "LACK_OF_MOTIVATION"
    else:
        pasos[p - 1] = "O"; motivos[p] = motivo
    return "/".join(pasos), motivos


def main():
    historicos = list(csv.DictReader(open(sys.argv[1], newline="", encoding="utf-8")))
    cab = list(csv.DictReader(open(sys.argv[2], newline="", encoding="utf-8")).fieldnames)
    gym = next(r for r in historicos if r["ubicacion"] == "GYM")["equipamiento"]
    ubicaciones = [("GYM", gym), ("HOME", "body weight|dumbbell|band"), ("HOME", "body weight")]
    casos = []
    for i in range(18):
        g = i % 4
        d = DIAS_N[i % 7]
        casos.append((GOALS[g], NIVELES[(i + i // 4) % 3], ubicaciones[(i // 4 + 2 * g) % 3], d, disponibles(i, d),
                      MINUTOS_N[(g + 2 * i) % 7], CAUSAS[i % 5], i, f"i={i} g={g} d={d}", ""))
    casos.append(("INCREASE_STRENGTH", "ADVANCED", ubicaciones[1], 1, [3], 31, CAUSAS[0], 0, "frontera N19",
                  "REV2: sin plan (FUERZA_SIN_TIEMPO); HIST: 1 sesiones programadas"))
    casos.append(("INCREASE_STRENGTH", "INTERMEDIATE", ubicaciones[0], 1, [6], 32, CAUSAS[1], 0, "frontera N20",
                  "1 sesiones programadas"))
    filas = []
    for k, (obj, nivel, u, d, disp, minutos, (etiqueta, motivo), i, regla, esperado) in enumerate(casos):
        base = next(r for r in historicos if r["goal_type"] == obj and r["patron"] == "E1")
        r = {c: base.get(c, "") for c in cab}
        n = min(d, TOPE.get(nivel, 6), len(disp))
        p = (2 * i) % n + 1
        sec, motivos = secuencia(etiqueta, motivo, n, p)
        r.update({"id": f"N{k + 1:02d}", "objetivo": obj, "goal_type": obj, "patron": "N-" + etiqueta,
                  "nivel": nivel, "ubicacion": u[0], "equipamiento": u[1], "dias_por_semana": str(d),
                  "dias_disponibles": dias_txt(disp), "minutos_sesion": str(minutos), "secuencia": sec,
                  "semilla": "userId x 1000003 + epochDay(2026-11-02)", "regla": regla,
                  "esperado": esperado or f"{n} sesiones programadas"})
        for s in range(1, 7):
            r[f"motivo_omision_s{s}"] = motivos.get(s, "")
        filas.append(r)
    with open(sys.argv[3], "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=cab, lineterminator="\n")
        w.writeheader()
        w.writerows(filas)


if __name__ == "__main__":
    main()
