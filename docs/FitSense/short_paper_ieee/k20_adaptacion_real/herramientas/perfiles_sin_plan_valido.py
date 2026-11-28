#!/usr/bin/env python3
"""Registra, SIN ALTERARLOS, los perfiles de la matriz de 960 (ComparacionPerfilesIT.perfiles960) sin plan valido.

Reconstruye los atributos de cada Rxxxx con el mismo orden que la prueba (equipamiento x dias x objetivo x nivel x
minutos; despues RESTRICCIONES y DOLOR_P02) y clasifica la causa por las reglas que fallan. No cambia ningun dato.
Uso: python3 -I perfiles_sin_plan_valido.py <planes.csv de la matriz> <salida.csv> [politica=PLAN-HIST-1]
"""
import csv
import sys

EQUIPOS = ["GIMNASIO", "CASA_MANCUERNAS_BANDAS", "CASA_PESO_CORPORAL"]
DIAS = [1, 2, 3, 4, 6, 7]
GOALS = ["LOSE_WEIGHT", "GAIN_MUSCLE", "INCREASE_STRENGTH", "GENERAL_FITNESS"]
NIVELES = ["BEGINNER", "INTERMEDIATE", "ADVANCED"]
MINUTOS = [30, 45, 60, 75]


def perfiles():
    n, out = 0, {}
    for e in EQUIPOS:
        for d in DIAS:
            for g in GOALS:
                for nv in NIVELES:
                    for m in MINUTOS:
                        n += 1
                        out[f"R{n:04d}"] = (g, nv, e, d, m, "BASE")
    for g in GOALS:
        for nv in NIVELES:
            for m in MINUTOS:
                n += 1
                out[f"R{n:04d}"] = (g, nv, "GIMNASIO", 3, m, "RESTRICCIONES")
                n += 1
                out[f"R{n:04d}"] = (g, nv, "GIMNASIO", 3, m, "DOLOR_P02")
    return out


def familia(g, nv, e, d, m, grupo, reglas):
    if g == "LOSE_WEIGHT" and d == 1 and "V15" in reglas:
        return "V15_LOSE_WEIGHT_1_DIA"
    if nv == "BEGINNER" and m == 75 and g in ("GENERAL_FITNESS", "LOSE_WEIGHT"):
        return "PRINCIPIANTE_75_MIN"
    if e == "CASA_PESO_CORPORAL" and d >= 6:
        return "PESO_CORPORAL_6_7_DIAS"
    return "OTRA"


def main():
    politica = sys.argv[3] if len(sys.argv) > 3 else "PLAN-HIST-1"
    attrs = perfiles()
    filas = [r for r in csv.DictReader(open(sys.argv[1], newline="", encoding="utf-8"))
             if r["politica"] == politica and r["resultado"] != "VALIDO"]
    with open(sys.argv[2], "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["perfil", "politica", "objetivo", "nivel", "equipamiento", "dias", "minutos", "grupo", "resultado",
                    "reglas_que_fallan", "sesiones", "seg_total", "familia"])
        for r in filas:
            g, nv, e, d, m, grupo = attrs[r["perfil"]]
            w.writerow([r["perfil"], politica, g, nv, e, d, m, grupo, r["resultado"], r["reglas"], r["sesiones"],
                        r["seg_total"], familia(g, nv, e, d, m, grupo, r["reglas"])])
    print(len(filas), "perfiles sin plan valido con", politica)


if __name__ == "__main__":
    main()
