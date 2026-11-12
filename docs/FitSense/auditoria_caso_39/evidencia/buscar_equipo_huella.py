"""Caso 39 · busca los subconjuntos de equipo que reproducen la huella real del conjunto elegible.

Huella del log real (SlotCoverageGate, 15:31:35.720):
    350 elegibles · LOWER_KNEE=27 LOWER_HIP=18 UPPER_PUSH=62 UPPER_PULL=33 fuera_de_slot=210

Entrada: elegibles_todo_el_equipo.csv (volcado de Caso39ReproduccionIT con TODO el equipo del catalogo: 396 elegibles).
Cada ejercicio es elegible si cada grupo de requisitos tiene al menos un equipo seleccionado (misma regla que
ExerciseRepository.findEligible) y, para el 1374, si hay barra de dominadas (INC-CAT-1374).

Uso: python3 -I -B buscar_equipo_huella.py elegibles_todo_el_equipo.csv
"""
import collections
import csv
import itertools
import sys

EQUIPO = ("body weight, dumbbell, band, kettlebell, stability ball, medicine ball, rope, barbell, ez barbell, "
          "olympic barbell, cable, leverage machine, smith machine, sled machine, weighted, assisted, other, bench, "
          "pull-up bar, suspension straps, parallel bars, gymnastic rings, bosu ball, balance board, ab wheel, "
          "step platform, plyo box, stationary bike, treadmill, landmine, captains chair, wall bars, anchor point, "
          "ghd bench, hyperextension bench, squat rack, preacher bench, vertical pole").split(", ")
HUELLA = collections.Counter({"T": 350, "LOWER_KNEE": 27, "LOWER_HIP": 18, "UPPER_PUSH": 62, "UPPER_PULL": 33,
                              "F": 210})
SLOTS = ("LOWER_KNEE", "LOWER_HIP", "UPPER_PUSH", "UPPER_PULL")


def requisitos(fila):
    grupos = collections.defaultdict(set)
    for parte in fila["requisitos"].split("|"):
        if parte:
            n, codigo = parte.split(":", 1)
            grupos[n].add(codigo)
    return list(grupos.values())


def recuento(filas, equipo):
    c = collections.Counter()
    for fila, grupos in filas:
        if not all(g & equipo for g in grupos):
            continue
        if int(fila["exercise_id"]) == 1374 and "pull-up bar" not in equipo:
            continue
        c[fila["slot"] if fila["slot"] in SLOTS else "F"] += 1
        c["T"] += 1
    return c


def main(ruta):
    filas = [(f, requisitos(f)) for f in csv.DictReader(open(ruta, encoding="utf-8"), delimiter=";")]
    completo = recuento(filas, set(EQUIPO))
    # Solo importan los equipos cuya retirada cambia algo
    influyentes = [e for e in EQUIPO if recuento(filas, set(EQUIPO) - {e}) != completo]
    soluciones = []
    for k in range(0, 7):
        for quitar in itertools.combinations(influyentes, k):
            if recuento(filas, set(EQUIPO) - set(quitar)) == HUELLA:
                soluciones.append(quitar)
    print("con todo el equipo:", dict(completo))
    print("equipos influyentes:", influyentes)
    print(f"{len(soluciones)} subconjuntos reproducen la huella real:")
    for s in soluciones:
        nota = "  (descartado: la IA uso el 1325, cinta, y el validador lo acepto como elegible)" \
            if "treadmill" in s else ""
        print("  sin", list(s), nota)


if __name__ == "__main__":
    main(sys.argv[1])
