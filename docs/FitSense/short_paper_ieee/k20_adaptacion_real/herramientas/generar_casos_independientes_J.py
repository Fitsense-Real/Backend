#!/usr/bin/env python3
"""Cohorte INDEPENDIENTE J01-J16 para la evaluacion final K20 (sustituye a H01-H16, que quedaron expuestos; A1 §7).

NO SE EJECUTA NADA SOBRE J (ni D, ni D', ni la IA simulada, ni Replicate) hasta que la version final este congelada.
Sus resultados no se usan para cambiar el sistema; lo que aparezca se informa como resultado.

Regla fija (i = 0..15, g = i % 4 objetivo, k = i // 4 causa), distinta de la de H en cada eje:
  objetivo   = GOALS[g]                    (igual que H: GAIN_MUSCLE, INCREASE_STRENGTH, LOSE_WEIGHT, GENERAL_FITNESS)
  causa      = CAUSAS[k]                   (igual que H: fatiga, demasiado dificil, desmotivacion; dificil+desmotivacion)
  nivel      = NIVELES[(g + k + 1) % 3]    (H: (g + 2k) % 3)
  ubicacion  = UBICACIONES[(2g + k + 1) % 3]   (H: (g + k) % 3)
  dias       = DIAS[(g + k + 1) % 2]       (H: (g + k) % 2; con el mismo (g, k), J tiene siempre el otro numero de dias)
  minutos    = MINUTOS_J[(g + k + 1) % 4]  (H: (30, 45, 60, 75)[(g + 3k) % 4])
MINUTOS_J son los puntos medios, redondeados hacia arriba, de los cuatro cuartiles del rango que admite P-01 (30-75,
46 valores): [30-41] -> 36, [42-52] -> 47, [53-64] -> 59, [65-75] -> 70. Con la rotacion, cada objetivo tiene los
cuatro valores, asi que todos tienen un caso de 61-75 (70) y ninguno repite un minuto de H.
La regla se fijo el 04/11/2026, DESPUES de conocer el hallazgo 53-59 de GAIN_MUSCLE (C-8), y NO se ajusto para
evitarlo: J05 (GAIN_MUSCLE, 59 min) cae en esa franja.
Secuencias y motivos: los de H (3 dias -> H/H/O o H/O/O; 4 dias -> H/H/H/O o H/O/O/O). Datos personales: los del
perfil E1 historico del mismo objetivo, como en H.
Uso: python3 -I generar_casos_independientes_J.py <03_PERFILES_U01_U20.csv> <salida.csv>
"""
import csv
import sys

GOALS = ["GAIN_MUSCLE", "INCREASE_STRENGTH", "LOSE_WEIGHT", "GENERAL_FITNESS"]
NIVELES = ["BEGINNER", "INTERMEDIATE", "ADVANCED"]
DIAS = {3: "1|3|5 (lunes, miercoles, viernes)", 4: "1|2|4|5 (lunes, martes, jueves, viernes)"}
MINUTOS_J = [36, 47, 59, 70]
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
        dias = [3, 4][(g + k + 1) % 2]
        etiqueta, motivo = CAUSAS[k]
        u = ubicaciones[(2 * g + k + 1) % 3]
        r = dict(base)
        r.update({"id": f"J{i + 1:02d}", "patron": "J-" + etiqueta, "nivel": NIVELES[(g + k + 1) % 3],
                  "ubicacion": u[0], "equipamiento": u[1],
                  "dias_por_semana": str(dias), "dias_disponibles": DIAS[dias],
                  "minutos_sesion": str(MINUTOS_J[(g + k + 1) % 4]),
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
