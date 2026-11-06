#!/usr/bin/env python3
"""Cohorte O01-O20 de la evaluacion integral (orden del 06/11/2026). Determinista: sin azar, sin consultar el backend.

Escribe, junto a este script:
  O_PERFILES.csv              perfiles y guion de 4 semanas (formato del arnes EvaluacionK20IT, modo integral)
  O_CRITERIOS_ESPERADOS.csv   lo que se espera de cada perfil, fijado ANTES de ejecutar
Uso: python3 -I generar_cohorte_O.py
"""
import csv, os, sys
sys.dont_write_bytecode = True
AQUI = os.path.dirname(os.path.abspath(__file__))

EQUIPO = {"GYM": "body weight|dumbbell|barbell|cable|leverage machine|bench|band|kettlebell|ez barbell|smith machine",
          "DB": "body weight|dumbbell|band", "BW": "body weight"}
UBIC = {"GYM": "GYM", "DB": "HOME", "BW": "HOME"}
NOMBRE_EQ = {"GYM": "gimnasio", "DB": "casa con mancuernas y bandas", "BW": "casa sin material"}
DIAS_ES = {1: "lunes", 2: "martes", 3: "miercoles", 4: "jueves", 5: "viernes", 6: "sabado", 7: "domingo"}
TOPE = {"BEGINNER": 4, "INTERMEDIATE": 6, "ADVANCED": 6}
# Fecha de referencia de la edad: el backend la calcula con el reloj real (UserProfile.getAge -> LocalDate.now()).
# Las fechas de nacimiento estan elegidas lejos de cualquier cumpleanos en noviembre de 2026.

R_IMPACTO = dict(permite_alto_impacto="false")
R_HOMBRO = dict(permite_por_encima_cabeza="false", permite_colgarse="false")
R_RODILLA = dict(permite_rodillas="false")

