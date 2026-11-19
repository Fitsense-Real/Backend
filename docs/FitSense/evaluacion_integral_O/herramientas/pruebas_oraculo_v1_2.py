#!/usr/bin/env python3
"""Pruebas del oraculo integral O v1.2 (IA real) antes de cualquier corrida pagada.

1. IDENTIDAD: en todas las corridas existentes (modos SIMULADA y DETERMINISTA) v1.2 produce exactamente lo mismo que
   v1.1 (matriz, detalle, semanas, adaptaciones, pasos y huella).
2. REGRESION DE LAS PRUEBAS NEGATIVAS: las 19 pruebas de pruebas_negativas_oraculo_v1_1.py, ejecutadas con v1.2 en el
   lugar de v1.1, siguen pasando (v1.2 rechaza lo mismo).
3. R-1 (solo modo REPLICATE), por mutacion de copias de la corrida S_O:
   R1-C  control: la corrida declarada REPLICATE, con sus intentos tal cual, da APROBADO en A10 con v1.2.
   R1-V  caso real tipico: el intento 1 de la IA es valido y publicable (O03, ciclo 1) -> APROBADO con v1.2, y FALLO
         con v1.1 (por eso v1.1 no sirve para la IA real: exigia un intento 1 invalido a proposito).
   R1-N  negativa: los intentos del ciclo publicado desde la IA se marcan todos NO publicables -> FALLO A10 con v1.2.
Nunca modifica las carpetas de entrada.

Uso: python3 -I pruebas_oraculo_v1_2.py <O_PERFILES.csv> <salida_dir> <G> <P> [<G2> <P2> ...]
     (pares de carpetas de corridas existentes; el primer par se usa para las mutaciones)
"""
import csv
import filecmp
import json
import os
import shutil
import subprocess
import sys
import tempfile

csv.field_size_limit(sys.maxsize)
AQUI = os.path.dirname(os.path.abspath(__file__))
V11 = os.path.join(AQUI, "oraculo_integral_O_v1_1.py")
V12 = os.path.join(AQUI, "oraculo_integral_O_v1_2.py")
PN = os.path.join(AQUI, "pruebas_negativas_oraculo_v1_1.py")


def correr(oraculo, perfiles, g, p, salida):
    os.makedirs(salida, exist_ok=True)
    cp = subprocess.run([sys.executable, "-I", oraculo, perfiles, salida, g, p], capture_output=True, text=True)
    if cp.returncode != 0:
        raise RuntimeError(cp.stderr[-2000:])
    with open(os.path.join(salida, "detalle.json"), encoding="utf-8") as f:
        return {d["id"]: d for d in json.load(f)}


