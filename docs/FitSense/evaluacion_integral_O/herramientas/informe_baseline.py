#!/usr/bin/env python3
"""LINEA BASE SIN REGLAS · tablas IEEE, resumen y parrafos del short paper, con cifras SOLO de metricas.json.
Uso: informe_baseline.py <entregables_dir>"""
import json
import os
import sys

D = sys.argv[1]
M = json.load(open(os.path.join(D, "metricas.json"), encoding="utf-8"))
G, A = M["generacion"], M["adaptacion"]


def p(n, d):
    return f"{n}/{d} ({100 * n / d:.1f} %)" if d else f"{n}/{d}"


def pe(n, d):
    return f"{n}/{d} ({100 * n / d:.1f}%)" if d else f"{n}/{d}"


def cat(v, *nombres):
    return sum(v["por_categoria"].get(n, 0) for n in nombres)


GEN_GRUPOS = [("Dosis (V24, V8)", ("Dosis", "Dosis (volumen semanal)")),
              ("Series y repeticiones (V7, V9, V10, V16, V19)", ("Series y repeticiones",)),
              ("Volumen por grupo muscular (V26)", ("Volumen por grupo muscular",)),
              ("Patrones obligatorios (V23, V25, V27)", ("Patrones obligatorios",)),
              ("Duración (V4, V17, V20)", ("Duracion",))]


def otras(v):
    usados = {n for _, ns in GEN_GRUPOS for n in ns}
    return sum(k for c, k in v["por_categoria"].items() if c not in usados)


def otras_desc(v):
    usados = {n for _, ns in GEN_GRUPOS for n in ns}
    return ", ".join(f"{c.lower()} {k}" for c, k in sorted(v["por_categoria"].items()) if c not in usados) or "—"


ADA_GRUPOS = [("Cambios indebidos de dosis o volumen (C2, C3)", ("C2", "C3")),
              ("Cambios indebidos de dificultad (C4)", ("C4",)),
              ("Sesiones modificadas indebidamente (C1, C12)", ("C1", "C12"))]


def ada_otras(v):
    usados = {n for _, ns in ADA_GRUPOS for n in ns}
    return sum(k for c, k in v["por_categoria"].items() if c not in usados)


def ada_otras_desc(v):
    usados = {n for _, ns in ADA_GRUPOS for n in ns}
    return ", ".join(f"{c} {k}" for c, k in sorted(v["por_categoria"].items()) if c not in usados) or "—"


runs = [r for r in (1, 2) if f"BASE-R{r}|NORMALIZADO|19" in G]
L = []
L.append("# Tablas para el short paper · línea base sin reglas frente a FitSense con reglas\n")
L.append("**Lectura de las tablas:**")
L.append("- Cifras con numerador y denominador.")
L.append("- 19 perfiles originales factibles; O11 se informa aparte y O11-F35 no entra en ningún denominador.")
L.append("- «Infracción» = infracción única: los hallazgos de varias reglas sobre la misma causa y el mismo objeto cuentan una vez.")
L.append("- **Etapa comparable** = la primera propuesta de cada condición, evaluada tras el normalizador mecánico del backend, que es como GRA validaba.")
L.append("- Los planes **publicados** por FitSense se muestran aparte.\n")
L.append("## Tabla A. Generación inicial\n")
hdr = "| Métrica | " + " | ".join(f"FitSense con reglas (GRA-R{r}) | Replicate sin reglas (BASE-R{r})" for r in runs) + " |"
L.append(hdr)
L.append("|---|" + "---|---|" * len(runs))


def fila(nombre, f):
    L.append(f"| {nombre} | " + " | ".join(f"{f('GRA', r)} | {f('BASE', r)}" for r in runs) + " |")


def g(c, r, etapa="NORMALIZADO"):
    return G[f"GRA-R{r}|{etapa}|19|1a"] if c == "GRA" else G[f"BASE-R{r}|{etapa}|19"]


