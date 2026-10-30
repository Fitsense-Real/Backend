"""Comparativa por etapa. Uso: comparar_v38.py base=<dir> nombre=<dir> ..."""
import sys, re, collections
V=[a.split("=",1) for a in sys.argv[1:]]
def rows(d,f):
    L=open(f"{d}/{f}").read().splitlines(); h=L[0].split("\t"); return [dict(zip(h,l.split("\t"))) for l in L[1:] if l and not l.startswith("#")]
def refs(d): return {l.split(" | ",1)[0]:l.strip() for l in open(f"{d}/generacion_referencia.txt")}
B=refs(V[0][1])
print("| Variante | Refs validas | Refs que cambian | Barrido con plan | Sin plan (barrido) | Robustez | Rechazos robustez | 192 publicados | Sesiones < 70 % |")
print("|---|---|---|---|---|---|---|---|---|")
for n,d in V:
    R=refs(d); bp=rows(d,"barrido_4_6_dias_planes.tsv"); rb=rows(d,"robustez_semillas_4_6_dias.tsv"); im=rows(d,"impacto_restricciones.tsv")
    bs=rows(d,"barrido_4_6_dias_sesiones.tsv")+rows(d,"impacto_v20_referencias_sesiones.tsv")
    ch=collections.Counter(k.split("/")[0] for k in R if R[k]!=B.get(k))
    sin=[p["perfil"] for p in bp if not p["validacion"].startswith("VALIDA")]
    rr=collections.Counter(); 
    for x in rb:
        if not x["resultado"].startswith("VALIDA"): rr.update(re.findall(r"(V\d+): el", x["resultado"]))
    print(f"| {n} | {sum(1 for x in R.values() if x.endswith('VALIDA'))}/72 | {dict(ch) or '-'} | {len(bp)-len(sin)}/{len(bp)} | {', '.join(sin) or '-'} | {sum(x['resultado'].startswith('VALIDA') for x in rb)}/{len(rb)} | {dict(rr) or '-'} | {sum(x['plan']=='PUBLICADO' for x in im)}/{len(im)} | {sum(int(s['segundos'])<int(s['piso_exacto_s']) for s in bs)} |")
