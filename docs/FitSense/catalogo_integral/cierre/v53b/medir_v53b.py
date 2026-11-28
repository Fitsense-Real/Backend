"""V53-B · compara la medicion opt-in de la rejilla de 168 (datos sinteticos) y escribe los TSV de esta carpeta.

Entrada: la salida de MedicionPermisoApoyoSuspendido{Apagado,OpcionA,OpcionB}IT (-Dfitsense.v53b.medicion=true, por
defecto en target/v53b). Cada clase mide los 168 perfiles con el permiso propio sin responder (null), confirmado que si
(true) y confirmado que no (false), con los MISMOS ids de usuario en cada pasada (misma semilla del motor de reglas): dos
filas solo difieren por el modo y el permiso.

La referencia de todas las comparaciones es APAGADO con el permiso sin responder (el comportamiento de hoy).

Uso: python3 -I -B medir_v53b.py <dir_medicion> <dir_salida>
"""
import csv
import os
import sys

APOYO = {457, 458, 459, 462, 777, 920, 934, 1069, 1153, 1184, 1303, 1304, 1306, 1317, 1335}
MODOS = ("APAGADO", "OPCION_A", "OPCION_B")
PERMISOS = ("null", "true", "false")
ORDEN = ["TODAS", "LIBRE", "SIN_COLGARSE", "SIN_CABEZA", "SIN_SUELO", "SIN_RODILLAS", "SIN_IMPACTO", "SIN_TODO"]


def ids(texto):
    return set(int(x) for x in texto.split(",") if x)


def leer(ruta):
    filas = {}
    with open(ruta, encoding="utf-8") as f:
        for r in csv.DictReader(f, delimiter="\t"):
            filas[(r["perfil"], r["permiso"])] = {
                "elegibles": ids(r["elegibles_ids"]),
                "desenlace": r["desenlace"],
                "en_plan": ids(r["apoyo_en_plan"]),
                "plan": r["plan"],
            }
    return filas


def lista(conjunto):
    return ",".join(str(i) for i in sorted(conjunto))


def escribir(ruta, filas):
    with open(ruta, "w", encoding="utf-8", newline="") as f:
        csv.writer(f, delimiter="\t", lineterminator="\n").writerows(filas)


def main():
    entrada, salida = sys.argv[1], sys.argv[2]
    datos = {m: leer(os.path.join(entrada, "medicion_%s.tsv" % m)) for m in MODOS}
    ref = {p: v for (p, permiso), v in datos["APAGADO"].items() if permiso == "null"}
    perfiles = sorted(ref)
    assert len(perfiles) == 168, len(perfiles)

    resumen = [["modo", "permiso", "restriccion", "perfiles", "con_plan", "perfiles_con_elegibles_distintos",
                "ejercicios_quitados", "ejercicios_anadidos", "cambios_fuera_de_los_15", "planes_distintos",
                "planes_con_alguno_de_los_15", "planes_con_alguno_de_los_15_hoy"]]
    detalle = [["modo", "permiso", "perfil", "elegibles_hoy", "elegibles", "quitados", "anadidos",
                "apoyo_elegible_hoy", "apoyo_elegible", "desenlace_hoy", "desenlace", "plan_identico",
                "apoyo_en_plan_hoy", "apoyo_en_plan"]]
    for modo in MODOS:
        for permiso in PERMISOS:
            acumulado = {}
            for p in perfiles:
                o, n = ref[p], datos[modo][(p, permiso)]
                quitados, anadidos = o["elegibles"] - n["elegibles"], n["elegibles"] - o["elegibles"]
                identico = o["plan"] == n["plan"] and o["desenlace"] == n["desenlace"]
                if quitados or anadidos or not identico:
                    detalle.append([modo, permiso, p, len(o["elegibles"]), len(n["elegibles"]), lista(quitados),
                                    lista(anadidos), lista(o["elegibles"] & APOYO), lista(n["elegibles"] & APOYO),
                                    o["desenlace"], n["desenlace"], "si" if identico else "no", lista(o["en_plan"]),
                                    lista(n["en_plan"])])
                for clave in (p.split("/")[-1], "TODAS"):
                    a = acumulado.setdefault(clave, {"perfiles": 0, "plan": 0, "cambian": 0, "quitados": set(),
                                                     "anadidos": set(), "fuera": set(), "distintos": 0,
                                                     "con15": 0, "con15_hoy": 0})
                    a["perfiles"] += 1
                    a["plan"] += n["desenlace"] == "PLAN"
                    a["cambian"] += 1 if quitados or anadidos else 0
                    a["quitados"] |= quitados
                    a["anadidos"] |= anadidos
                    a["fuera"] |= (quitados | anadidos) - APOYO
                    a["distintos"] += 0 if identico else 1
                    a["con15"] += 1 if n["en_plan"] else 0
                    a["con15_hoy"] += 1 if o["en_plan"] else 0
            for clave in ORDEN:
                a = acumulado[clave]
                resumen.append([modo, permiso, clave, a["perfiles"], a["plan"], a["cambian"], lista(a["quitados"]),
                                lista(a["anadidos"]), lista(a["fuera"]), a["distintos"], a["con15"], a["con15_hoy"]])

    # B sin responder frente a la opcion A con los mismos perfiles: deberian coincidir en todo.
    contraste = [["comparacion", "perfiles", "elegibles_iguales", "planes_iguales"]]
    for nombre, (m1, p1), (m2, p2) in (
            ("B_sin_responder_vs_A", ("OPCION_B", "null"), ("OPCION_A", "null")),
            ("B_confirma_si_vs_apagado", ("OPCION_B", "true"), ("APAGADO", "null")),
            ("A_permiso_true_vs_A_null", ("OPCION_A", "true"), ("OPCION_A", "null")),
            ("A_permiso_false_vs_A_null", ("OPCION_A", "false"), ("OPCION_A", "null")),
            ("apagado_permiso_true_vs_null", ("APAGADO", "true"), ("APAGADO", "null")),
            ("apagado_permiso_false_vs_null", ("APAGADO", "false"), ("APAGADO", "null"))):
        e = sum(1 for p in perfiles if datos[m1][(p, p1)]["elegibles"] == datos[m2][(p, p2)]["elegibles"])
        q = sum(1 for p in perfiles if datos[m1][(p, p1)]["plan"] == datos[m2][(p, p2)]["plan"]
                and datos[m1][(p, p1)]["desenlace"] == datos[m2][(p, p2)]["desenlace"])
        contraste.append([nombre, len(perfiles), e, q])

    os.makedirs(salida, exist_ok=True)
    escribir(os.path.join(salida, "medicion_v53b_resumen.tsv"), resumen)
    escribir(os.path.join(salida, "medicion_v53b_perfiles_con_cambios.tsv"), detalle)
    escribir(os.path.join(salida, "medicion_v53b_contraste.tsv"), contraste)
    for fila in resumen + contraste:
        print("\t".join(str(x) for x in fila))


if __name__ == "__main__":
    main()