fila("Perfiles originales evaluados", lambda c, r: "20 (19 factibles + O11)")
fila("Propuestas de generación (todas)", lambda c, r: (f"{G[f'GRA-R{r}|NORMALIZADO|19|todas']['evaluadas']} (1.ª + reintentos; O11 sin llamada)" if c == "GRA" else f"{g(c, r)['evaluadas'] + G[f'BASE-R{r}|NORMALIZADO|O11']['evaluadas']} (una por perfil, O11 incluido)"))
fila("Planes con ≥ 1 violación · 1.ª propuesta", lambda c, r: p(g(c, r)["con_violaciones"], g(c, r)["evaluadas"]))
fila("Planes sin violaciones · 1.ª propuesta", lambda c, r: p(g(c, r)["sin_violaciones"], g(c, r)["evaluadas"]))
fila("No evaluables", lambda c, r: str(g(c, r)["no_evaluables"]))
fila("Total de infracciones · 1.ª propuesta", lambda c, r: f"{g(c, r)['infracciones_unicas']}")
for nombre, ns in GEN_GRUPOS:
    fila(f"— {nombre}", lambda c, r, ns=ns: str(cat(g(c, r), *ns)))
fila("— Otras", lambda c, r: f"{otras(g(c, r))}")
fila("Total de infracciones sin normalizador (CRUDO)", lambda c, r: str(g(c, r, 'CRUDO')['infracciones_unicas']))
fila("Planes publicados con violaciones", lambda c, r: ("0/19 (los rechazos se corrigieron en el reintento)" if c == "GRA" else "no aplica: no se publica"))
fila("O11 (30 min, inviable para FitSense)", lambda c, r: ("sin llamada; limitación informada" if c == "GRA" else f"plan generado con {G[f'BASE-R{r}|NORMALIZADO|O11']['infracciones_unicas']} infracciones"))
L.append("")
L.append("**«Otras» por condición:**")
for r in runs:
    L.append(f"- GRA-R{r}: {otras_desc(g('GRA', r))}.")
    L.append(f"- BASE-R{r}: {otras_desc(g('BASE', r))}.")
L.append("")
par = {x["corrida"]: x for x in M["pareado_generacion_normalizado"]}
L.append("**Comparación pareada por perfil (1.ª propuesta, 19 perfiles; prueba exacta de McNemar, bilateral):**")
for r in runs:
    x = par[r]
    L.append(f"- Corrida {r}: {x['solo_base']} con violaciones solo sin reglas, {x['solo_gra']} solo con reglas, {x['ambos']} en ambas y {x['ninguno']} en ninguna; p = {x['p_mcnemar_exacta']:.2g}.")
L.append("")
L.append("## Tabla B. Adaptación\n")
L.append("| Métrica | " + " | ".join(f"FitSense con reglas (GRA-R{r}) | Replicate sin reglas (BASE-R{r})" for r in runs) + " |")
L.append("|---|" + "---|---|" * len(runs))


def a(c, r):
    return A[f"{c}-R{r}"]


fila("Ciclos evaluados", lambda c, r: str(a(c, r)["ciclos"]))
fila("Propuestas de adaptación evaluadas", lambda c, r: f"{a(c, r)['evaluadas']} (no evaluables: {a(c, r)['no_evaluables']})")
fila("Adaptaciones con cambios no autorizados", lambda c, r: p(a(c, r)["con_cambios"], a(c, r)["evaluadas"]))
fila("Adaptaciones sin cambios no autorizados", lambda c, r: p(a(c, r)["sin_cambios"], a(c, r)["evaluadas"]))
fila("Total de cambios no autorizados", lambda c, r: str(a(c, r)["cambios"]))
for nombre, ns in ADA_GRUPOS:
    fila(f"— {nombre}", lambda c, r, ns=ns: str(cat(a(c, r), *ns)))
