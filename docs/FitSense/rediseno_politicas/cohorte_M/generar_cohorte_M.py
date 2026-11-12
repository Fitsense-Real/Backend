#!/usr/bin/env python3
"""Cohorte INDEPENDIENTE M01-M30 (cierre de PLAN-REV-2.0). Regla FIJADA Y PUBLICADA (commit) ANTES de ejecutar nada
sobre M y DESPUES de cerrar el codigo. Sus resultados no se usan para cambiar el sistema, las politicas, el prompt ni
los oraculos: se informan como resultado, fallos incluidos.

Rejilla principal M01-M28 (i = 0..27, g = i % 4 objetivo, d = i // 4 + 1 dias pedidos 1..7):
  objetivo   = GOALS[g]                                  (los cuatro)
  dias       = d (1..7, disponibilidad = los d primeros dias de DIAS_DISP[d]; dias pedidos = disponibles)
  nivel      = NIVELES[(g + d) % 3]
  ubicacion  = UBICACIONES[(g + 2d) % 3]
  minutos    = MINUTOS_M[(g + 3d) % 7]   MINUTOS_M = [30, 31, 38, 46, 57, 66, 75] (fronteras de P-01 y valores que no
               usa ninguna cohorte anterior: U 45/60; H 30/45/60/75; J 36/47/59/70; K 33/44/56/67; L 31/42/53/64)
  causa      = CAUSAS[i % 5]  F fatiga, D demasiado dificil, M desmotivacion, DM dificil+desmotivacion, P parcial (P40)
  SESIONES PROGRAMADAS n = min(dias, tope del nivel) (S-MAX: principiante 4, resto 6). La secuencia se escribe sobre
  las n sesiones PROGRAMADAS (no sobre los dias pedidos: la leccion de L05):
     F/D/M: H ... H, O(causa) en la ultima;  DM: H y despues O alternas TOO_DIFFICULT/LACK_OF_MOTIVATION;
     P: H ... H, P40 en la ultima;  con n = 1: F/D/M/DM -> O(causa) ; P -> P40.
Casos frontera M29-M30 (fijados aqui, no derivados de resultados):
  M29 INCREASE_STRENGTH INTERMEDIATE GYM 1 dia de 30 min, causa F: resultado ESPERADO con REV2 = sin plan
      (FUERZA_SIN_TIEMPO); con HIST, plan.
  M30 GAIN_MUSCLE BEGINNER HOME peso corporal, 7 dias pedidos y disponibles, 45 min, causa M: 4 sesiones programadas.
Esfuerzo y datos personales: los convenios de J y K (perfil E1 historico del objetivo). Semilla del motor: la de
siempre (userId x 1.000.003 + epochDay(2026-11-02)).
Uso: python3 -I generar_cohorte_M.py <03_PERFILES_U01_U20.csv> <J_PERFILES_INDEPENDIENTES.csv> <salida.csv>
"""
import csv
import sys

GOALS = ["GAIN_MUSCLE", "INCREASE_STRENGTH", "LOSE_WEIGHT", "GENERAL_FITNESS"]
NIVELES = ["BEGINNER", "INTERMEDIATE", "ADVANCED"]
NOMBRES = {1: "lunes", 2: "martes", 3: "miercoles", 4: "jueves", 5: "viernes", 6: "sabado", 7: "domingo"}
DIAS_DISP = {1: [1], 2: [2, 4], 3: [1, 3, 5], 4: [1, 2, 4, 5], 5: [1, 2, 3, 4, 5], 6: [1, 2, 3, 4, 5, 6],
             7: [1, 2, 3, 4, 5, 6, 7]}
MINUTOS_M = [30, 31, 38, 46, 57, 66, 75]
CAUSAS = [("F", "FATIGUE"), ("D", "TOO_DIFFICULT"), ("M", "LACK_OF_MOTIVATION"), ("DM", None), ("P", None)]
TOPE = {"BEGINNER": 4}


def dias_txt(d):
    ds = DIAS_DISP[d]
    return "|".join(str(x) for x in ds) + " (" + ", ".join(NOMBRES[x] for x in ds) + ")"


def secuencia(etiqueta, motivo, n):
    motivos = {}
    if etiqueta == "P":
        pasos = ["H"] * (n - 1) + ["P40"]
    elif etiqueta == "DM":
        pasos = ["O"] if n == 1 else ["H"] + ["O"] * (n - 1)
        for s in range(1, n + 1):
            if pasos[s - 1] == "O":
                motivos[s] = "TOO_DIFFICULT" if s % 2 == 0 or n == 1 else "LACK_OF_MOTIVATION"
    else:
        pasos = ["H"] * (n - 1) + ["O"]
        motivos[n] = motivo
    return "/".join(pasos), motivos


def main():
    historicos = list(csv.DictReader(open(sys.argv[1], newline="", encoding="utf-8")))
    cab = list(csv.DictReader(open(sys.argv[2], newline="", encoding="utf-8")).fieldnames)
    if "motivo_omision_s1" not in cab:
        cab.insert(cab.index("motivo_omision_s2"), "motivo_omision_s1")
    for s in (5, 6):
        if f"motivo_omision_s{s}" not in cab:
            cab.insert(cab.index(f"motivo_omision_s{s - 1}") + 1, f"motivo_omision_s{s}")
    if "esperado" not in cab:
        cab.append("esperado")
    gym = next(r for r in historicos if r["ubicacion"] == "GYM")["equipamiento"]
    ubicaciones = [("GYM", gym), ("HOME", "body weight|dumbbell|band"), ("HOME", "body weight")]
    casos = []
    for i in range(28):
        g, d = i % 4, i // 4 + 1
        casos.append((GOALS[g], NIVELES[(g + d) % 3], ubicaciones[(g + 2 * d) % 3], d, MINUTOS_M[(g + 3 * d) % 7],
                      CAUSAS[i % 5], f"i={i} g={g} d={d}", ""))
    casos.append(("INCREASE_STRENGTH", "INTERMEDIATE", ubicaciones[0], 1, 30, CAUSAS[0], "frontera M29",
                  "REV2: sin plan (FUERZA_SIN_TIEMPO); HIST: plan"))
    casos.append(("GAIN_MUSCLE", "BEGINNER", ubicaciones[2], 7, 45, CAUSAS[2], "frontera M30",
                  "4 sesiones programadas (S-MAX)"))
    filas = []
    for k, (obj, nivel, u, d, minutos, (etiqueta, motivo), regla, esperado) in enumerate(casos):
        base = next(r for r in historicos if r["goal_type"] == obj and r["patron"] == "E1")
        r = {c: base.get(c, "") for c in cab}
        n = min(d, TOPE.get(nivel, 6))
        sec, motivos = secuencia(etiqueta, motivo, n)
        r.update({"id": f"M{k + 1:02d}", "objetivo": obj, "goal_type": obj, "patron": "M-" + etiqueta,
                  "nivel": nivel, "ubicacion": u[0], "equipamiento": u[1], "dias_por_semana": str(d),
                  "dias_disponibles": dias_txt(d), "minutos_sesion": str(minutos), "secuencia": sec,
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
