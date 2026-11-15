"""Impacto de la contencion INC-CAT-CONS-v1 en la rejilla de 168 perfiles (CatalogoIntegralComparacionIT, opt-in).

Solo lee las dos salidas de la rejilla (contencion encendida y apagada, mismo commit) y las listas del codigo. Uso:
  python3 -I -B herramientas/impacto_rejilla.py <raiz_repo> <rejilla_encendida.csv> <rejilla_apagada.csv> <salida.tsv>
Cada fila: perfil, desenlace con y sin contencion, clasificacion (PLAN_VALIDO / NO_FACTIBLE_LEGITIMO / ERROR_TECNICO /
VIOLACION_SEGURIDAD / REGRESION_INESPERADA) y los contenidos que el plan sin contencion usaba.
"""
import csv, re, sys, collections

raiz, on_csv, off_csv, salida = sys.argv[1:5]
cons = open(raiz + '/src/main/java/main/web/services/fitsense/planning/domain/services/ContencionConservadora.java').read()

def ids_de(nombre):
    m = re.search(nombre + r'\s*=\s*Set\.of\(([^;]*)\);', cons, re.S)
    return {int(x) for x in re.findall(r'(\d+)L', m.group(1))}

IDENT, APOYO, DIFIC = ids_de('IDENTIDAD_NO_DETERMINADA'), ids_de('APOYO_SUSPENDIDO'), ids_de('DIFICULTAD_EN_DUDA')

def leer(f):
    return {r['clave']: r for r in csv.DictReader(open(f), delimiter=';')}

def ejercicios(r):
    return [int(x.split(':')[2]) for x in r['ejercicios'].split()] if r['ejercicios'] else []

on, off = leer(on_csv), leer(off_csv)
assert set(on) == set(off) and len(on) == 168
filas, cuenta = [], collections.Counter()
especiales = collections.Counter()
for k in sorted(on):
    objetivo, nivel, lugar, restriccion = k.split('/')
    contenidos = IDENT | APOYO | (DIFIC if nivel == 'BEGINNER' else set())
    e_on, e_off = ejercicios(on[k]), ejercicios(off[k])
    elig_on = {int(x) for x in on[k]['elegibles'].split(',') if x}
    usados_off = sorted(set(e_off) & contenidos)
    presentes_on = sorted((set(e_on) | elig_on) & contenidos)
    d_on, d_off = on[k]['desenlace'], off[k]['desenlace']
    if presentes_on:
        clase = 'VIOLACION_SEGURIDAD'
    elif d_on == 'PLAN':
        clase = 'PLAN_VALIDO'
    elif d_on.startswith('SIN_PLAN') and d_off == 'PLAN':
        # Perdida de cobertura causada por la contencion: legitima solo si el motivo es de factibilidad del catalogo.
        clase = 'NO_FACTIBLE_LEGITIMO' if 'PlanGenerationFailed' in d_on else 'REGRESION_INESPERADA'
    elif d_on.startswith('SIN_PLAN'):
        clase = 'NO_FACTIBLE_LEGITIMO' if 'PlanGenerationFailed' in d_on else 'ERROR_TECNICO'
    else:
        clase = 'ERROR_TECNICO'
    cuenta[clase] += 1
    if set(e_off) & APOYO:
        especiales['apoyo_suspendido_en_plan_sin_contencion'] += 1
    if nivel == 'BEGINNER' and set(e_off) & DIFIC:
        especiales['1113_principiante_en_plan_sin_contencion'] += 1
    if set(e_off) & IDENT:
        especiales['identidad_en_plan_sin_contencion'] += 1
    if restriccion == 'SIN_TODO':
        especiales['SIN_TODO_' + d_on.split(':')[0]] += 1
    filas.append({'perfil': k, 'desenlace_con_contencion': d_on, 'desenlace_sin_contencion': d_off,
                  'clasificacion': clase, 'n_elegibles_con': len(elig_on),
                  'n_elegibles_sin': len([x for x in off[k]['elegibles'].split(',') if x]),
                  'plan_igual': 'SI' if on[k]['ejercicios'] == off[k]['ejercicios'] else 'NO',
                  'contenidos_usados_sin_contencion': ' '.join(map(str, usados_off)),
                  'contenidos_en_plan_o_elegibles_con_contencion': ' '.join(map(str, presentes_on)) or 'ninguno'})
with open(salida, 'w', newline='') as f:
    w = csv.DictWriter(f, fieldnames=list(filas[0]), delimiter='\t')
    w.writeheader()
    w.writerows(filas)
print('clasificacion', dict(cuenta))
print('PLAN con/sin', sum(1 for r in on.values() if r['desenlace'] == 'PLAN'), sum(1 for r in off.values() if r['desenlace'] == 'PLAN'))
print('plan distinto', sum(1 for r in filas if r['plan_igual'] == 'NO'), 'elegibles distintos',
      sum(1 for k in on if on[k]['elegibles'] != off[k]['elegibles']))
print('especiales', dict(especiales))
