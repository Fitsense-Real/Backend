import csv,sys,collections
def load(p):
    with open(p) as f: return {r['clave']:r for r in csv.DictReader(f,delimiter=';')}
P=load(sys.argv[1]);A=load(sys.argv[2]);B=load(sys.argv[3])
C8={1153,1301,1303,1304,1306,131,400,448}; COLG8={1153,1301,1303,1304,1306}; CAB8={131,400,448}
def ids(r): return [int(t.split(':')[2]) for t in r['ejercicios'].split()] if r['ejercicios'] else []
def out(name,X):
    print('==',name, collections.Counter(r['desenlace'].split(':')[0] for r in X.values()))
for n,X in (('pila',P),('26',A),('8',B)): out(n,X)
for k in P:
    if P[k]['desenlace']!=B[k]['desenlace'] or A[k]['desenlace']!=B[k]['desenlace']:
        print('desenlace distinto', k, 'pila',P[k]['desenlace'][:25],'26',A[k]['desenlace'][:25],'8',B[k]['desenlace'][:25])
# contained in plans of 8
viol=[]
for k,r in B.items():
    rest=k.split('/')[3]
    forb=set()
    if rest in ('SIN_COLGARSE','SIN_TRES'): forb|=COLG8
    if rest in ('SIN_CABEZA','SIN_TRES'): forb|=CAB8
    if forb & set(ids(r)): viol.append((k,forb&set(ids(r))))
print('planes de 8 con contenidos para su restriccion:', viol)
# compare 8 vs pila
same=semdiff=eldiff=0; eldiff_keys=[]
for k in P:
    if P[k]['elegibles']==B[k]['elegibles']:
        if P[k]['ejercicios']==B[k]['ejercicios']: same+=1
        else: semdiff+=1
    else:
        eldiff+=1; eldiff_keys.append(k)
print('8 vs pila: identicos',same,'mismo elegible semana distinta',semdiff,'elegible distinto',eldiff)
# what differs in eligible sets 8 vs pila
diffids=collections.Counter()
for k in eldiff_keys:
    a=set(P[k]['elegibles'].split(','));b=set(B[k]['elegibles'].split(','))
    for x in a-b: diffids['-'+x]+=1
    for x in b-a: diffids['+'+x]+=1
print('ids que difieren (pila->8):',diffids.most_common())
# 8 vs 26
same=semdiff=eldiff=0;d2=collections.Counter()
for k in A:
    if A[k]['elegibles']==B[k]['elegibles']:
        if A[k]['ejercicios']==B[k]['ejercicios']: same+=1
        else: semdiff+=1
    else:
        eldiff+=1
        a=set(A[k]['elegibles'].split(','));b=set(B[k]['elegibles'].split(','))
        for x in b-a: d2['+'+x]+=1
        for x in a-b: d2['-'+x]+=1
print('8 vs 26: identicos',same,'mismo elegible semana distinta',semdiff,'elegible distinto',eldiff)
print('ids que difieren (26->8):',d2.most_common())
for k in ('LOSE_WEIGHT/BEGINNER/HOME/SIN_SUELO','LOSE_WEIGHT/BEGINNER/HOME/SIN_TRES'):
    print(k,'| pila:',P[k]['desenlace'],'| 26:',A[k]['desenlace'],'| 8:',B[k]['desenlace'])
