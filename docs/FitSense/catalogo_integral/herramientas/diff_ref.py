"""Diferencias entre dos referencias de generacion. Uso: diff_ref.py antes.txt despues.txt ids_cambiados(coma) etiqueta"""
import sys,re,collections
a=[l for l in open(sys.argv[1],encoding='utf-8').read().splitlines() if '|' in l]
d=[l for l in open(sys.argv[2],encoding='utf-8').read().splitlines() if '|' in l]
CH=set(sys.argv[3].split(','))
A={l.split(' | ')[0]:l for l in a}; D={l.split(' | ')[0]:l for l in d}
def ids(l): return re.findall(r' (\d+):', l)
def valid(l): return l.rsplit('|',1)[-1].strip()
n=0; expl=0; out=[]; vals=collections.Counter()
for k in A:
    if A[k]==D.get(k): continue
    n+=1
    ia,idd=collections.Counter(ids(A[k])),collections.Counter(ids(D[k]))
    quit_=sorted((ia-idd).keys(),key=int); nuevos=sorted((idd-ia).keys(),key=int)
    causa=sorted(set(ids(A[k]))&CH,key=int)
    if causa: expl+=1
    vals[(valid(A[k]),valid(D[k]))]+=1
    out.append(f"| {k} | {', '.join(causa) or '—'} | {' '.join(quit_) or '—'} | {' '.join(nuevos) or '—'} | {valid(A[k])} → {valid(D[k])} |")
print(f"perfiles con cambio: {n} de {len(A)}; con un ejercicio corregido en la semana anterior: {expl}; validez: {dict(vals)}")
open(sys.argv[4],'w',encoding='utf-8').write('\n'.join(out)+'\n')
