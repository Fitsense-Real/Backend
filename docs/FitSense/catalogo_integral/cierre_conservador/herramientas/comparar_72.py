"""Comparacion plan a plan de la referencia de 72 perfiles: historica (generacion_referencia.txt, sin contencion) frente
a la conservadora (generacion_referencia_conservador_v1.txt, INC-CAT-CONS-v1 encendida, catalogo hasta V56).

Solo lee. Uso:
  python3 -I -B herramientas/comparar_72.py <raiz_repo> <carpeta_salida>
Formato de cada linea: PERFIL | DIA FOCO NNmin id:SxRr/desc id:Sx90s/desc| ... || ESTADO
"""
import csv, os, re, sys, collections

raiz, salida = sys.argv[1], sys.argv[2]
prog = os.path.join(raiz, 'docs/FitSense/evidencia/progresion')
ANTES = os.path.join(prog, 'generacion_referencia.txt')
DESPUES = os.path.join(prog, 'generacion_referencia_conservador_v1.txt')
cons = open(os.path.join(raiz, 'src/main/java/main/web/services/fitsense/planning/domain/services/ContencionConservadora.java')).read()

def ids_de(nombre):
    m = re.search(nombre + r'\s*=\s*Set\.of\(([^;]*)\);', cons, re.S)
    return {int(x) for x in re.findall(r'(\d+)L', m.group(1))}

IDENT, APOYO, DIFIC = ids_de('IDENTIDAD_NO_DETERMINADA'), ids_de('APOYO_SUSPENDIDO'), ids_de('DIFICULTAD_EN_DUDA')
catalogo = {int(r['id']): r for r in csv.DictReader(open(os.path.join(
    raiz, 'docs/FitSense/catalogo_integral/cierre_conservador/catalogo_clasificado.tsv')), delimiter='\t')}
CASA = {'body weight', 'dumbbell', 'band'}

def contenidos(perfil):
    nivel = perfil.split('/')[1]
    return IDENT | APOYO | (DIFIC if nivel == 'BEGINNER' else set())

def compatible_casa(eid):
    eq = catalogo[eid]['equipo']
    if eq in ('', '-'):
        return True
    grupos = collections.defaultdict(set)
    for parte in eq.split(','):
        g, codigo = parte.split(':', 1)
        grupos[g].add(codigo)
    return all(c & CASA for c in grupos.values())

def leer(ruta):
    planes = {}
    for linea in open(ruta, encoding='utf-8'):
        linea = linea.rstrip('\n')
        if not linea:
            continue
        cuerpo, estado = linea.rsplit('||', 1)
        partes = [p.strip() for p in cuerpo.split('|')]
        perfil, sesiones = partes[0], []
        for s in partes[1:]:
            if not s:
                continue
            tok = s.split()
            dia, foco, minutos = tok[0], tok[1], int(tok[2].rstrip('min'))
            ejercicios = []
            for t in tok[3:]:
                eid, dosis = t.split(':', 1)
                m = re.match(r'(\d+)x(\d+)([rs])/(\d+)', dosis)
                series, valor, tipo, desc = int(m.group(1)), int(m.group(2)), m.group(3), int(m.group(4))
                ejercicios.append((int(eid), dosis, series, valor if tipo == 'r' else 0, valor if tipo == 's' else 0))
            sesiones.append((dia, foco, minutos, ejercicios))
        planes[perfil] = {'estado': estado.strip(), 'sesiones': sesiones, 'linea': linea}
    return planes

antes, despues = leer(ANTES), leer(DESPUES)
assert list(antes) == list(despues), 'los 72 perfiles deben coincidir y en el mismo orden'

def resumen(p):
    ej = [e for s in p['sesiones'] for e in s[3]]
    return {
        'dias': ' '.join(s[0][:3] for s in p['sesiones']),
        'minutos': ' '.join(str(s[2]) for s in p['sesiones']),
        'n_ejercicios': len(ej),
        'ids': collections.Counter(e[0] for e in ej),
        'series': sum(e[2] for e in ej),
        'reps': sum(e[2] * e[3] for e in ej),
        'segundos': sum(e[2] * e[4] for e in ej),
        'cardio': sum(1 for e in ej if catalogo[e[0]]['patron'] == 'LOCOMOTION_CARDIO'),
        'dosis': collections.Counter(e[1] for e in ej),
        'por_id_dosis': {e[0]: e[1] for e in ej},
    }