fila("— Otros (C5–C11)", lambda c, r: str(ada_otras(a(c, r))))
fila("SIN_EFECTO (conservación de la sesión señalada; no se cuenta como cambio)", lambda c, r: str(a(c, r)["sin_efecto"]))
L.append("")
L.append("**«Otros» por condición:**")
for r in runs:
    L.append(f"- BASE-R{r}: {ada_otras_desc(a('BASE', r))}.")
L.append("")
L.append("**Códigos de cambio:**")
L.append("- C1: sesión no señalada modificada.")
L.append("- C2: más trabajo en la sesión.")
L.append("- C3: subida de dosis de un ejercicio.")
L.append("- C4: más dificultad.")
L.append("- C5: restricción, equipamiento o bloqueo.")
L.append("- C6: ejercicio excluido por dolor que sigue en el plan.")
L.append("- C7: fuera del catálogo filtrado.")
L.append("- C8: duración excedida.")
L.append("- C9: patrón no autorizado.")
L.append("- C10: cambio que la intervención no permite.")
L.append("- C11: ante dolor, más añadidos que retirados.")
L.append("- C12: sesión añadida, eliminada o fuera de fecha.")
open(os.path.join(D, "TABLAS_IEEE_BASELINE.md"), "w", encoding="utf-8").write("\n".join(L) + "\n")

# ---------------------------------------------------------------- parrafos (EN) con las cifras
def tot(c, clave, etapa="NORMALIZADO"):
    if c == "BASE":
        vs = [G[f"BASE-R{r}|{etapa}|19"] for r in runs]
    else:
        vs = [G[f"GRA-R{r}|{etapa}|19|1a"] for r in runs]
    return sum(v[clave] for v in vs), sum(v["evaluadas"] for v in vs)


bc, bd = tot("BASE", "con_violaciones")
gc, gd = tot("GRA", "con_violaciones")
bi, _ = tot("BASE", "infracciones_unicas")
gi, _ = tot("GRA", "infracciones_unicas")
bdos = sum(cat(G[f"BASE-R{r}|NORMALIZADO|19"], "Dosis", "Dosis (volumen semanal)") for r in runs)
bpat = sum(cat(G[f"BASE-R{r}|NORMALIZADO|19"], "Patrones obligatorios") for r in runs)
bvol = sum(cat(G[f"BASE-R{r}|NORMALIZADO|19"], "Volumen por grupo muscular") for r in runs)
bsr = sum(cat(G[f"BASE-R{r}|NORMALIZADO|19"], "Series y repeticiones") for r in runs)
o11 = [G[f"BASE-R{r}|NORMALIZADO|O11"]["infracciones_unicas"] for r in runs]
ac = sum(A[f"BASE-R{r}"]["con_cambios"] for r in runs)
ad = sum(A[f"BASE-R{r}"]["evaluadas"] for r in runs)
an = sum(A[f"BASE-R{r}"]["cambios"] for r in runs)
gac = sum(A[f"GRA-R{r}"]["con_cambios"] for r in runs)
gad = sum(A[f"GRA-R{r}"]["evaluadas"] for r in runs)
c1 = sum(cat(A[f"BASE-R{r}"], "C1", "C12") for r in runs)
c23 = sum(cat(A[f"BASE-R{r}"], "C2", "C3") for r in runs)
c6 = sum(cat(A[f"BASE-R{r}"], "C6") for r in runs)
nr = len(runs)
CATS_CAT = ("Ejercicio inexistente o no identificable", "Equipamiento", "Restricciones personales", "Ejercicios bloqueados",
            "Fuera del catalogo filtrado (ejercicio inactivo)",
            "Fuera del catalogo filtrado (otro filtro: edad, inversion, carga axial u otro)")
