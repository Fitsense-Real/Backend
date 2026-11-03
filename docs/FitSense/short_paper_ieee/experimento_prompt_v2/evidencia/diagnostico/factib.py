import json,re,sys,csv
B=sys.argv[1]
SPR,TRANS,WARM,DREST=3,60,5,60
t=[json.loads(l) for l in open(B+'/14_TRAZAS_IA_REAL.jsonl')]
log=open(B+'/evidencia/C/ejecucion_maven.log',encoding='utf-8',errors='replace').read()
res={r['id']:r for r in csv.DictReader(open(B+'/evidencia/C/resultados_20.csv'))}
uid={r['user_id']:r['id'] for r in res.values()}
rech={}
for m in re.finditer(r'Intento (\d) para el usuario (\d+): la semana propuesta no pasa \S* \[(.*)', log):
    rech[(uid[m.group(2)], m.group(1))]=m.group(3)
def parse_dose(d):
    m=re.match(r'(\d+)x(\d+)(s?)',d)
    s,v,dur=int(m.group(1)),int(m.group(2)),m.group(3)=='s'
    return dict(sets=s,reps=None if dur else v,secs=v if dur else None,tipo='DURATION' if dur else 'SETS_REPS')
def ex_secs(e):
    w=e['sets']*e['secs'] if e['tipo']=='DURATION' else e['sets']*e['reps']*SPR
    r=max(0,e['sets']-1)*(e['rest'] if e['rest'] is not None else DREST)
    return w+r
def dur(exs): return 0 if not exs else WARM*60+sum(ex_secs(e) for e in exs)+(len(exs)-1)*TRANS
def sesiones(pr):
    i=pr.find('SESIONES SENALADAS'); j=pr.find('Datos de entrada:',i); out=[]
    for s in re.split(r'\n- source_planned_workout_id: ', pr[i:j])[1:]:
        sid=int(s.split('\n')[0]); g=lambda p:(re.search(p,s).group(1).strip() if re.search(p,s) else '')
        exs=[]
        for a,n,d,r in re.findall(r'exercise_id (\d+) \((.*)\): (\d+x\d+|\d+ series de \d+ s)(?:, descanso (\d+) s)?\n', s):
            m2=re.match(r'(\d+) series de (\d+) s',d)
            e=parse_dose(f"{m2.group(1)}x{m2.group(2)}s" if m2 else d); e.update(id=int(a),name=n,rest=int(r) if r else None); exs.append(e)
        m=re.search(r'ahora dura unos (\d+) s.*?(?:no puede bajar de (\d+) s)?\.',s)
        out.append(dict(sid=sid,motivo=g(r'como le fue: (.*)'),puede=g(r'QUE PUEDES CAMBIAR AQUI: (.*)'),
            dosis=g(r'DOSIS: (.*)'),dur=int(m.group(1)),minimo=int(m.group(2)) if m.group(2) else None,exs=exs))
    return out
