#!/usr/bin/env python3
"""Pruebas del oraculo integral O v1.4 (R-2: origen real de la generacion inicial con IA), sin llamadas pagadas.

Sobre COPIAS de una corrida REPLICATE existente (bloque G), declarada `generacion_ia: true`, con un generacion_ia.csv
construido para cada caso y el generation_source del plan S1 de O01 fijado:
  G-C  S1 = RULE_ENGINE, 3 llamadas rechazadas  -> A10 APROBADO, nota "RESPALDO DETERMINISTA" (no cuenta como IA)
  G-V  S1 = AI, 1 llamada valida                -> A10 APROBADO, nota "PROPUESTA DE LA IA VALIDA"
  G-N  S1 = AI, 3 llamadas rechazadas           -> A10 FALLO R-2
  G-E  S1 = AI, 1 llamada con error del proveedor -> A10 FALLO R-2
Ademas: v1.4 = v1.3 en la corrida sin declarar (identidad) y las pruebas de v1.3 siguen pasando con v1.4.
Uso: python3 -I pruebas_oraculo_v1_4.py <O_PERFILES.csv> <salida> <R_G> <R_P> <S_O_G> <S_O_P> <Rp_G> <Rp_P>
"""
import csv
import importlib.util
import json
import os
import shutil
import subprocess
import sys
import tempfile

csv.field_size_limit(sys.maxsize)
AQUI = os.path.dirname(os.path.abspath(__file__))
V13 = os.path.join(AQUI, "oraculo_integral_O_v1_3.py")
V14 = os.path.join(AQUI, "oraculo_integral_O_v1_4.py")


def modulo(nombre):
    spec = importlib.util.spec_from_file_location(nombre[:-3], os.path.join(AQUI, nombre))
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    return m


PN = modulo("pruebas_negativas_oraculo_v1_1.py")
P13 = modulo("pruebas_oraculo_v1_3.py")
CAMPOS = ["id", "intento", "resultado_proveedor", "prediction_id", "rechazada_por_el_backend", "motivos",
          "plan_publicado", "generation_source", "generation_attempts", "model_name", "error_generacion"]


def correr(oraculo, perfiles, g, p, salida):
    os.makedirs(salida, exist_ok=True)
    cp = subprocess.run([sys.executable, "-I", oraculo, perfiles, salida, g, p], capture_output=True, text=True)
    if cp.returncode != 0:
        raise RuntimeError(cp.stderr[-2000:])
    with open(os.path.join(salida, "detalle.json"), encoding="utf-8") as f:
        return {d["id"]: d for d in json.load(f)}


def preparar(g, fuente, llamadas):
    ruta = os.path.join(g, "00_configuracion.txt")
    with open(ruta, "a", encoding="utf-8") as f:
        f.write("generacion_ia: true (prueba R-2)\n")
    T = PN.Tablas(g)
    plan = T.planes_vigentes("O01")[0]
    plan["generation_source"] = fuente
    T.guardar()
    filas = [{"id": "O01", "intento": i + 1, "resultado_proveedor": r, "prediction_id": f"p{i}",
              "rechazada_por_el_backend": rech, "motivos": "V24 prueba" if rech == "SI" else "",
              "plan_publicado": plan["plan_id"], "generation_source": fuente, "generation_attempts": len(llamadas),
              "model_name": "gpt-5-mini", "error_generacion": ""} for i, (r, rech) in enumerate(llamadas)]
    with open(os.path.join(g, "generacion_ia.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=CAMPOS, lineterminator="\n")
        w.writeheader()
        w.writerows(filas)


CASOS = [("G-C", "RULE_ENGINE", [("RESPUESTA", "SI")] * 3, "APROBADO", "RESPALDO DETERMINISTA"),
         ("G-V", "AI", [("RESPUESTA", "NO")], "APROBADO", "PROPUESTA DE LA IA VALIDA"),
         ("G-N", "AI", [("RESPUESTA", "SI")] * 3, "FALLO", "ninguna propuesta de generacion fue valida"),
         ("G-E", "AI", [("ERROR_PROVEEDOR", "")], "FALLO", "ninguna propuesta de generacion fue valida")]


def main():
    perfiles, salida, rg, rp, sg, sp, rpg, rpp = sys.argv[1:9]
    os.makedirs(salida, exist_ok=True)
    filas, ok = [], True
    with tempfile.TemporaryDirectory() as tmp:
        a = correr(V13, perfiles, rg, rp, os.path.join(tmp, "i13"))
        b = correr(V14, perfiles, rg, rp, os.path.join(tmp, "i14"))
        ig = all(a[k]["estados"] == b[k]["estados"] and a[k]["checks"] == b[k]["checks"] for k in a)
        ok &= ig
        filas.append({"prueba": "IDENTIDAD v1.4 = v1.3 (corrida sin generacion_ia)", "esperado": "identicos",
                      "obtenido": "identicos" if ig else "DISTINTOS", "resultado": "PASA" if ig else "NO PASA"})
        P13.V13 = V14
        sys.argv = ["x", perfiles, os.path.join(tmp, "p13"), sg, sp, rpg, rpp]
        try:
            P13.main()
            pasa = True
        except SystemExit as e:
            pasa = e.code == 0
        ok &= pasa
        filas.append({"prueba": "pruebas de v1.3 (19 negativas, R-1, D1-D5) con v1.4", "esperado": "todas PASA",
                      "obtenido": "todas PASA" if pasa else "ALGUNA NO PASA", "resultado": "PASA" if pasa else "NO PASA"})
        for cod, fuente, llamadas, esp, frag in CASOS:
            mg, mp = os.path.join(tmp, cod, "G"), os.path.join(tmp, cod, "P")
            shutil.copytree(rg, mg)
            shutil.copytree(rp, mp)
            preparar(mg, fuente, llamadas)
            r = correr(V14, perfiles, mg, mp, os.path.join(tmp, cod, "v14"))["O01"]
            est = r["estados"]["A10_seguridad_integridad"]
            texto = " | ".join(r["checks"]["A10_seguridad_integridad"] + r["notas"]["A1_generacion"])
            bien = est == esp and frag in texto
            ok &= bien
            filas.append({"prueba": f"{cod} S1={fuente}, llamadas={llamadas}", "esperado": f"A10 {esp} + '{frag}'",
                          "obtenido": f"A10 {est} [{texto[:160]}]", "resultado": "PASA" if bien else "NO PASA"})
    with open(os.path.join(salida, "pruebas_oraculo_v1_4.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(filas[0]), lineterminator="\n")
        w.writeheader()
        w.writerows(filas)
    for x in filas:
        print(f"{x['resultado']:8} {x['prueba'][:70]:70} {x['obtenido'][:120]}")
    print("TODAS PASAN" if ok else "HAY PRUEBAS QUE NO PASAN")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
