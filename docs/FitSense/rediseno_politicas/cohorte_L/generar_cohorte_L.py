#!/usr/bin/env python3
"""Cohorte INDEPENDIENTE L01-L16 (rediseno de politicas tras la auditoria R/R', 05/11/2026).

Regla FIJADA Y PUBLICADA (commit) ANTES de ejecutar nada sobre L y DESPUES de cerrar el codigo de PLAN-REV-2.0.
Sus resultados no se usan para cambiar el sistema, el prompt, las politicas ni los oraculos: se informan como
resultado, fallos incluidos.

Regla (i = 0..15, g = i % 4 objetivo, k = i // 4 causa), distinta de las de H, J y K en nivel, ubicacion, dias y
minutos:
  objetivo   = GOALS[g]                        (los cuatro, igual que H, J y K)
  causa      = CAUSAS[k]                       (fatiga, demasiado dificil, desmotivacion; dificil+desmotivacion)
  nivel      = NIVELES[(2g + 2k + 1) % 3]      (H: (g + 2k); J: (g + k + 1); K: (2g + k); todos % 3)
  ubicacion  = UBICACIONES[(2g + 2k) % 3]      (H: (g + k); J: (2g + k + 1); K: (g + 2k + 2); todos % 3)
  dias       = [2, 3, 4, 5][(g + 3k) % 4]      (H, J y K solo usan 3 y 4: L anade 2 y 5, donde vive P-G3)
  minutos    = MINUTOS_L[(g + 3k + 1) % 4]
MINUTOS_L = [31, 42, 53, 64]: un valor por cuartil de P-01 (30-75) que no usa ninguna cohorte anterior
(U: 45, 60; H: 30, 45, 60, 75; J: 36, 47, 59, 70; K: 33, 44, 56, 67).
Secuencias: H en todas las sesiones menos la ultima, que se omite con el motivo de la causa (con 2 dias: H/O). La
causa combinada (DM): H y despues omisiones alternas TOO_DIFFICULT / LACK_OF_MOTIVATION. Esfuerzo y datos personales:
los mismos convenios que J y K (perfil E1 historico del objetivo). Semilla del motor: la de siempre
(userId x 1.000.003 + epochDay(2026-11-02)).
Uso: python3 -I generar_cohorte_L.py <03_PERFILES_U01_U20.csv> <J_PERFILES_INDEPENDIENTES.csv> <salida.csv>
"""
import csv
import sys

GOALS = ["GAIN_MUSCLE", "INCREASE_STRENGTH", "LOSE_WEIGHT", "GENERAL_FITNESS"]
NIVELES = ["BEGINNER", "INTERMEDIATE", "ADVANCED"]
DIAS = {2: "2|4 (martes, jueves)", 3: "1|3|5 (lunes, miercoles, viernes)",
        4: "1|2|4|5 (lunes, martes, jueves, viernes)", 5: "1|2|3|4|5 (lunes a viernes)"}
MINUTOS_L = [31, 42, 53, 64]
CAUSAS = [("F", "FATIGUE"), ("D", "TOO_DIFFICULT"), ("M", "LACK_OF_MOTIVATION"), ("DM", None)]


def main():
    historicos = list(csv.DictReader(open(sys.argv[1], newline="", encoding="utf-8")))
    cab = list(csv.DictReader(open(sys.argv[2], newline="", encoding="utf-8")).fieldnames)
    if "motivo_omision_s5" not in cab:
        cab.insert(cab.index("motivo_omision_s4") + 1, "motivo_omision_s5")
    gym = next(r for r in historicos if r["ubicacion"] == "GYM")["equipamiento"]
    ubicaciones = [("GYM", gym), ("HOME", "body weight|dumbbell|band"), ("HOME", "body weight")]
    filas = []
    for i in range(16):
        g, k = i % 4, i // 4
        base = next(r for r in historicos if r["goal_type"] == GOALS[g] and r["patron"] == "E1")
        dias = [2, 3, 4, 5][(g + 3 * k) % 4]
        etiqueta, motivo = CAUSAS[k]
        u = ubicaciones[(2 * g + 2 * k) % 3]
        r = {c: base.get(c, "") for c in cab}
        r.update({"id": f"L{i + 1:02d}", "objetivo": GOALS[g], "goal_type": GOALS[g], "patron": "L-" + etiqueta,
                  "nivel": NIVELES[(2 * g + 2 * k + 1) % 3], "ubicacion": u[0], "equipamiento": u[1],
                  "dias_por_semana": str(dias), "dias_disponibles": DIAS[dias],
                  "minutos_sesion": str(MINUTOS_L[(g + 3 * k + 1) % 4]),
                  "semilla": "userId x 1000003 + epochDay(2026-11-02)", "regla": f"i={i} g={g} k={k}"})
        for s in range(2, 6):
            r[f"motivo_omision_s{s}"] = ""
        if motivo:
            r["secuencia"] = "/".join(["H"] * (dias - 1) + ["O"])
            r[f"motivo_omision_s{dias}"] = motivo
        else:
            r["secuencia"] = "/".join(["H"] + ["O"] * (dias - 1))
            for s in range(2, dias + 1):
                r[f"motivo_omision_s{s}"] = "TOO_DIFFICULT" if s % 2 == 0 else "LACK_OF_MOTIVATION"
        filas.append(r)
    with open(sys.argv[3], "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=cab, lineterminator="\n")
        w.writeheader()
        w.writerows(filas)


if __name__ == "__main__":
    main()
