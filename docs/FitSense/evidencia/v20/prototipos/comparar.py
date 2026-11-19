import sys,os,re,collections
D=sys.argv[1]
V=["base_v37","A_v37","EX_v37","AEX_v37","base_v37b","A_v37b","EX_v37b","AEX_v37b"]
def rows(v,f):
    p=f"{D}/{v}/{f}"
    L=open(p).read().splitlines()
    h=L[0].split("\t"); return [dict(zip(h,l.split("\t"))) for l in L[1:] if l and not l.startswith("#")]
def refs(v):
    r={}
    for l in open(f"{D}/{v}/generacion_referencia.txt"):
        k=l.split(" | ",1)[0]; r[k]=l.strip()
    return r
def ids(s): return re.findall(r"\b(\d+)(?=[:\],])",s)
out=[]
P=lambda *a: out.append(" ".join(str(x) for x in a))
P("variante\trefs_validas\trefs_cambian_vs_base(por objetivo)\tsesiones_ref_bajo_70exacto\tbarrido_validos/72\tsesiones_barrido_bajo_70exacto\trobustez_validas/288\timpacto_publicados/192\timpacto_violaciones")
for v in V:
    base="base_"+v.split("_",1)[1]
    R,B=refs(v),refs(base)
    val=sum(1 for x in R.values() if x.endswith("VALIDA") or "|| VALIDA" in x)
    ch=collections.Counter(k.split("/")[0] for k in R if R[k]!=B.get(k))
    rs=rows(v,"impacto_v20_referencias_sesiones.tsv")
    bajo_r=sum(1 for s in rs if int(s["segundos"])<int(s["piso_exacto_s"]))
    bp=rows(v,"barrido_4_6_dias_planes.tsv")
    okp=sum(1 for p in bp if p["validacion"].startswith("VALIDA"))
    bs=rows(v,"barrido_4_6_dias_sesiones.tsv")
    bajo_b=sum(1 for s in bs if s.get("segundos","").isdigit() and int(s["segundos"])<int(s["piso_exacto_s"]))
    rb=rows(v,"robustez_semillas_4_6_dias.tsv"); okr=sum(1 for x in rb if x.get("resultado","").startswith("VALIDA")); 
    im=rows(v,"impacto_restricciones.tsv"); pub=sum(1 for x in im if x["plan"]=="PUBLICADO"); viol=sum(1 for x in im if x["violaciones_plan"]!="[]" or x["violaciones_elegibles"]!="[]")
    P(v,val,dict(ch),bajo_r,f"{okp}/{len(bp)}",bajo_b,f"{okr}/{len(rb)}",f"{pub}/{len(im)}",viol,sep="\t") if False else P("\t".join(map(str,[v,val,dict(ch),bajo_r,f"{okp}/{len(bp)}",bajo_b,f"{okr}/{len(rb)}",f"{pub}/{len(im)}",viol])))
# fallos de barrido y robustez
for v in V:
    bp=rows(v,"barrido_4_6_dias_planes.tsv")
    f=[p["perfil"] for p in bp if not p["validacion"].startswith("VALIDA")]
    rb=rows(v,"robustez_semillas_4_6_dias.tsv")
    fr=collections.Counter(x["perfil"] for x in rb if not x.get("resultado","").startswith("VALIDA"))
    P(f"## {v} barrido_sin_plan={f} robustez_sin_plan={dict(fr)}")
# ejercicios nuevos vs base (barrido + refs), y grupos
for v in V:
    base="base_"+v.split("_",1)[1]
    if v==base: continue
    def multiset(vv):
        c=collections.Counter()
        for s in rows(vv,"barrido_4_6_dias_sesiones.tsv")+rows(vv,"impacto_v20_referencias_sesiones.tsv"):
            for i in ids(s["ejercicios"]): c[(s["perfil"].split("/")[0],i)]+=1
        return c
    a,b=multiset(v),multiset(base)
    nuevos=collections.Counter(); 
    for k in a:
        if a[k]>b.get(k,0): nuevos[k]+=a[k]-b.get(k,0)
    P(f"## {v} apariciones nuevas por (objetivo,id): {dict(sorted(nuevos.items(),key=lambda x:-x[1]))}")
    # sesiones LOSE_WEIGHT: media colocaciones y grupos
    for vv in (base,v):
        ss=[s for s in rows(vv,"barrido_4_6_dias_sesiones.tsv")+rows(vv,"impacto_v20_referencias_sesiones.tsv") if s["perfil"].startswith("LOSE_WEIGHT")]
        n=sum(len(ids(s["ejercicios"])) for s in ss)/len(ss); mx=max(int(s["segundos"]) for s in ss); pct=max(float(s["pct_configurado"]) for s in ss)
        P(f"   {vv}: sesiones LW={len(ss)} media_ejercicios={n:.2f} max_s={mx} max_pct={pct} banderas={collections.Counter(b for s in ss for b in s['banderas'].split(',') if b)}")
print("\n".join(out))