# Patron de dosis de la politica por objetivo y nivel: lo que la referencia historica usa en cualquier perfil del mismo
# objetivo y nivel (las dosis salen de la clase del ejercicio, el objetivo y el nivel, no del ejercicio concreto).
dosis_politica = collections.defaultdict(set)
for perfil, p in antes.items():
    objetivo, nivel = perfil.split('/')[:2]
    for s in p['sesiones']:
        for e in s[3]:
            dosis_politica[(objetivo, nivel)].add(e[1])

filas, causas = [], collections.Counter()
for perfil in antes:
    a, d = resumen(antes[perfil]), resumen(despues[perfil])
    cont = contenidos(perfil)
    cont_antes = sorted(i for i in a['ids'] if i in cont)
    cont_despues = sorted(i for i in d['ids'] if i in cont)
    retirados = sorted(set(a['ids']) - set(d['ids']))
    anadidos = sorted(set(d['ids']) - set(a['ids']))
    dosis_nuevas = sorted(set(d['dosis']) - dosis_politica[tuple(perfil.split('/')[:2])])
    misma_dosis_cambiada = sorted(i for i in set(a['por_id_dosis']) & set(d['por_id_dosis'])
                                  if a['por_id_dosis'][i] != d['por_id_dosis'][i])
    casa = perfil.endswith('/CASA')
    incompatibles = sorted(i for i in d['ids'] if casa and not compatible_casa(i))
    igual = antes[perfil]['linea'] == despues[perfil]['linea']
    if igual:
        causa = 'SIN_CAMBIO'
    elif cont_antes:
        causa = 'CONTENCION'
    elif a['dias'] != d['dias'] or a['minutos'] != d['minutos'] and not anadidos:
        causa = 'INESPERADA'
    else:
        causa = 'SELECCION_DETERMINISTA'
    # Cambios inesperados: plan no valido, contenido presente, equipamiento incompatible o dosis fuera del patron
    # de dosis del propio perfil (V24 lo rechazaria; aqui se comprueba de forma independiente).
    inesperado = []
    if despues[perfil]['estado'] != 'VALIDA':
        inesperado.append('plan no valido')
    if cont_despues:
        inesperado.append('contenido presente')
    if incompatibles:
        inesperado.append('equipamiento incompatible')
    if dosis_nuevas:
        inesperado.append('dosis que la referencia historica no usa para este objetivo y nivel: ' + ','.join(dosis_nuevas))
    if inesperado:
        causa = 'INESPERADA'
    causas[causa] += 1
    if causa == 'CONTENCION':
        explicacion = ('El plan historico usaba ' + ', '.join(map(str, cont_antes)) + ', contenido(s) por INC-CAT-CONS-v1; '
                       'al faltar, el motor elige otro candidato y la secuencia aleatoria con semilla se desplaza.')
    elif causa == 'SELECCION_DETERMINISTA':
        explicacion = ('Ningun contenido en el plan historico. Cambia el conjunto de candidatos (p. ej. 1311, '
                       'elegible para todos sin la contencion): nextInt(n)/shuffle sobre listas mas cortas devuelve otro '
                       'ejercicio con la misma semilla y desplaza las elecciones siguientes.')
    elif causa == 'SIN_CAMBIO':
        explicacion = 'Identico.'
    else:
        explicacion = '; '.join(inesperado) or 'estructura distinta sin causa de catalogo'
    filas.append({
        'perfil': perfil,
        '1_validacion_antes': antes[perfil]['estado'], '2_validacion_despues': despues[perfil]['estado'],
        '3_dias_antes': a['dias'], '4_dias_despues': d['dias'],
        '5_minutos_antes': a['minutos'], '6_minutos_despues': d['minutos'],
        '7_n_ejercicios_antes': a['n_ejercicios'], '8_n_ejercicios_despues': d['n_ejercicios'],
        '9_retirados': ' '.join(map(str, retirados)), '10_anadidos': ' '.join(map(str, anadidos)),
        '11_contenidos_en_historico': ' '.join(map(str, cont_antes)),
        '12_contenidos_en_conservador': ' '.join(map(str, cont_despues)) or 'ninguno',
        '13_series_antes_despues': f"{a['series']} -> {d['series']}",
        '14_reps_antes_despues': f"{a['reps']} -> {d['reps']}",
        '15_segundos_antes_despues': f"{a['segundos']} -> {d['segundos']}",
        'cardio_antes_despues': f"{a['cardio']} -> {d['cardio']}",
        'dosis_fuera_del_patron_objetivo_nivel': ' '.join(dosis_nuevas) or 'ninguna',
        'mismo_ejercicio_otra_dosis': ' '.join(map(str, misma_dosis_cambiada)) or 'ninguno',
        'equipamiento_incompatible': ' '.join(map(str, incompatibles)) or 'ninguno',
        'causa': causa, 'explicacion': explicacion,
    })

