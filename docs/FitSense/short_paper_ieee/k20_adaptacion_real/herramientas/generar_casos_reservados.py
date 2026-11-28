#!/usr/bin/env python3
"""Casos RESERVADOS H01-H16 para la evaluacion final K20 (no se usan para ajustar el prompt).

Regla fija, decidida antes de ejecutar nada sobre ellos (i = 0..15, g = i % 4 objetivo, k = i // 4 causa):
  objetivo   = GOALS[g]                      (GAIN_MUSCLE, INCREASE_STRENGTH, LOSE_WEIGHT, GENERAL_FITNESS)
  causa      = CAUSAS[k]                     (fatiga, demasiado dificil, desmotivacion: localizadas; dificil+desmotivacion: ampliada)
  nivel      = NIVELES[(g + 2k) % 3]
  ubicacion  = UBICACIONES[(g + k) % 3]      (gimnasio completo, casa con mancuernas y bandas, casa sin material)
  dias       = DIAS[(g + k) % 2]             (3: L-X-V, 4: L-M-J-V)
  minutos    = MINUTOS[(g + 3k) % 4]         (30, 45, 60, 75)
Secuencia de la semana medida (el resto hecho): 3 dias -> H/H/O (localizada, Aobs 66,7) o H/O/O (ampliada, 33,3);
4 dias -> H/H/H/O (75) o H/O/O/O (25). Datos personales: los del perfil E1 historico del mismo objetivo.
Uso: python3 -I generar_casos_reservados.py <03_PERFILES_U01_U20.csv> <salida.csv>
"""
import csv
import sys

GOALS = ["GAIN_MUSCLE", "INCREASE_STRENGTH", "LOSE_WEIGHT", "GENERAL_FITNESS"]
NIVELES = ["BEGINNER", "INTERMEDIATE", "ADVANCED"]
DIAS = {3: "1|3|5 (lunes, miercoles, viernes)", 4: "1|2|4|5 (lunes, martes, jueves, viernes)"}
MINUTOS = [30, 45, 60, 75]
CAUSAS = [("F", "FATIGUE"), ("D", "TOO_DIFFICULT"), ("M", "LACK_OF_MOTIVATION"), ("DM", None)]


def main():
    historicos = list(csv.DictReader(open(sys.argv[1], newline="", encoding="utf-8")))
    cab = list(historicos[0].keys()) + ["motivo_omision_s4", "regla"]
    gym = next(r for r in historicos if r["ubicacion"] == "GYM")["equipamiento"]
    ubicaciones = [("GYM", gym), ("HOME", "body weight|dumbbell|band"), ("HOME", "body weight")]
    filas = []
    for i in range(16):
        g, k = i % 4, i // 4
        base = next(r for r in historicos if r["goal_type"] == GOALS[g] and r["patron"] == "E1")
        dias = [3, 4][(g + k) % 2]
        etiqueta, motivo = CAUSAS[k]
        r = dict(base)
        r.update({"id": f"H{i + 1:02d}", "patron": "R-" + etiqueta, "nivel": NIVELES[(g + 2 * k) % 3],
                  "ubicacion": ubicaciones[(g + k) % 3][0], "equipamiento": ubicaciones[(g + k) % 3][1],
                  "dias_por_semana": str(dias), "dias_disponibles": DIAS[dias],
                  "minutos_sesion": str(MINUTOS[(g + 3 * k) % 4]),
                  "motivo_omision_s2": "", "motivo_omision_s3": "", "motivo_omision_s4": "",
                  "semilla": "userId x 1000003 + epochDay(2026-11-02)",
                  "regla": f"i={i} g={g} k={k}"})
        if motivo:  # localizada: la ultima sesion omitida con el motivo
            r["secuencia"] = "H/H/O" if dias == 3 else "H/H/H/O"
            r[f"motivo_omision_s{dias}"] = motivo
        else:       # ampliada: demasiado dificil y desmotivacion (y otra vez dificil con 4 dias)
            r["secuencia"] = "H/O/O" if dias == 3 else "H/O/O/O"
            r["motivo_omision_s2"], r["motivo_omision_s3"] = "TOO_DIFFICULT", "LACK_OF_MOTIVATION"
            if dias == 4:
                r["motivo_omision_s4"] = "TOO_DIFFICULT"
        r["objetivo"] = r["goal_type"]
        filas.append(r)
    with open(sys.argv[2], "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=cab, lineterminator="\n")
        w.writeheader()
        w.writerows(filas)


if __name__ == "__main__":
    main()
