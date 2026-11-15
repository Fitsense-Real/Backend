"""Matriz de coherencia final del catalogo: reglas automaticas por ejercicio sobre el registro maestro
final y resumen por regla. No decide nada: mide.
Uso: python3 -I -B matriz_coherencia.py <registro_maestro_final.tsv> <REGISTRO_DECISIONES_PENDIENTES.tsv> <salida.xlsx> <salida.tsv>"""
import csv, sys, re, collections
reg, prof, out_x, out_t = sys.argv[1:5]
rd = lambda f: list(csv.DictReader(open(f, encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE))
R = [r for r in rd(reg) if r['activo'] == 'SI']
P = collections.defaultdict(list)
for p in rd(prof):
    # Registro unico de decisiones (orden de desbloqueo): solo cuenta lo que sigue abierto o contenido.
    if 'estado' in p:
        if not p['id'] or p['estado'] == 'RESUELTA_TECNICA': continue
        P[p['id']].append(p['origen'])
    else:
        P[p['id']].append(p['categoria'])
low = lambda s: s.lower()

def ultima(t):
    f = [x.strip() for x in re.split(r'(?<=[.!?])\s+', t.strip()) if x.strip()]
    return f[-1] if f else ''

REGLAS = [
    ('R01', 'HG implica OH', 'CHECK ck_exercises_hanging_is_overhead',
     lambda r, b: 'HG' not in b or 'OH' in b),
    ('R02', 'HI implica impacto distinto de BAJO', 'CHECK ck_exercises_high_impact_not_bajo',
     lambda r, b: 'HI' not in b or r['impacto'] != 'BAJO'),
    ('R03', 'Impacto ALTO implica HI', 'coherencia V51 (ALTO sin HI no lo excluye «sin impacto»)',
     lambda r, b: r['impacto'] != 'ALTO' or 'HI' in b),
    ('R04', 'Impacto y estiramiento explicitos en todo activo', 'CHECK ck_exercises_active_impact_stretch',
     lambda r, b: r['impacto'] in ('BAJO', 'MODERADO', 'ALTO') and r['estiramiento'] in ('SI', 'NO')),
    ('R05', 'El equipo principal esta entre los requisitos', 'trigger tg_requisitos_principal',
     lambda r, b: r['equipo_principal'] in ('body weight', '') or r['equipo_principal'] in r['requisitos']),
    ('R06', 'Un estiramiento no es de alto impacto', 'is_stretch / impact_level',
     lambda r, b: r['estiramiento'] != 'SI' or r['impacto'] == 'BAJO'),
    ('R07', 'DURATION: el texto no pide repeticiones en su frase final', 'prescripcion / instrucciones',
     lambda r, b: r['prescripcion'] != 'DURATION' or 'repetici' not in low(ultima(r['instrucciones_es']))),
    ('R08', 'SETS_REPS: el texto no pide mantener una duracion en su frase final', 'prescripcion / instrucciones',
     lambda r, b: r['prescripcion'] != 'SETS_REPS' or 'duración indicada' not in low(ultima(r['instrucciones_es']))),
    ('R09', 'Textos reescritos por V52: contienen la frase reglamentaria de su prescripcion', 'gen_v52.py',
     lambda r, b: 'instructions_es' not in r['cambios_V52'] or
     (('Mantén la duración indicada' in r['instrucciones_es']) if r['prescripcion'] == 'DURATION'
      else ('Repite el número de repeticiones indicado' in r['instrucciones_es']))),
    ('R10', 'Dificultad nunca bajada en el cierre', 'V27 / gen_v52.py',
     lambda r, b: int(r['dificultad']) >= int(r['dificultad_anterior_V50'] or r['dificultad'])),
    ('R11', 'Identidad del movimiento confirmada (CLARA) en la R2', 'nombre / GIF',
     lambda r, b: r['r2_identidad'] == 'CLARA'),
    ('R12', 'Informativa: el nombre dice «estiramiento» y el atributo is_stretch coincide (el nombre ya no decide; las diferencias son decisiones V3)', 'is_stretch (V51) frente al nombre',
     lambda r, b: ('estiramiento' in low(r['nombre_es'])) == (r['estiramiento'] == 'SI')),
    ('R13', 'Sin decision profesional pendiente', 'REGISTRO_DECISIONES_PENDIENTES.tsv',
     lambda r, b: r['id'] not in P),
]
filas, resumen = [], []
for cod, nombre, fuente, f in REGLAS:
    malos = []
    for r in R:
        b = r['banderas'].split()
        if not f(r, b):
            malos.append(r)
    resumen.append([cod, nombre, fuente, len(R), len(R) - len(malos), len(malos),
                    ' '.join(r['id'] for r in malos[:60]) + (' ...' if len(malos) > 60 else '')])
    for r in malos:
        filas.append([r['id'], r['nombre_es'], cod, nombre, r['prescripcion'], r['impacto'], r['estiramiento'],
                      r['banderas'], r['dificultad'], r['contencion_vigente'], ';'.join(P.get(r['id'], [])) or '-',
                      ultima(r['instrucciones_es'])[:160]])
H1 = ['regla', 'descripcion', 'fuente', 'activos', 'cumplen', 'no_cumplen', 'ids_no_cumplen']
H2 = ['id', 'nombre', 'regla', 'descripcion', 'prescripcion', 'impacto', 'estiramiento', 'banderas', 'dificultad',
      'contencion', 'pendiente_profesional', 'frase_final']
w = csv.writer(open(out_t, 'w', encoding='utf-8'), delimiter='\t', lineterminator='\n')
w.writerow(H2); w.writerows(filas)
import openpyxl
from openpyxl.styles import Font, PatternFill
wb = openpyxl.Workbook()
for nom, H, D in (('Reglas', H1, resumen), ('Incumplimientos', H2, filas)):
    ws = wb.active if nom == 'Reglas' else wb.create_sheet(nom)
    ws.title = nom
    ws.append(H)
    for c in ws[1]:
        c.font = Font(bold=True); c.fill = PatternFill('solid', fgColor='DDDDDD')
    for d in D:
        ws.append(d)
    ws.freeze_panes = 'A2'
    ws.auto_filter.ref = ws.dimensions
sis = wb.create_sheet('Sistema')
SIS = [l.rstrip('\n').split('\t') for l in open(sys.argv[0].replace('matriz_coherencia.py', 'matriz_sistema.tsv'), encoding='utf-8')]
for l in SIS:
    sis.append(l)
for c in sis[1]:
    c.font = Font(bold=True); c.fill = PatternFill('solid', fgColor='DDDDDD')
sis.freeze_panes = 'A2'
wb.save(out_x)
for x in resumen:
    print(*x[:6], x[6][:120], sep=' | ')