# id, objetivo, nivel, equipo, dias disponibles, dias pedidos, minutos, nacimiento, sexo, restricciones, bloqueados,
# bloque (G = progresion apagada; P = progresion encendida), escenario, S1..S4, horizonte, intencion
P = [
 ("O01", "GAIN_MUSCLE", "BEGINNER", "GYM", [1, 3, 5], 3, 45, "1998-03-14", "MALE", {}, "", "G", "CUMPLIMIENTO_TOTAL",
  ["H/H/H"] * 4, 4, "NO_CHANGE en los 4 ciclos; la semana siguiente copia la publicada"),
 ("O02", "LOSE_WEIGHT", "BEGINNER", "BW", [2, 5], 2, 30, "1991-06-20", "FEMALE", {}, "", "G", "OMISION_POR_TIEMPO",
  ["O:LACK_OF_TIME/H", "H/H", "H/H", "H/H"], 4,
  "C1: adherencia observada 50 % -> ADAPT_LOCALIZED (ACORTAR_O_REORGANIZAR) en la sesion omitida; C2-C4 NO_CHANGE"),
 ("O03", "INCREASE_STRENGTH", "INTERMEDIATE", "GYM", [1, 2, 4, 5], 4, 60, "1985-02-11", "MALE", R_IMPACTO, "", "G",
  "FATIGA_OMISION", ["H/H/O:FATIGUE/H", "H/H/H/H", "H/H/H/H", "H/H/H/H"], 4,
  "C1: 75 % -> ADAPT_LOCALIZED (REVISAR_CARGA_Y_RECUPERACION) en la sesion 3, sin subir dosis; sin alto impacto"),
 ("O04", "GENERAL_FITNESS", "ADVANCED", "DB", [1, 2, 3, 4, 5], 5, 50, "1996-09-02", "FEMALE", {}, "", "G",
  "DIFICULTAD", ["H/O:TOO_DIFFICULT/H/P60/H", "H/H/H/H/H", "H/H/H/H/H", "H/H/H/H/H"], 4,
  "C1: 72 % -> ADAPT_LOCALIZED (VARIANTE_MAS_SENCILLA_O_CARGA) en la sesion 2 (peor: 0 %), sustituto no mas dificil"),
 ("O05", "GAIN_MUSCLE", "INTERMEDIATE", "BW", [1, 2, 4, 6], 4, 40, "2000-01-25", "MALE", {}, "", "G",
  "DESMOTIVACION", ["O:LACK_OF_MOTIVATION/O:LACK_OF_MOTIVATION/H/P40", "H/H/H/H", "H/H/H/H", "H/H/H/H"], 4,
  "C1: 35 % -> ADAPT_EXPANDED; sesiones 1-2 INVESTIGAR_CAMBIO_MINIMO; la parcial sin causa (P40) no autoriza cambios"),
 ("O06", "LOSE_WEIGHT", "INTERMEDIATE", "GYM", [1, 3, 5], 3, 35, "1974-04-18", "FEMALE", {}, "", "G",
  "DOLOR_EN_EJERCICIO", ["HD/H/H", "H/H/H", "H/H/H", "H/H/H"], 4,
  "C1: dolor en el ultimo ejercicio de la sesion 1 -> SEGURIDAD_ANTE_DOLOR (localizada); ese ejercicio no vuelve"),
 ("O07", "INCREASE_STRENGTH", "BEGINNER", "DB", [1, 2, 3, 4, 5, 6], 5, 50, "1993-07-30", "MALE", {}, "", "G",
  "SMAX_CUMPLIMIENTO", ["H/H/H/H"] * 4, 4,
  "Pide 5 dias: el tope de principiante programa 4 (aviso S_MAX); NO_CHANGE en los 4 ciclos"),
 ("O08", "GENERAL_FITNESS", "BEGINNER", "BW", [1, 2, 3, 4, 5, 6, 7], 7, 30, "1981-10-05", "FEMALE", R_IMPACTO, "",
  "G", "SMAX_SIN_REGISTRO", ["H/N/H/H", "H/H/H/H", "H/H/H/H", "H/H/H/H"], 4,
  "Pide 7 dias -> 4 (S_MAX); C1: sesion sin registro = hueco (cobertura 75 %, observada 100 %) -> NO_CHANGE"),
 ("O09", "GENERAL_FITNESS", "ADVANCED", "GYM", [1, 2, 3, 4, 5, 6, 7], 7, 75, "1997-12-08", "MALE", {}, "", "G",
  "OMISION_TOLERADA", ["H/H/H/O:SCHEDULE_CHANGE/H/H", "H/H/H/H/H/H", "H/H/H/H/H/H", "H/H/H/H/H/H"], 4,
  "Pide 7 dias -> 6 (S_MAX); C1: 83,3 % >= 80 -> NO_CHANGE (una omision no obliga a adaptar)"),
 ("O10", "LOSE_WEIGHT", "ADVANCED", "BW", [6], 1, 30, "1988-05-16", "FEMALE", {}, "", "G", "PARCIAL_SIN_CAUSA",
  ["P40", "H", "H", "H"], 4,
  "1 dia x 30 min; C1: 40 % sin causa registrada -> ADAPT_EXPANDED con CAUSA_NO_REGISTRADA: no se inventan cambios"),
 ("O11", "INCREASE_STRENGTH", "ADVANCED", "DB", [3], 1, 30, "1995-08-22", "MALE", {}, "", "G", "ESPERADO_SIN_PLAN",
  ["", "", "", ""], 4,
  "ESPERADO_SIN_PLAN: fuerza avanzada, 1 dia x 30 min (barrido V46 REV2: INVALIDO V25; precedente N19 FUERZA_SIN_TIEMPO)"),
 ("O12", "GENERAL_FITNESS", "INTERMEDIATE", "GYM", [2, 4], 2, 55, "1964-03-10", "FEMALE", R_HOMBRO, "", "G",
  "DOLOR_OMISION_MAYOR", ["H/O:PAIN_OR_DISCOMFORT", "H/H", "H/H", "H/H"], 4,
  "62 anos (sin alto impacto) y hombro (sin por encima de la cabeza ni colgarse); C1: omision por dolor -> SEGURIDAD_ANTE_DOLOR"),
 ("O13", "GAIN_MUSCLE", "BEGINNER", "BW", [1, 3, 5], 3, 60, "1959-02-17", "MALE", R_RODILLA, "1205|1263", "G",
  "PARCIAL_TOLERADO", ["H/P60/H", "H/H/H", "H/H/H", "H/H/H"], 4,
  "67 anos (sin impacto, carga axial ni inversion) y rodilla (sin arrodillarse; 1205 y 1263 bloqueados); C1: 86,7 % -> NO_CHANGE"),
 ("O14", "LOSE_WEIGHT", "BEGINNER", "GYM", [1, 2, 4, 5], 4, 40, "2002-11-27", "FEMALE", {}, "", "G", "FATIGA_TARDIA",
  ["H/H/HF/H", "O:FATIGUE/H/H/H", "H/H/H/H", "H/H/H/H"], 4,
  "C1: fatiga en un ejercicio con la sesion >= 80 % -> NO_CHANGE; C2: 75 % -> ADAPT_LOCALIZED (REVISAR_CARGA)"),
 ("O15", "GAIN_MUSCLE", "INTERMEDIATE", "GYM", [1, 3, 5], 3, 45, "1992-04-03", "MALE", {}, "", "P",
  "PROGRESION_ORDINARIA", ["H/H/H"] * 4, 4,
  "Progresion encendida: C1 NO_CHANGE sin paso (una semana no basta); desde C2, como mucho UN paso por ciclo, techo 1,10"),
 ("O16", "INCREASE_STRENGTH", "ADVANCED", "GYM", [1, 2, 4, 5], 4, 60, "1987-01-19", "FEMALE", {}, "", "P",
  "PROGRESION_NO_FORZADA", ["H/H/H/H", "H/H/O:LACK_OF_TIME/H", "H/H/H/H", "H/H/H/H"], 4,
  "C1 sin paso (una semana); C2 75 % -> ADAPT_LOCALIZED (ACORTAR); C3 sin paso (la semana previa no fue 100 %)"),
 ("O17", "GENERAL_FITNESS", "BEGINNER", "DB", [1, 3, 5], 3, 45, "1999-06-11", "FEMALE", {}, "", "P",
  "FATIGA_Y_RECUPERACION", ["H/H/O:FATIGUE", "H/H/H", "H/H/H", "H/H/H"], 4,
  "C1: 66,7 % -> ADAPT_LOCALIZED (REVISAR_CARGA, dosis reducida); despues la recuperacion nunca supera la referencia"),
 ("O18", "GAIN_MUSCLE", "ADVANCED", "GYM", [1, 3, 5], 3, 45, "1995-10-28", "MALE", {}, "", "P", "REINCORPORACION",
  ["H/H/PT", "H/H/H", "H/H/H", "H/H/H"], 26,
  "C1: parcial por falta de tiempo -> ACORTAR (retira el ultimo relleno); reincorporacion solo tras una progresion posterior"),
 ("O19", "LOSE_WEIGHT", "INTERMEDIATE", "BW", [1, 3, 5], 3, 50, "1982-09-14", "FEMALE", R_RODILLA, "", "P",
  "REINCORPORACION", ["H/PT/H", "H/H/H", "H/H/H", "H/H/H"], 26,
  "Rodilla (sin arrodillarse); C1: ACORTAR en la sesion 2; reincorporacion solo tras una progresion posterior"),
 ("O20", "INCREASE_STRENGTH", "INTERMEDIATE", "DB", [1, 3, 5], 3, 45, "1990-12-01", "MALE", {}, "", "P",
  "DOLOR_Y_PROGRESION", ["H/H/HD", "H/H/H", "H/H/H", "H/H/H"], 8,
  "C1: dolor -> SEGURIDAD_ANTE_DOLOR; con progresion encendida, el ejercicio excluido no vuelve en 8 semanas"),
]