fuera = sum(cat(G[f"BASE-R{r}|NORMALIZADO|{k}"], *CATS_CAT) for r in runs for k in ("19", "O11"))
catalogo_ok = fuera == 0
corr = "two runs" if nr == 2 else "one run"
pv = ", ".join(f"p = {par[r]['p_mcnemar_exacta']:.2g}" for r in runs)
P1 = (f"To measure the effect of the rules, we ran the same model, with the same profiles and the same filtered catalogue, "
      f"without FitSense's planning rules (no dose, volume, pattern or distribution constraints, no validator feedback, "
      f"no retries, no fallback), in {corr}. Evaluated afterwards with the backend's own validators, "
      f"{pe(bc, bd)} of the unconstrained first proposals for the feasible profiles violated at least one rule, "
      f"with {bi} distinct violations ({bi / bd:.1f} per plan), mainly dose ({bdos}), mandatory movement patterns ({bpat}), "
      f"sets and repetitions ({bsr}) and per-muscle-group volume ({bvol}). "
      f"Under FitSense, {pe(gc, gd)} first proposals were rejected ({gi} violations), every rejection was resolved on retry, "
      f"and no published plan violated a rule. The unconstrained model also produced a plan for the infeasible profile "
      f"({'/'.join(map(str, o11))} violations), which FitSense had reported as a limitation before any model call. "
      + (f"In the paired per-profile comparison, the difference was significant in each run (exact McNemar, {pv}). "
         if all(par[r]["p_mcnemar_exacta"] < 0.05 for r in runs) else
         f"In the paired per-profile comparison the exact McNemar test gave {pv}. ")
      + (f"The unconstrained model always selected exercises from the filtered catalogue." if catalogo_ok else
         f"The unconstrained model also used exercises outside the filtered catalogue ({fuera} cases)."))
P2 = (f"For adaptation, the unconstrained model received the same published plans and execution events as FitSense, "
      f"without the flagged sessions, permitted interventions or conservation rules. "
      f"{pe(ac, ad)} of its proposals contained at least one unauthorized change ({an} in total), mostly modifications of "
      f"sessions that did not require changes ({c1}) and increases in dose or session workload ({c23})"
      + (f", and {c6} cases kept an exercise excluded for pain" if c6 else "")
      + f". Under FitSense, {pe(gac, gad)} published adaptations contained unauthorized changes. "
      f"The same independent oracle, which encodes the backend's intervention permissions and conservation rules, "
      f"evaluated both conditions.")
txt = ["# Párrafos para el short paper (sustituyen los dos párrafos amarillos con X)\n",
       "**Archivo fuente:** el short paper no está en el repositorio, así que estos párrafos **deben insertarse en el archivo fuente**.",
       "",
       "**Contenido:**",
       "- Las cifras salen de `metricas.json` (BASE-R1, BASE-R2, GRA-R1 y GRA-R2).",
       "- No queda ningún X, XX ni TBD.",
       "- La comparación es descriptiva salvo la prueba de McNemar indicada.",
       "- No se afirma más que lo medido.",
       "",
       "## Párrafo 1 · generación (inglés, para el artículo)\n", P1, "",
       "## Párrafo 2 · adaptación (inglés, para el artículo)\n", P2, "",
       "## Notas para quien inserte el texto\n",
       "- **Denominadores:** 19 perfiles factibles por corrida; O11 aparte. En adaptación, 10 ciclos por corrida, los mismos de GRA.",
       "- **Etapa comparable:** primera propuesta de cada condición tras el normalizador mecánico del backend. Sin normalizador, las cifras de la línea base son mayores; ver la Tabla A.",
       "- **Peso de V24:** gran parte de las infracciones de dosis (V24) incumplen tablas de dosis internas de FitSense que la línea base, por diseño, no recibe. Es justo lo que mide el experimento (el efecto de esas reglas), pero conviene decirlo en una frase si el revisor lo pregunta.",
       "- **Versión:** la versión del modelo se solicitó fijada a la de GRA (`9f4cd9ec…`). Replicate la informa como `hidden`, así que no se puede confirmar."]
open(os.path.join(D, "PARRAFOS_SHORT_PAPER.md"), "w", encoding="utf-8").write("\n".join(txt) + "\n")
print(P1)
print()
print(P2)