ad=[x for x in t if x['tipo']=='ADAPTACION']
users=sorted(set(x['etiqueta'].split('/')[0] for x in ad))
report={}
for U in users:
    its=sorted([x for x in ad if x['etiqueta'].startswith(U+'/')],key=lambda x:int(x['intento']))
    p0=json.load(open(B+'/evidencia/C/predicciones_replicate/'+its[0]['prediccion_id']+'.json'))
    pr=p0['input']['prompt']; snap=json.loads(pr[pr.find('Datos de entrada:')+17:].strip().split('\n')[0])
    cat={e['exercise_id']:e for e in snap['available_exercises']}
    goal=snap['user']['goal_type']
    print('='*100); print(U,goal,snap['user']['fitness_level'],'prompt',len(pr),'chars; tokens in',its[0]['tokens_entrada'])
    for s in sesiones(pr):
        d=dur(s['exs']); marg=s['dur']-s['minimo'] if s['minimo'] else None
        print(f"\n-- S{s['sid']} motivo={s['motivo']} | {s['puede'][:70]}\n   dur prompt {s['dur']} / recalculada {d} / minimo {s['minimo']} / MARGEN {marg}")
        unit=[]
        for e in s['exs']:
            c=cat.get(e['id'],{})
            mr=c.get('min_reps')
            o=[]
            if e['tipo']=='SETS_REPS':
                if mr is not None and e['reps']-1>=mr: o.append(('reps-1',e['sets']*SPR))
                if e['sets']>2: o.append(('series-1',e['reps']*SPR+(e['rest'] or DREST)))
            else:
                if e['sets']>2: o.append(('series-1',e['secs']+(e['rest'] or DREST)))
                if e['secs']-10>=20: o.append(('-10 s',e['sets']*10))
            unit.append((e,o))
            vari=[x for x in snap['available_exercises'] if x['exercise_id']!=e['id'] and x.get('movement_pattern')==c.get('movement_pattern') and x.get('target_muscle')==c.get('target_muscle') and x['difficulty']<=c.get('difficulty',9) and x['prescription_type']==e['tipo'] and x['exercise_id'] not in [y['id'] for y in s['exs']]]
            print(f"   {e['id']:>5} {e['name'][:38]:38} {e['sets']}x{e['reps'] or str(e['secs'])+'s'} r{e['rest']} | min_reps {mr} dif {c.get('difficulty')} {c.get('movement_pattern')}/{c.get('target_muscle')} clase {c.get('clase_dosis')} | opciones {o} | variantes mismo patron+musculo, dif<=: {len(vari)}")
        if marg is not None:
            minimo_unit=min([x[1] for e,o in unit for x in o],default=None)
            reps_tot=0
            for e,o in unit:
                c=cat.get(e['id'],{})
                if e['tipo']=='SETS_REPS' and c.get('min_reps') is not None:
                    reps_tot+=min(e['reps']-c['min_reps'], marg//(e['sets']*SPR))
            caben=[ (e['id'],x) for e,o in unit for x in o if x[1]<=marg]
            print(f"   >> menor retirada unitaria {minimo_unit} s; reducciones unitarias que caben en el margen: {caben}")
        for x in its:
            pp=json.load(open(B+'/evidencia/C/predicciones_replicate/'+x['prediccion_id']+'.json'))
            out=pp['output']['json_output']; ps=[q for q in out.get('sessions',[]) if q['source_planned_workout_id']==s['sid']]
            if not ps: print(f"   intento {x['intento']}: OMITIDA"); continue
            nex=[dict(id=q['exercise_id'],sets=q['planned_sets'],reps=q['planned_reps'],secs=q['planned_duration_seconds'],tipo=q['prescription_type'],rest=q['rest_seconds']) for q in ps[0]['exercises']]
            dn=dur(nex); 
            ch=[]
            old={e['id']:e for e in s['exs']}
            for q in nex:
                o=old.get(q['id'])
                if not o: ch.append(f"+{q['id']}")
                elif (o['sets'],o['reps'],o['secs'],o['rest'])!=(q['sets'],q['reps'],q['secs'],q['rest']): ch.append(f"{q['id']}:{o['sets']}x{o['reps'] or o['secs']}r{o['rest']}->{q['sets']}x{q['reps'] or q['secs']}r{q['rest']}")
            ch+=[f"-{i}" for i in old if i not in [q['id'] for q in nex]]
            print(f"   intento {x['intento']}: dur {dn} (retira {d-dn} s; margen {marg}) {'RECHAZO: '+rech[(U,x['intento'])][:160] if (U,x['intento']) in rech else ''}\n      cambios: {ch}")

print('\n\n###### DESGLOSE POR CAMBIO DE LAS PROPUESTAS RECHAZADAS ######')
import itertools
for U in users:
    its=sorted([x for x in ad if x['etiqueta'].startswith(U+'/')],key=lambda x:int(x['intento']))
    p0=json.load(open(B+'/evidencia/C/predicciones_replicate/'+its[0]['prediccion_id']+'.json'))
    for s in sesiones(p0['input']['prompt']):
        marg=s['dur']-s['minimo']; old={e['id']:e for e in s['exs']}
        for x in its:
            if (U,x['intento']) not in rech: continue
            pp=json.load(open(B+'/evidencia/C/predicciones_replicate/'+x['prediccion_id']+'.json'))
            ps=[q for q in pp['output']['json_output'].get('sessions',[]) if q['source_planned_workout_id']==s['sid']]
            if not ps: continue
            nex=[dict(id=q['exercise_id'],sets=q['planned_sets'],reps=q['planned_reps'],secs=q['planned_duration_seconds'],tipo=q['prescription_type'],rest=q['rest_seconds']) for q in ps[0]['exercises']]
            per=[]
            for q in nex:
                o=old.get(q['id'])
                if o and (o['sets'],o['reps'],o['secs'])!=(q['sets'],q['reps'],q['secs']): per.append((f"{q['id']} {o['sets']}x{o['reps'] or str(o['secs'])+'s'}->{q['sets']}x{q['reps'] or str(q['secs'])+'s'}", ex_secs(o)-ex_secs(q)))
            subs=[q for q in nex if q['id'] not in old]; outs=[o for i,o in old.items() if i not in [q['id'] for q in nex]]
            if subs or outs: per.append((f"sustituciones {[o['id'] for o in outs]}->{[q['id'] for q in subs]}", sum(ex_secs(o) for o in outs)-sum(ex_secs(q) for q in subs)))
            tot=sum(v for _,v in per)
            best=None
            for k in range(len(per),0,-1):
                for c in itertools.combinations(per,k):
                    sm=sum(v for _,v in c)
                    if 0<sm<=marg and (best is None or len(c)>len(best[0]) or (len(c)==len(best[0]) and sm>best[1])): best=(c,sm)
                if best: break
            print(f"{U} i{x['intento']} S{s['sid']} margen {marg} retira {tot}: {per}\n    mayor subconjunto propio que cabe: {[n for n,_ in best[0]] if best else 'ninguno'} ({best[1] if best else 0} s)")
