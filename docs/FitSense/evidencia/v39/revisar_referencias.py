"""Revisa las diferencias entre dos referencias de 72 perfiles con el catalogo de la nueva.
Uso: revisar_referencias.py <ref_antes> <ref_despues> <catalogo_despues.tsv> > informe.md"""
import sys, re, collections
antes, despues, catp = sys.argv[1:4]
C = "id src active name_en name_es bp target syn sec eq reqs diff presc pattern iso hi floor axial kneel hang over inv gif ien ies".split()
cat = {}
for l in open(catp):
    if l.startswith("EQ\t"): continue
    d = dict(zip(C, l.rstrip("\n").split("\t"))); cat[d["id"]] = d
SLOT = {"HORIZONTAL_PULL": "UPPER_PULL", "VERTICAL_PULL": "UPPER_PULL", "HORIZONTAL_PUSH": "UPPER_PUSH", "VERTICAL_PUSH": "UPPER_PUSH",
        "KNEE_DOMINANT": "LOWER_KNEE", "HIP_HINGE": "LOWER_HIP", "HIP_EXTENSION": "LOWER_HIP"}
def clase(d):
    p = d["pattern"]
    if p == "LOCOMOTION_CARDIO": return "CARDIO"
    if p == "MOBILITY": return "MOVILIDAD"
    if p == "BALANCE_CONTROL": return "EQUILIBRIO"
    return "ESTRUCTURAL" if p in SLOT else "RESISTIDO"
CASA = {"body weight", "dumbbell", "band"}
def equipo_ok(d, casa):
    if not casa or not d["reqs"]: return True
    g = collections.defaultdict(set)
    for x in d["reqs"].split(","): a, b = x.split(":", 1); g[a].add(b)
    return all(v & CASA for v in g.values())
def leer(p):
    r = {}
    for l in open(p):
        k, resto = l.rstrip("\n").split(" | ", 1)
        ses = [s.strip() for s in resto.split("||")[0].split("| ") if s.strip()]
        r[k] = (ses, resto.split("||")[-1].strip())
    return r
A, B = leer(antes), leer(despues)
NIVEL = {"BEGINNER": 1, "INTERMEDIATE": 2, "ADVANCED": 3}
problemas, nuevos = [], collections.Counter()
porobj = collections.defaultdict(lambda: collections.Counter())
lineas = []
for k in A:
    obj, niv, mins, lugar = k.split("/")
    casa = lugar == "CASA"
    ea = [re.findall(r"(\d+):(\S+)", s) for s in A[k][0]]
    eb = [re.findall(r"(\d+):(\S+)", s) for s in B[k][0]]
    ida = {i for s in ea for i, _ in s}; idb = {i for s in eb for i, _ in s}
    entran, salen = sorted(idb - ida, key=int), sorted(ida - idb, key=int)
    for s, txt in zip(eb, B[k][0]):
        clases = collections.Counter()
        for i, dosis in s:
            d = cat[i]; c = clase(d); clases[c] += 1
            if not equipo_ok(d, casa): problemas.append(f"{k}: {i} {d['name_en']} exige {d['reqs']} en casa")
            if int(d["diff"]) > NIVEL[niv]: problemas.append(f"{k}: {i} dificultad {d['diff']} para {niv}")
            if niv == "BEGINNER" and d["hi"] == "t": problemas.append(f"{k}: {i} alto impacto para principiante")
            if c == "MOVILIDAD" and re.match(r"\d+x\d+r/(90|120|150)", dosis): problemas.append(f"{k}: {i} movilidad con dosis de fuerza {dosis}")
            if obj == "GAIN_MUSCLE" and c not in ("ESTRUCTURAL", "RESISTIDO"): problemas.append(f"{k}: {i} {c} en GAIN_MUSCLE")
        if obj == "LOSE_WEIGHT" and clases["CARDIO"] == 0: problemas.append(f"{k}: sesion sin cardio: {txt[:60]}")
    for i in entran:
        d = cat[i]; nuevos[(obj, clase(d))] += 1
        porobj[obj][f"{i} {d['name_en']} [{d['bp']}/{clase(d)}/d{d['diff']}]"] += 1
    lineas.append(f"| {k} | {B[k][1]} | {len(salen)} | {len(entran)} | {', '.join(entran[:8])}{' ...' if len(entran) > 8 else ''} |")
print("# Diferencias de las 72 referencias: V37 (motor V20) frente a V39 (motor E1/E2)\n")
print(f"Perfiles que cambian: {sum(1 for k in A if A[k][0] != B[k][0])} de {len(A)}. Validos en V39: {sum(1 for k in B if B[k][1] == 'VALIDA')}.\n")
print("## Comprobaciones sobre TODOS los ejercicios de V39 (no solo los nuevos)\n")
print("Equipamiento de casa segun requisitos de V39, dificultad del nivel, impacto en principiante, movilidad sin dosis de fuerza, GAIN_MUSCLE solo resistido o estructural, LOSE_WEIGHT con cardio en cada sesion.\n")
print("**Problemas: " + (str(len(problemas)) if problemas else "0") + "**\n")
for p in problemas: print("- " + p)
print("\n## Ejercicios que entran, por objetivo y clase\n")
for (o, c), n in sorted(nuevos.items()): print(f"- {o} / {c}: {n}")
print("\n## Ejercicios nuevos mas frecuentes por objetivo (para revision humana)\n")
for o, c in porobj.items():
    print(f"**{o}**: " + "; ".join(f"{x} ({n})" for x, n in c.most_common(10)) + "\n")
print("## Por perfil\n\n| Perfil | Validacion V39 | Salen | Entran | Entran (ids) |\n|---|---|---|---|---|")
print("\n".join(lineas))
