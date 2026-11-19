#!/usr/bin/env python3
"""Cohorte INDEPENDIENTE K01-K16 (auditoria post R/R', 05/11/2026). Regla FIJADA Y PUBLICADA ANTES de ejecutar nada
sobre K. Sus resultados no se usan para cambiar el sistema ni el prompt: se informan como resultado.

Regla (i = 0..15, g = i % 4 objetivo, k = i // 4 causa), distinta de la de H y de la de J en nivel, ubicacion, dias
y minutos:
  objetivo   = GOALS[g]                        (los cuatro, igual que H y J)
  causa      = CAUSAS[k]                       (fatiga, demasiado dificil, desmotivacion; dificil+desmotivacion)
  nivel      = NIVELES[(2g + k) % 3]           (H: (g + 2k) % 3; J: (g + k + 1) % 3)
  ubicacion  = UBICACIONES[(g + 2k + 2) % 3]   (H: (g + k) % 3; J: (2g + k + 1) % 3)
  dias       = [3, 4][(g + 2k + 1) % 2]        (H: (g + k) % 2; J: (g + k + 1) % 2)
  minutos    = MINUTOS_K[(3g + k) % 4]
MINUTOS_K = [33, 44, 56, 67]: un valor por cuartil del rango de P-01 (30-75) que no usa ninguna cohorte anterior
(U: 45, 60; H: 30, 45, 60, 75; J: 36, 47, 59, 70). Con la rotacion, cada objetivo tiene los cuatro valores.
Secuencias, motivos, esfuerzo y datos personales: los mismos convenios que J (perfil E1 historico del objetivo).
La regla NO se ajusto tras ver ningun resultado de K (no hay ninguno: se publica antes de ejecutar).
Uso: python3 -I generar_cohorte_K.py <03_PERFILES_U01_U20.csv> <J_PERFILES_INDEPENDIENTES.csv> <salida.csv>
"""
import csv
import sys

GOALS = ["GAIN_MUSCLE", "INCREASE_STRENGTH", "LOSE_WEIGHT", "GENERAL_FITNESS"]
NIVELES = ["BEGINNER", "INTERMEDIATE", "ADVANCED"]
DIAS = {3: "1|3|5 (lunes, miercoles, viernes)", 4: "1|2|4|5 (lunes, martes, jueves, viernes)"}
MINUTOS_K = [33, 44, 56, 67]
CAUSAS = [("F", "FATIGUE"), ("D", "TOO_DIFFICULT"), ("M", "LACK_OF_MOTIVATION"), ("DM", None)]


def main():
    historicos = list(csv.DictReader(open(sys.argv[1], newline="", encoding="utf-8")))
    cab = list(csv.DictReader(open(sys.argv[2], newline="", encoding="utf-8")).fieldnames)
    gym = next(r for r in historicos if r["ubicacion"] == "GYM")["equipamiento"]
    ubicaciones = [("GYM", gym), ("HOME", "body weight|dumbbell|band"), ("HOME", "body weight")]
    filas = []
    for i in range(16):
        g, k = i % 4, i // 4
        base = next(r for r in historicos if r["goal_type"] == GOALS[g] and r["patron"] == "E1")
        dias = [3, 4][(g + 2 * k + 1) % 2]
        etiqueta, motivo = CAUSAS[k]
        u = ubicaciones[(g + 2 * k + 2) % 3]
        r = {c: base.get(c, "") for c in cab}
        r.update({"id": f"K{i + 1:02d}", "objetivo": GOALS[g], "goal_type": GOALS[g], "patron": "K-" + etiqueta,
                  "nivel": NIVELES[(2 * g + k) % 3], "ubicacion": u[0], "equipamiento": u[1],
                  "dias_por_semana": str(dias), "dias_disponibles": DIAS[dias],
                  "minutos_sesion": str(MINUTOS_K[(3 * g + k) % 4]),
                  "motivo_omision_s2": "", "motivo_omision_s3": "", "motivo_omision_s4": "",
                  "semilla": "userId x 1000003 + epochDay(2026-11-02)", "regla": f"i={i} g={g} k={k}"})
        # Mismo convenio de esfuerzo que J (texto del perfil E1 del objetivo en J).
        if motivo:
            r["secuencia"] = "H/H/O" if dias == 3 else "H/H/H/O"
            r[f"motivo_omision_s{dias}"] = motivo
        else:
            r["secuencia"] = "H/O/O" if dias == 3 else "H/O/O/O"
            r["motivo_omision_s2"], r["motivo_omision_s3"] = "TOO_DIFFICULT", "LACK_OF_MOTIVATION"
            if dias == 4:
                r["motivo_omision_s4"] = "TOO_DIFFICULT"
        filas.append(r)
    with open(sys.argv[3], "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=cab, lineterminator="\n")
        w.writeheader()
        w.writerows(filas)


if __name__ == "__main__":
    main()
