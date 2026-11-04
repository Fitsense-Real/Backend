"""Genera la lista PROPUESTA de casos independientes I01-I16 con una regla fija (sin mirar resultados).

Rotacion por indice i = 0..15, con g = i % 4 (objetivo) y k = i // 4 (causa):
  objetivo        = OBJETIVOS[g]
  causa           = CAUSAS[k]                            (4 casos por causa, uno por objetivo)
  equipamiento    = UBICACIONES[(g + k) % 3]             (dentro de cada objetivo rotan los tres)
  nivel           = NIVELES[(g + 2 * k) % 3]             (rota dentro de cada objetivo y no coincide con el equipo)
  dias            = DIAS[(g + k) % 4]                    (cuatro opciones; no colineal con los minutos)
  minutos         = MINUTOS[(g + 3 * k) % 4]             (dentro de cada objetivo, los cuatro)
La semana 1 aplica la causa a S2 (el resto se hace entero); la semana 2 se hace entera (continuidad).
Uso: python3 -I generar_casos_independientes.py > ../02_CASOS_INDEPENDIENTES_PROPUESTOS.csv
"""
import csv, sys

OBJETIVOS = ["GAIN_MUSCLE", "INCREASE_STRENGTH", "LOSE_WEIGHT", "GENERAL_FITNESS"]
CAUSAS = [
    ("DOLOR_IDENTIFICADO", "S2: el ejercicio en la posicion 1 con PAIN_OR_DISCOMFORT y 0 hecho; el resto hecho"),
    ("FALTA_DE_TIEMPO", "S2 omitida con LACK_OF_TIME"),
    ("CAMBIO_DE_HORARIO", "S2 omitida con SCHEDULE_CHANGE"),
    ("DOLOR_DE_SESION", "S2 omitida con PAIN_OR_DISCOMFORT (sin identificar el ejercicio)"),
]
NIVELES = ["BEGINNER", "INTERMEDIATE", "ADVANCED"]
UBICACIONES = [
    ("GYM", "body weight|dumbbell|barbell|cable|leverage machine|bench|band|stationary bike|treadmill|kettlebell|ez barbell|smith machine"),
    ("HOME", "body weight|dumbbell|band"),
    ("HOME", "body weight"),
]
DIAS = [(3, "1|3|5"), (4, "1|2|4|5"), (5, "1|2|3|4|5"), (6, "1|2|3|4|5|6")]
MINUTOS = [30, 45, 60, 75]

w = csv.writer(sys.stdout)
w.writerow(["id", "goal_type", "nivel", "ubicacion", "equipamiento", "dias_por_semana", "dias_disponibles",
            "minutos_sesion", "causa", "semana1", "semana2", "regla"])
for i in range(16):
    g, k = i % 4, i // 4
    causa, desc = CAUSAS[k]
    ub, eq = UBICACIONES[(g + k) % 3]
    nd, dd = DIAS[(g + k) % 4]
    w.writerow([f"I{i + 1:02d}", OBJETIVOS[g], NIVELES[(g + 2 * k) % 3], ub, eq, nd, dd,
                MINUTOS[(g + 3 * k) % 4], causa, desc, "todas las sesiones hechas (continuidad)",
                f"i={i} g={g} k={k}: rotacion fija (ver cabecera del script)"])