os.makedirs(salida, exist_ok=True)
with open(os.path.join(salida, 'comparacion_72.tsv'), 'w', newline='', encoding='utf-8') as f:
    w = csv.DictWriter(f, fieldnames=list(filas[0]), delimiter='\t')
    w.writeheader()
    w.writerows(filas)
uso_a = collections.Counter(i for p in antes.values() for s in p['sesiones'] for i in [e[0] for e in s[3]])
uso_d = collections.Counter(i for p in despues.values() for s in p['sesiones'] for i in [e[0] for e in s[3]])
print(dict(causas), 'contenidos en conservador:', sum(1 for r in filas if r['12_contenidos_en_conservador'] != 'ninguno'),
      'planes con contenido en historico:', sum(1 for r in filas if r['11_contenidos_en_historico']))

from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment
from openpyxl.utils import get_column_letter
wb = Workbook()
ws0 = wb.active
ws0.title = 'LEEME'
for linea in [
    'COMPARACION DE LOS 72 PLANES DE REFERENCIA - FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1',
    'Antes: generacion_referencia.txt (historica, conservada intacta; se reproduce con la contencion apagada).',
    'Despues: generacion_referencia_conservador_v1.txt (contencion INC-CAT-CONS-v1 encendida, catalogo hasta V56).',
    'Mismos 72 perfiles (4 objetivos x 3 niveles x 30/45/60 min x gimnasio/casa), misma semana fija (semilla) y politica PLAN-HIST-1.',
    'Columnas 1-15: dimensiones comparadas. causa: CONTENCION, SELECCION_DETERMINISTA, SIN_CAMBIO o INESPERADA.',
    'Resumen: ' + ', '.join(f'{k}={v}' for k, v in sorted(causas.items())),
    'Comprobaciones independientes del validador: ningun contenido en el plan conservador, equipamiento de casa compatible,',
    'ninguna dosis fuera del patron de dosis que la referencia historica usa para el mismo objetivo y nivel.',
    'Ninguna de estas comprobaciones es validacion profesional.',
]:
    ws0.append([linea])
ws0.column_dimensions['A'].width = 140
ws = wb.create_sheet('Comparacion')
ws.append(list(filas[0]))
for r in filas:
    ws.append(list(r.values()))
for c in ws[1]:
    c.font = Font(bold=True)
color = {'CONTENCION': 'FFF2CC', 'SELECCION_DETERMINISTA': 'DDEBF7', 'SIN_CAMBIO': 'E2EFDA', 'INESPERADA': 'F8CBAD'}
k_causa = list(filas[0]).index('causa') + 1
for fila in ws.iter_rows(min_row=2):
    fila[k_causa - 1].fill = PatternFill('solid', fgColor=color[fila[k_causa - 1].value])
for j, k in enumerate(filas[0], 1):
    ws.column_dimensions[get_column_letter(j)].width = 80 if k == 'explicacion' else (34 if k == 'perfil' else 16)
ws.freeze_panes = 'B2'
ws.auto_filter.ref = ws.dimensions
wu = wb.create_sheet('Uso_por_ejercicio')
wu.append(['exercise_id', 'nombre', 'clasificacion', 'apariciones_historica', 'apariciones_conservadora', 'diferencia'])
for i in sorted(set(uso_a) | set(uso_d), key=lambda i: (uso_d[i] - uso_a[i], i)):
    wu.append([i, catalogo[i]['nombre_es'], catalogo[i]['estado'] + (('/' + catalogo[i]['subtipo']) if catalogo[i]['subtipo'] else ''),
               uso_a[i], uso_d[i], uso_d[i] - uso_a[i]])
for c in wu[1]:
    c.font = Font(bold=True)
wu.column_dimensions['B'].width = 50
wl = wb.create_sheet('Lineas')
wl.append(['perfil', 'historica', 'conservadora'])
for p in antes:
    wl.append([p, antes[p]['linea'], despues[p]['linea']])
wb.save(os.path.join(salida, 'COMPARACION_72_PLANES.xlsx'))
