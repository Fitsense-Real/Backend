#!/usr/bin/env python3
"""Oraculo K20 VERSIONADO por politica de planificacion (rediseno tras la auditoria R/R').

No modifica el oraculo historico (docs/FitSense/short_paper_ieee/k20_adaptacion_real/herramientas/oraculo_k20.py,
evidencia del PR #41): lo IMPORTA y cambia solo la funcion validar_plan, de forma explicita y documentada.

Politica PLAN-HIST-1: identico al historico (no cambia nada; sirve de control: debe dar la misma huella).

Politica PLAN-REV-2.0 (cambios aprobados en la orden del 05/11/2026, P-G1, P-G3; ver 03_ORACULO_VERSIONADO.md):
  QUITA  (una sola regla, aprobada):
    V20   piso del 70 % de sessionMinutes. P-G1: "V20 deja de rechazar solo por no llegar al 70 %".
  MANTIENE todas las demas del historico, incluida V4 historica (declarados <= round(M x 1,15)).
  ANADE  (mas estrictas, independientes del backend):
    V4M   segundos estimados <= M x 60 (M = minutos del perfil; si la sesion declara minutos disponibles por
          falta de tiempo, el menor de los dos). P-G1: "V4 respeta el maximo".
    V26O  series directas SETS_REPS por grupo grande (chest, back, shoulders, upper legs, upper arms):
          por sesion <= 6 (BEGINNER) / 10; por semana <= 12 / 20 / 24 (BEGINNER / INTERMEDIATE / ADVANCED).
          Aproximacion propia: cuenta TODO ejercicio SETS_REPS del grupo (incluidos los que el backend trata como
          complemento), asi que es igual o mas estricta que V26.
    V27O  una sesion PULL tiene al menos un ejercicio HORIZONTAL_PULL o VERTICAL_PULL (P-G3: no hay PULL falsos).
    QINS  sesion insuficiente: menos de 3 ejercicios o menos de 6 series en total.
Las cifras de V26O y QINS son las de DosisSemanalPolicy / CalidadDosis, todas [HP][PRO] (02_EVIDENCIA_DOSIS.md).

Uso:
    python3 -I oraculo_k20_politica.py <PLAN-HIST-1|PLAN-REV-2.0> <carpeta> <perfiles.csv> [<teorico.csv>]
Escribe en <carpeta>/oraculo_<politica>/ (no pisa <carpeta>/oraculo/).
"""
import importlib.util
import os
import shutil
import sys
from datetime import date

AQUI = os.path.dirname(os.path.abspath(__file__))
HISTORICO = os.path.normpath(os.path.join(AQUI, "..", "..", "short_paper_ieee", "k20_adaptacion_real",
                                          "herramientas", "oraculo_k20.py"))

GRANDES = {"chest", "back", "shoulders", "upper legs", "upper arms"}
TECHO_SEMANA = {"BEGINNER": 12, "INTERMEDIATE": 20, "ADVANCED": 24}
TRACCION = {"HORIZONTAL_PULL", "VERTICAL_PULL"}


def cargar_historico():
    sys.dont_write_bytecode = True  # no tocar el __pycache__ versionado del oraculo historico (evidencia)
    spec = importlib.util.spec_from_file_location("oraculo_k20", HISTORICO)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def validar_plan_rev2(mod, original):
    def validar(D, plan, perfil, nombre):
        problemas = [p for p in original(D, plan, perfil, nombre) if ": V20 " not in p]
        codigo = {r["body_part_id"]: r["code"] for r in D.tab["body_parts"]}
        nivel = perfil["fitness_level"]
        minutos = int(perfil["session_minutes"])
        techo_sesion = 6 if nivel == "BEGINNER" else 10
        techo_semana = TECHO_SEMANA.get(nivel, 20)
        semana = {}
        for w in D.sesiones(int(plan["plan_id"])):
            f = date.fromisoformat(w["scheduled_date"])
            ej = D.ejercicios(w)
            m = minutos
            if w.get("declared_available_minutes"):
                m = min(m, int(w["declared_available_minutes"]))
            seg = mod.segundos_estimados(ej, D.p)
            if seg > m * 60:
                problemas.append(f"{nombre} {f}: V4M {seg} s > {m * 60} s")
            sesion = {}
            for e in ej:
                c = D.ejercicio.get(e["id"])
                if c is None or e["type"] != "SETS_REPS":
                    continue
                g = codigo.get(c["body_part_id"])
                if g in GRANDES:
                    sesion[g] = sesion.get(g, 0) + (e["sets"] or 0)
            for g, n in sorted(sesion.items()):
                if n > techo_sesion:
                    problemas.append(f"{nombre} {f}: V26O {n} series de {g} en la sesion > {techo_sesion}")
                semana[g] = semana.get(g, 0) + n
            if w["focus_code"] == "PULL" and not any(
                    (D.ejercicio.get(e["id"]) or {}).get("movement_pattern") in TRACCION for e in ej):
                problemas.append(f"{nombre} {f}: V27O sesion PULL sin ejercicio de traccion")
            series = sum((e["sets"] or 1) for e in ej)
            if len(ej) < 3 or series < 6:
                problemas.append(f"{nombre} {f}: QINS {len(ej)} ejercicios y {series} series")
        for g, n in sorted(semana.items()):
            if n > techo_semana:
                problemas.append(f"{nombre}: V26O {n} series de {g} en la semana > {techo_semana}")
        return problemas
    return validar


def main():
    politica, carpeta = sys.argv[1], sys.argv[2]
    if politica not in ("PLAN-HIST-1", "PLAN-REV-2.0"):
        sys.exit(f"politica desconocida: {politica}")
    mod = cargar_historico()
    if politica == "PLAN-REV-2.0":
        mod.validar_plan = validar_plan_rev2(mod, mod.validar_plan)
    propio = os.path.join(carpeta, "oraculo")
    aparte = os.path.join(carpeta, ".oraculo_historico_aparte")
    destino = os.path.join(carpeta, "oraculo_" + politica)
    if os.path.exists(propio):
        os.rename(propio, aparte)
    try:
        sys.argv = [HISTORICO] + sys.argv[2:]
        mod.main()
        if os.path.exists(destino):
            shutil.rmtree(destino)
        os.rename(propio, destino)
        with open(os.path.join(destino, "politica_oraculo.txt"), "w", encoding="utf-8") as f:
            f.write(politica + "\n")
    finally:
        if os.path.exists(aparte):
            os.rename(aparte, propio)


if __name__ == "__main__":
    main()
