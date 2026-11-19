"""Comparativa V20: antes (redondeo), 70 % exacto sin correcciones y final, con V37 y V37 + V37.1.
Uso: comparar_final.py <dir con carpetas base_v37, EX_v37, fin_v37, base_v37b, EX_v37b, fin_v37b>"""
import sys, re, collections
D = sys.argv[1]
GRUPOS = [("v37", ["base_v37", "EX_v37", "fin_v37"]), ("v37b", ["base_v37b", "EX_v37b", "fin_v37b"])]
NOMBRE = {"base": "antes (redondeo)", "EX": "70 % exacto sin correcciones", "fin": "final (exacto + 2 correcciones)"}

def rows(v, f):
    L = open(f"{D}/{v}/{f}").read().splitlines()
    h = L[0].split("\t")
    return [dict(zip(h, l.split("\t"))) for l in L[1:] if l and not l.startswith("#")]

def refs(v):
    return {l.split(" | ", 1)[0]: l.strip() for l in open(f"{D}/{v}/generacion_referencia.txt")}

def reglas(msg):
    return collections.Counter(re.findall(r"(V\d+): el entrenamiento", msg))

out = []
P = out.append
for cat, vs in GRUPOS:
    P(f"## Catalogo {cat}\n")
    P("| Variante | Refs validas | Refs que cambian vs antes | Sesiones ref < 70 % | Barrido: planes validos | Barrido: perfiles sin plan | Sesiones barrido < 70 % | Robustez validas | 192 publicados | 192 con violaciones |")
    P("|---|---|---|---|---|---|---|---|---|---|")
    B = refs(vs[0])
    for v in vs:
        R = refs(v)
        val = sum(1 for x in R.values() if "|| VALIDA" in x or x.endswith("VALIDA"))
        ch = collections.Counter(k.split("/")[0] for k in R if R[k] != B.get(k))
        rs = rows(v, "impacto_v20_referencias_sesiones.tsv")
        bajo_r = sum(1 for s in rs if int(s["segundos"]) < int(s["piso_exacto_s"]))
        bp = rows(v, "barrido_4_6_dias_planes.tsv")
        okp = sum(1 for p in bp if p["validacion"].startswith("VALIDA"))
        bs = rows(v, "barrido_4_6_dias_sesiones.tsv")
        bajo_b = sum(1 for s in bs if s["segundos"].isdigit() and int(s["segundos"]) < int(s["piso_exacto_s"]))
        rb = rows(v, "robustez_semillas_4_6_dias.tsv")
        okr = sum(1 for x in rb if x.get("resultado", "").startswith("VALIDA"))
        im = rows(v, "impacto_restricciones.tsv")
        pub = sum(1 for x in im if x["plan"] == "PUBLICADO")
        viol = sum(1 for x in im if x["violaciones_plan"] != "[]" or x["violaciones_elegibles"] != "[]")
        P(f"| {NOMBRE[v.split('_')[0]]} | {val}/72 | {dict(ch) or '—'} | {bajo_r} | {okp}/{len(bp)} | {len(bp)-okp} | {bajo_b} | {okr}/{len(rb)} | {pub}/{len(im)} | {viol} |")
    P("")
    for v in vs:
        bp = rows(v, "barrido_4_6_dias_planes.tsv")
        rb = rows(v, "robustez_semillas_4_6_dias.tsv")
        sin = [(p["perfil"], dict(reglas(p["validacion"]))) for p in bp if not p["validacion"].startswith("VALIDA")]
        fr = collections.Counter(x["perfil"] for x in rb if not x.get("resultado", "").startswith("VALIDA"))
        rr = collections.Counter()
        for x in rb:
            if not x.get("resultado", "").startswith("VALIDA"): rr.update(reglas(x["resultado"]))
        P(f"- **{NOMBRE[v.split('_')[0]]}**: barrido sin plan {sin or 'ninguno'}; robustez sin plan {dict(fr) or 'ninguno'}; sesiones rechazadas por regla en robustez {dict(rr) or '—'}")
    P("")
    # cambios de referencias del final frente a antes, por perfil
    F = refs(vs[2])
    P(f"Referencias que cambian (final frente a antes, {cat}): " + ", ".join(k for k in F if F[k] != B.get(k)))
    P("")
    # sesiones LW: cardio y composicion
    for v in vs:
        ss = [s for s in rows(v, "barrido_4_6_dias_sesiones.tsv") + rows(v, "impacto_v20_referencias_sesiones.tsv")]
        porobj = collections.defaultdict(list)
        for s in ss: porobj[s["perfil"].split("/")[0]].append(s)
        txt = []
        for o, l in sorted(porobj.items()):
            n = sum(len(re.findall(r"\d+", s["ejercicios"])) for s in l) / len(l)
            mx = max(float(s["pct_configurado"]) for s in l)
            txt.append(f"{o}: {len(l)} sesiones, {n:.2f} ejercicios de media, max {mx} %")
        P(f"- {NOMBRE[v.split('_')[0]]}: " + "; ".join(txt))
    P("")
print("\n".join(out))
