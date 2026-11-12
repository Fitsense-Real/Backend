"""5.3 Ficha de catalogo de ejercicios: fila original de V9 (dataset hasaneyldrm/exercises-dataset: zona, objetivo,
instrucciones en/es, GIF) y todas las lineas de migraciones posteriores que citan su source_code o su exercise_id.
Uso: python3 -I s8_catalogo_migraciones.py <exercise_id> ...   Salida: csv/5_3_fichas.csv"""
import sys, re, os, csv, glob
sys.path.insert(0, '/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/aud3/scripts')
from comun import *

MIG = REPO + '/src/main/resources/db/migration'
cat = leer_catalogo()
v9 = open(MIG + '/V9__catalog_full.sql', encoding='utf-8').read().splitlines()
files = sorted(glob.glob(MIG + '/*.sql'))
rows = []
for a in sys.argv[1:]:
    i = int(a)
    c = cat[i]
    sc = c['source_code']
    print('=' * 100)
    print(f"{i} [{sc}] en='{c['name_en']}' es='{c['name_es']}' grupo={c['grupo']} equipo={c['equipo']} "
          f"target={c['target_muscle']} syn={c['synergist_muscle']} sec={c['secondary_muscles']} dif={c['difficulty_level']} "
          f"presc={c['default_prescription']} high_impact={c['high_impact']} floor={c['requires_floor']} "
          f"kneel={c['requires_kneeling']} patron={c['movement_pattern']} iso={c['is_isolation']} activo={c['is_active']}")
    orig = [l for l in v9 if l.strip().startswith(f"('{sc}',")]
    instr_en = gif = orig_bp = orig_tgt = ''
    if orig:
        m = re.match(r"\s*\('([^']*)','((?:[^']|'')*)','([^']*)','([^']*)','([^']*)'", orig[0])
        orig_bp, orig_tgt = m.group(3), m.group(5)
        en = re.findall(r"'((?:[^']|'')*)'", orig[0])
        # orden: sc,name_en,bp,eq,target,syn,sec,instr_es,steps_es,instr_en,image,gif,attrib,presc
        instr_en = en[9] if len(en) > 9 else ''
        gif = en[11] if len(en) > 11 else ''
        print(f"  V9 original: zona={orig_bp} target={orig_tgt} gif={gif}")
        print(f"  V9 instructions_en: {instr_en}")
    else:
        print('  (no esta en V9: ejercicio MVP- o anadido despues)')
    pat = re.compile(r"(?<![0-9])(" + re.escape(sc) + r")(?![0-9])|exercise_id\s*(=|IN\s*\()[^;]*\b" + str(i) + r"\b|\b"
                     + str(i) + r"L?\b")
    for f in files:
        if f.endswith('V9__catalog_full.sql'):
            continue
        for n, l in enumerate(open(f, encoding='utf-8'), 1):
            if (f"'{sc}'" in l) or re.search(r"(exercise_id\s*(=|IN)|\(|,\s*)\s*" + str(i) + r"\s*[,)]", l) \
                    or re.search(r"--.*\b" + str(i) + r"\b", l):
                print(f"  {os.path.basename(f)}:{n}: {l.strip()[:260]}")
    rows.append(dict(exercise_id=i, source_code=sc, name_en=c['name_en'], name_es=c['name_es'], grupo=c['grupo'],
                     target=c['target_muscle'], v9_zona=orig_bp, v9_target=orig_tgt, patron=c['movement_pattern'],
                     aislamiento=c['is_isolation'], dificultad=c['difficulty_level'], high_impact=c['high_impact'],
                     requiere_suelo=c['requires_floor'], requiere_rodillas=c['requires_kneeling'],
                     prescripcion=c['default_prescription'], gif=gif, instrucciones_en=instr_en))
with open(SCR + '/csv/5_3_fichas.csv', 'w', newline='') as fh:
    w = csv.DictWriter(fh, fieldnames=list(rows[0]))
    w.writeheader(); w.writerows(rows)