COLS = ["id", "objetivo", "goal_type", "patron", "secuencia", "nivel", "ubicacion", "minutos_sesion", "dias_disponibles",
        "dias_por_semana", "equipamiento", "restricciones", "notas_salud", "sexo", "nacimiento", "altura_cm", "peso_kg",
        "peso_objetivo_kg", "zona_horaria", "semana_medida", "semana_siguiente", "semilla",
        "permite_alto_impacto", "permite_suelo", "permite_rodillas", "permite_colgarse", "permite_por_encima_cabeza",
        "ejercicios_bloqueados", "bloque", "escenario", "semana1", "semana2", "semana3", "semana4", "horizonte_semanas",
        "tipo_equipamiento", "sesiones_esperadas", "esperado"]


def main():
    filas, crit = [], []
    for (i, obj, niv, eq, dias, pedidos, mins, nac, sexo, restr, bloq, bloque, esc, sem, hor, intencion) in P:
        esperadas = min(pedidos, TOPE[niv], len(dias))
        r = {c: "" for c in COLS}
        r.update(id=i, objetivo=obj, goal_type=obj, patron=esc, secuencia=sem[0], nivel=niv, ubicacion=UBIC[eq],
                 minutos_sesion=mins, dias_por_semana=pedidos,
                 dias_disponibles="|".join(map(str, dias)) + " (" + ", ".join(DIAS_ES[d] for d in dias) + ")",
                 equipamiento=EQUIPO[eq], notas_salud="Ninguna", sexo=sexo, nacimiento=nac,
                 altura_cm=178 if sexo == "MALE" else 164, peso_kg=78 if sexo == "MALE" else 63,
                 peso_objetivo_kg=(72 if sexo == "MALE" else 58) if obj == "LOSE_WEIGHT" else "",
                 zona_horaria="America/Lima", semana_medida="2026-11-02", semana_siguiente="2026-11-09",
                 semilla="userId x 1000003 + epochDay(2026-11-02)",
                 permite_alto_impacto=restr.get("permite_alto_impacto", "true"),
                 permite_suelo=restr.get("permite_suelo", "true"),
                 permite_rodillas=restr.get("permite_rodillas", "true"),
                 permite_colgarse=restr.get("permite_colgarse", "true"),
                 permite_por_encima_cabeza=restr.get("permite_por_encima_cabeza", "true"),
                 ejercicios_bloqueados=bloq, bloque=bloque, escenario=esc,
                 semana1=sem[0], semana2=sem[1], semana3=sem[2], semana4=sem[3], horizonte_semanas=hor,
                 tipo_equipamiento=NOMBRE_EQ[eq], sesiones_esperadas=esperadas, esperado=intencion)
        txt = [k for k, v in restr.items()] + (["bloqueados " + bloq] if bloq else [])
        r["restricciones"] = "; ".join(txt) if txt else "ninguna"
        for k in range(4):
            if sem[k] and len(sem[k].split("/")) != esperadas:
                raise SystemExit(f"{i} S{k+1}: {len(sem[k].split('/'))} pasos para {esperadas} sesiones")
        filas.append(r)
        crit.append({"id": i, "bloque": bloque, "escenario": esc,
                     "generacion_esperada": "ESPERADO_SIN_PLAN" if esc == "ESPERADO_SIN_PLAN" else "PLAN_VALIDO",
                     "sesiones_esperadas": 0 if esc == "ESPERADO_SIN_PLAN" else esperadas,
                     "aviso_smax_esperado": "SI" if pedidos > TOPE[niv] else "NO",
                     "progresion_activada": "SI" if bloque == "P" else "NO",
                     "horizonte_semanas": hor, "intencion": intencion})
    # Comprobaciones de diseno (protocolo §1): fallan si la cohorte no cumple la cobertura minima.
    from collections import Counter
    c = Counter(r["goal_type"] for r in filas); assert all(v == 5 for v in c.values()) and len(c) == 4, c
    n = Counter(r["nivel"] for r in filas); assert all(v >= 6 for v in n.values()), n
    e = Counter(r["tipo_equipamiento"] for r in filas)
    assert e["casa sin material"] >= 6 and e["casa con mancuernas y bandas"] >= 5 and e["gimnasio"] >= 6, e
    assert sum(1 for r in filas if r["restricciones"] != "ninguna") >= 4
    assert sum(1 for r in filas if int(r["nacimiento"][:4]) <= 1966) >= 2
    assert sum(1 for x in crit if x["aviso_smax_esperado"] == "SI") >= 2
    assert sum(1 for r in filas if r["goal_type"] == "LOSE_WEIGHT" and int(r["dias_por_semana"]) <= 2) >= 2
    assert sum(1 for r in filas if r["nivel"] == "BEGINNER" and r["tipo_equipamiento"] == "casa sin material") >= 3
    with open(os.path.join(AQUI, "O_PERFILES.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=COLS, lineterminator="\n"); w.writeheader(); w.writerows(filas)
    with open(os.path.join(AQUI, "O_CRITERIOS_ESPERADOS.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(crit[0].keys()), lineterminator="\n"); w.writeheader(); w.writerows(crit)
    print("O_PERFILES.csv y O_CRITERIOS_ESPERADOS.csv:", len(filas), "perfiles;", dict(c), dict(n), dict(e))


main()
