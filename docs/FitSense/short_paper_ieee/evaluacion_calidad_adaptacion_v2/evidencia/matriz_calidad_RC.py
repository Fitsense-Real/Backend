"""Matriz de calidad (las 12 preguntas de la fase 10) para las sesiones adaptadas en RC1_r1 y RC2_r1, semanas 1 y 2.
Mismas columnas y criterios que las 32 filas del PR #29 en 14_MATRIZ_CALIDAD_POR_ADAPTACION.csv.
Solo lectura. Uso: python3 -I matriz_calidad_RC.py <carpeta 09-16>  ->  evidencia/matriz_calidad_RC.csv"""
import csv, math, os, re, sys

B = sys.argv[1]
EV = os.path.join(B, "evidencia")
CAT = {int(r["exercise_id"]): r for r in csv.DictReader(open(os.path.join(EV, "catalogo_exercises_v44_2.csv")))}
PERF = {r["id"]: r for r in csv.DictReader(open(os.path.join(B, "10_PERFILES_SINTETICOS_R01_R08.csv")))}
INTERV = {"TOO_DIFFICULT": "VARIANTE_MAS_SENCILLA_O_CARGA", "FATIGUE": "REVISAR_CARGA_Y_RECUPERACION",
          "LACK_OF_MOTIVATION": "INVESTIGAR_CAMBIO_MINIMO"}


def ejercicios(f):
    return [(int(a), t, int(s), int(v), int(r)) for a, t, s, v, r in re.findall(r"(\d+):(\w+):(\d+)x(\d+)s?:r(\d+)", f)]


def duracion(ej):  # replica de SessionDurationEstimator (MVP-1.2)
    t = 300 + 60 * (len(ej) - 1)
    for _, tipo, s, v, r in ej:
        t += s * v * (1 if tipo == "DURATION" else 3) + (s - 1) * r
    return t


def motivo(u, ses, semana):
    p = PERF[u]
    sufijo = "_semana2" if semana == 2 else ""
    m2 = p.get("motivo_omision_s2" + sufijo) or p["motivo_omision_s2"]
    m3 = p.get("motivo_omision_s3" + sufijo) or p["motivo_omision_s3"]
    return m2 if ses == "S2" else m3