def main():
    perfiles, salida = sys.argv[1], sys.argv[2]
    pares = list(zip(sys.argv[3::2], sys.argv[4::2]))
    os.makedirs(salida, exist_ok=True)
    filas, ok = [], True
    with tempfile.TemporaryDirectory() as tmp:
        # 1. identidad
        for i, (g, p) in enumerate(pares):
            a, b = os.path.join(tmp, f"id{i}_v11"), os.path.join(tmp, f"id{i}_v12")
            correr(V11, perfiles, g, p, a)
            correr(V12, perfiles, g, p, b)
            iguales = all(filecmp.cmp(os.path.join(a, f), os.path.join(b, f), shallow=False) for f in
                          ("matriz_10_aspectos.csv", "detalle.json", "semanas.csv", "adaptaciones.csv",
                           "pasos_progresion.csv", "huella_normalizada.txt"))
            ok &= iguales
            filas.append({"prueba": f"IDENTIDAD {os.path.basename(g.rstrip('/'))[:-2]}", "esperado": "v1.2 == v1.1",
                          "obtenido": "identicos" if iguales else "DISTINTOS", "resultado": "PASA" if iguales else "NO PASA"})
        # 2. pruebas negativas de v1.1 con v1.2
        g, p = pares[0]
        env_out = os.path.join(tmp, "pn")
        script = (f"import sys,importlib.util;spec=importlib.util.spec_from_file_location('pn',{PN!r});"
                  f"m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);m.V11={V12!r};"
                  f"sys.argv=['pn',{perfiles!r},{g!r},{p!r},{env_out!r}];m.main()")
        cp = subprocess.run([sys.executable, "-I", "-c", script], capture_output=True, text=True)
        pasa = cp.returncode == 0 and "TODAS PASAN" in cp.stdout
        ok &= pasa
        n = sum(1 for l in cp.stdout.splitlines() if l.startswith("PASA"))
        filas.append({"prueba": "PRUEBAS NEGATIVAS v1.1 ejecutadas con v1.2", "esperado": "19/19 PASA",
                      "obtenido": f"{n}/19 PASA", "resultado": "PASA" if pasa else "NO PASA"})
        if os.path.exists(os.path.join(env_out, "pruebas_negativas_v1_1.csv")):
            shutil.copy(os.path.join(env_out, "pruebas_negativas_v1_1.csv"),
                        os.path.join(salida, "pruebas_negativas_con_v1_2.csv"))
        # 3. R-1
        for cod in ("R1-C", "R1-V", "R1-N"):
            mg, mp = os.path.join(tmp, cod, "G"), os.path.join(tmp, cod, "P")
            shutil.copytree(g, mg)
            shutil.copytree(p, mp)
            ruta = os.path.join(mg, "00_configuracion.txt")
            txt = open(ruta, encoding="utf-8").read()
            assert "modo: SIMULADA" in txt
            open(ruta, "w", encoding="utf-8").write(txt.replace("modo: SIMULADA", "modo: REPLICATE"))
            if cod in ("R1-N", "R1-V"):
                ri = os.path.join(mg, "intentos_ia.csv")
                with open(ri, newline="", encoding="utf-8") as f:
                    r = csv.DictReader(f)
                    cab, ints = r.fieldnames, list(r)
                for x in ints:
                    if x["etiqueta"] == "O03/ciclo1":
                        x["publicable"] = "false" if cod == "R1-N" else ("true" if x["intento"] == "1" else x["publicable"])
                with open(ri, "w", newline="", encoding="utf-8") as f:
                    w = csv.DictWriter(f, fieldnames=cab, lineterminator="\n")
                    w.writeheader()
                    w.writerows(ints)
            r12 = correr(V12, perfiles, mg, mp, os.path.join(tmp, cod, "v12"))
            r11 = correr(V11, perfiles, mg, mp, os.path.join(tmp, cod, "v11"))
            e12, e11 = r12["O03"]["estados"]["A10_seguridad_integridad"], r11["O03"]["estados"]["A10_seguridad_integridad"]
            mot = " | ".join(r12["O03"]["checks"]["A10_seguridad_integridad"]) or "-"
            esp = "FALLO" if cod == "R1-N" else "APROBADO"
            bien = e12 == esp and (cod != "R1-N" or "sin ningun intento juzgado publicable" in mot) \
                and (cod != "R1-V" or e11 == "FALLO")
            ok &= bien
            filas.append({"prueba": cod + {"R1-C": " control: corrida REPLICATE con sus intentos tal cual",
                                           "R1-V": " intento 1 real valido y publicable (v1.1 lo daria FALLO)",
                                           "R1-N": " negativa: adaptacion de la IA publicada sin intento publicable"}[cod],
                          "esperado": f"O03 A10 {esp} (v1.2)" + (" y FALLO (v1.1)" if cod == "R1-V" else ""), "obtenido": f"v1.2 {e12} [{mot}] · v1.1 {e11}",
                          "resultado": "PASA" if bien else "NO PASA"})
    with open(os.path.join(salida, "pruebas_oraculo_v1_2.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(filas[0]), lineterminator="\n")
        w.writeheader()
        w.writerows(filas)
    for x in filas:
        print(f"{x['resultado']:8} {x['prueba'][:70]:70} {x['obtenido'][:110]}")
    print("TODAS PASAN" if ok else "HAY PRUEBAS QUE NO PASAN")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