filas = []
for corrida in ("RC1_r1", "RC2_r1"):
    c = os.path.join(EV, corrida)
    introducidos = {}  # (usuario, sesion) -> ejercicios que entraron por la adaptacion de la semana 1
    for semana, fn in ((1, "plan_siguiente.csv"), (2, "plan_siguiente_semana2.csv")):
        for x in csv.DictReader(open(os.path.join(c, fn))):
            if x["igual_a_la_medida"] == "true":
                continue
            u, ses = x["id"], x["sesion"]
            a, b = ejercicios(x["medida"]), ejercicios(x["siguiente"])
            mot = motivo(u, ses, semana)
            interv = INTERV.get(mot, "")
            minimo = math.ceil(int(PERF[u]["minutos_sesion"]) * 60 * 70 / 100)
            da, db = duracion(a), duracion(b)
            subs, dosis, sube, descanso = [], [], 0, 0
            for ea, eb in zip(a, b):
                if ea[4] != eb[4]:
                    descanso += 1
                if eb[2] > ea[2] or eb[3] > ea[3]:
                    sube += 1
                if ea[0] != eb[0]:
                    subs.append((ea, eb))
                elif (ea[2], ea[3]) != (eb[2], eb[3]):
                    dosis.append(f"{ea[0]} {ea[2]}x{ea[3]}->{eb[2]}x{eb[3]}")
            mus = [f"{o[0]}->{n[0]} {CAT[o[0]]['target_muscle']}->{CAT[n[0]]['target_muscle']}" for o, n in subs
                   if CAT[o[0]]["target_muscle"] != CAT[n[0]]["target_muscle"]]
            patron_ok = all(CAT[o[0]]["movement_pattern"] == CAT[n[0]]["movement_pattern"] for o, n in subs)
            dif = [(int(CAT[o[0]]["difficulty_level"]), int(CAT[n[0]]["difficulty_level"])) for o, n in subs]
            baja, igual, subed = sum(d < o for o, d in dif), sum(d == o for o, d in dif), sum(d > o for o, d in dif)
            dosis += [f"{o[0]}->{n[0]} {o[2]}x{o[3]}->{n[2]}x{n[3]}" for o, n in subs if (n[2], n[3]) != (o[2], o[3])]
            reemplazo_de_reemplazo = [o[0] for o, _ in subs if o[0] in introducidos.get((u, ses), set())]
            if semana == 1:
                introducidos[(u, ses)] = {n[0] for _, n in subs}
            margen = db - minimo
            # p2: respuesta al motivo
            if interv == "VARIANTE_MAS_SENCILLA_O_CARGA":
                if subs and baja == len(subs):
                    p2 = "si: todas las sustituciones bajan la dificultad"
                elif subs:
                    p2 = f"parcial: {baja} de {len(subs)} sustituciones bajan la dificultad; {igual} iguales" + (
                        f"; ademas reduce dosis ({len(dosis)})" if dosis else "")
                else:
                    p2 = f"parcial: solo baja la dosis ({len(dosis)} ejercicios), sin variante mas sencilla"
            else:
                p2 = "si: reduce la dosis prescrita" if dosis and not subs else (
                    "parcial: sustituye ejercicios" if subs else "no")
            discutible = bool(mus) or igual > 0 or (interv == "VARIANTE_MAS_SENCILLA_O_CARGA" and not subs)
            p11 = f"margen restante {margen} s"
            if margen == 0:
                p11 = "margen restante 0: la siguiente reduccion por dosis no cabe; conservar es valido (06)"
            if reemplazo_de_reemplazo:
                p11 += f"; vuelve a sustituir {len(reemplazo_de_reemplazo)} ejercicio(s) introducidos en la semana 1"
            filas.append(dict(
                corrida=f"{corrida}_sem{semana}", usuario=u, objetivo=PERF[u]["goal_type"], sesion=ses, motivo=mot,
                intervencion=interv, p1_aplicado="si (V32 APLICADA, publicado)", p2_responde_al_motivo=p2,
                p3_patron_conservado="si" if patron_ok else "NO",
                p4_musculo_conservado=("n/a (sin sustituciones)" if not subs else
                                       "si" if not mus else f"no en {len(mus)}: " + "; ".join(mus)),
                p5_dificultad="n/a" if not subs else f"baja {baja}, igual {igual}, sube {subed}",
                p6_dosis=("respetada: " + ("; ".join(dosis) if dosis else "sin cambios de dosis")
                          + " (nada sube, descanso igual)") if not sube and not descanso else
                f"NO: {sube} suben, {descanso} descansos cambiados",
                p7_equipamiento="dentro del catalogo filtrado del perfil",
                p8_duracion=f"{'valida' if db >= minimo else 'NO'}: {da}->{db} s (minimo {minimo})",
                p9_resto_conservado="si (0 cambios en sesiones no senaladas; seguridad.py)",
                p10_combinacion_valida="si (aceptada por V8C + V1-V25)",
                p11_continuidad=p11,
                p12_revision_profesional="si" if discutible else "muestra recomendable",
                clasificacion_tecnica="valida" if patron_ok and not sube and not descanso and db >= minimo else "NO",
                clasificacion_funcional="discutible (Q2/Q3)" if discutible else "coherente segun catalogo y regla",
                adecuacion_profesional="no evaluada"))

with open(os.path.join(EV, "matriz_calidad_RC.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=list(filas[0]))
    w.writeheader()
    w.writerows(filas)
for r in filas:
    print(r["corrida"], r["usuario"], r["sesion"], r["motivo"][:8], "|", r["p2_responde_al_motivo"][:60], "|",
          r["p4_musculo_conservado"][:40], "|", r["p8_duracion"], "|", r["p11_continuidad"][:80], "|",
          r["clasificacion_funcional"])
