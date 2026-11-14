"""Fichas para la revision profesional (grupo B de la orden de cierre de pendientes, 14/11/2026).

Una ficha por cada decision del registro que sigue pendiente de un profesional (estado final «Contenido
preventivamente» o «Bloqueado por validacion especializada»). No cambia ningun estado ni reduce el numero de
pendientes: solo prepara la pregunta. Los campos de respuesta quedan VACIOS.

Columnas: GIF (ruta en el conjunto de datos) y orden para generar la hoja de fotogramas completa, nombre e instrucciones
vigentes, clasificacion vigente, alternativa propuesta, diferencia, evidencia (R2 y V3), riesgo de mantener el valor
vigente, impacto (uso real), pregunta concreta y respuesta.

Uso (desde la raiz del repositorio):
  python3 -I -B docs/FitSense/catalogo_integral/cierre/herramientas/fichas_profesionales.py
"""
import csv
from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.utils import get_column_letter

BASE = 'docs/FitSense/catalogo_integral/cierre/'
HUELLA = 'src/test/resources/catalogo/huella_catalogo.tsv'


def rd(p):
    with open(p, newline='', encoding='utf-8') as f:
        return list(csv.DictReader(f, delimiter='\t'))


REG = rd(BASE + 'REGISTRO_DECISIONES_PENDIENTES.tsv')
CAT = {r['id']: r for r in rd(BASE + 'linea_base/catalogo_pre_cierre_V50.tsv')}
MAESTRO = {r['id']: r for r in rd(BASE + 'registro_maestro_final.tsv')}
H = {r['id']: r for r in rd(HUELLA)}
R2 = {r['id']: r for r in rd(BASE + 'adjudicacion/revision2_consolidada.tsv')}
# Textos vigentes: V50 con las reescrituras de V52 (V52.1 solo toca ejercicios que ya no estan pendientes).
V52 = {}
for r in rd(BASE + 'adjudicacion/decisiones_v52.tsv'):
    if r['atributo'] in ('instructions_es', 'name_es'):
        V52[(r['id'], r['atributo'])] = r['despues']
V3 = {}
for r in rd(BASE + 'adjudicacion/verificacion3_consolidada.tsv'):
    V3.setdefault(r['id'], []).append(r)

PENDIENTE = ('Contenido preventivamente', 'Bloqueado por validacion especializada')
HOJA = ('python3 -I -B docs/FitSense/catalogo_integral/cierre/herramientas/hojas_completas.py '
        'docs/FitSense/catalogo_integral/cierre/linea_base/catalogo_pre_cierre_V50.tsv <conjunto_de_datos> <salida> %s')
BANDERA = {'HI': 'impacto', 'FL': 'suelo', 'AX': 'carga axial', 'KN': 'rodilla apoyada', 'HG': 'colgarse',
           'OH': 'brazos sobre la cabeza', 'INV': 'inversion'}


def evidencia(i):
    partes = []
    r2 = R2.get(i)
    if r2:
        partes.append('R2: ' + r2['movimiento_observado'] + (' (fotogramas: %s)' % r2['fotogramas_clave'] if r2['fotogramas_clave'] else '')
                      + (' Dificultad R2: %s.' % r2['dificultad'] if r2.get('dificultad') else ''))
    for v in V3.get(i, []):
        partes.append('V3 %s %s: %s' % (v['tipo'], v['decision'], v['evidencia']))
    return ' / '.join(partes) or 'sin revision registrada'


def ficha(r):
    i, origen, bloque = r['id'], r['origen'], r['bloque']
    h = H.get(i, {})
    dif = h.get('dificultad', '')
    if bloque == 'DIFICULTAD' and origen.startswith('DIFICULTAD'):
        alt = 'dificultad=%d' % (int(dif) - 1)
        diferencia = 'Un nivel menos: el ejercicio pasaria a ser elegible para perfiles de nivel %d' % (int(dif) - 1)
        riesgo = 'Ninguno de seguridad (el nivel vigente es el mas alto y llega a menos perfiles). Coste: menos variedad para el nivel inferior'
        pregunta = '¿El ejercicio que muestra el GIF es apropiado para un participante de nivel %d? Si: bajar a %d. No: mantener %s' % (
            int(dif) - 1, int(dif) - 1, dif)
    elif origen == 'DECISION PREVIA V39':
        alt = 'dificultad=2'
        diferencia = 'Un nivel mas: dejaria de ser elegible para principiantes'
        riesgo = 'Material: principiantes pueden recibir un ejercicio que R2 y V3 sitúan en nivel 2. La decision historica V39 lo fijo en 1'
        pregunta = '¿El ejercicio del GIF es apropiado para un principiante? Si: mantener 1 (V39). No: subir a 2'
    elif origen.startswith('CONTENCION INC-4C-03'):
        alt = 'liberar el ejercicio con sus banderas vigentes (%s)' % h.get('banderas', '')
        diferencia = 'Volveria a prescribirse a los perfiles que sus banderas y su dificultad permiten'
        riesgo = 'Ninguno de seguridad (no se prescribe a nadie). Coste: el ejercicio no existe para el motor'
        pregunta = '¿Es seguro prescribir este ejercicio, con las banderas %s y dificultad %s, a quien no tenga esas restricciones? Si: liberar. No: mantener retenido o desactivar' % (h.get('banderas', ''), dif)
    elif origen == 'CONTENCION INC-CAT-C39':
        alt = 'liberar la contencion (no cuenta como brazos sobre la cabeza)'
        diferencia = 'Volveria a ser elegible para quien restringe los brazos sobre la cabeza'
        riesgo = 'Ninguno de seguridad (contenido para quien lo restringe). Coste: menos opciones de biceps para esos perfiles'
        pregunta = '¿El curl con el brazo a la altura del hombro debe contar como «brazos sobre la cabeza»? Si: mantener contenido. No: liberar'
    elif origen == 'DEC-APOYO-SUSPENDIDO (V53)' or r['clave'] in ('A-V53', 'A-1301'):
        alt = 'excluirlo para quien declara «no colgarse» (V53 encendido)'
        diferencia = 'Saldria de los elegibles de los perfiles «no colgarse»'
        riesgo = ('Material para los 10 de V53 sin contencion: quien declaro «no colgarse» puede recibir fondos en paralelas o silla del capitan'
                  if r['vigente_conservador'] == 'NO' else 'Ninguno: contenido por INC-CAT-C39 para «no colgarse»')
        pregunta = '¿Debe «no colgarse» excluir el apoyo suspendido sobre un aparato sin soporte de pies (V53_DEFINITIVO.md)?'
    elif origen in ('DEC-HI-LANZAMIENTOS', 'DEC-HI-CARRERA', 'DECISION PREVIA D2 (V37.1)', 'BANDERA_QUITAR'):
        alt = 'quitar HI (impacto)'
        diferencia = 'Volveria a ser elegible para quien restringe el impacto'
        riesgo = 'Ninguno de seguridad (el vigente es el conservador). Coste: menos opciones para quien restringe el impacto'
        pregunta = '¿El movimiento del GIF es de impacto (vuelo, aterrizaje o choque repetido)? Si: mantener HI. No: quitar HI'
    elif origen == 'DEC-INV-DECLINADO':
        alt = 'quitar INV (inversion)'
        diferencia = 'Volveria a ser elegible para mayores de 65'
        riesgo = 'Ninguno de seguridad (el vigente es el conservador). Coste: menos opciones de press declinado para mayores de 65'
        pregunta = '¿El banco declinado de este GIF supone inversion (cabeza por debajo de la cadera) a efectos de seguridad? Si: mantener INV. No: quitarlo'
    elif origen == 'BANDERA_ANADIR':
        alt = 'INV ya anadido por V52.1 (conservador)'
        diferencia = 'Confirmar o retirar la bandera'
        riesgo = 'Ninguno de seguridad: ya contenido por INV, HG y OH'
        pregunta = '¿El pike colgado supone inversion? Si: mantener INV. No: retirarla (seguiria excluido por HG y OH)'
    elif bloque == 'IDENTIDAD':
        alt = 'confirmar o renombrar la ficha segun el GIF'
        diferencia = 'Nombre, instrucciones y clasificacion pasarian a describir el ejercicio confirmado'
        riesgo = ('Ninguno de seguridad mientras siga retenido o contenido por sus banderas' if r['estado_final'] == PENDIENTE[0]
                  else 'Bajo: la ficha puede describir un ejercicio distinto del GIF; las banderas vigentes se mantienen')
        pregunta = '¿Que ejercicio muestra el GIF? ¿Coinciden con el el nombre y las instrucciones vigentes?'
    elif bloque == 'TEXTO':
        alt = 'reescribir las instrucciones segun el GIF'
        diferencia = 'Las instrucciones describirian el movimiento del GIF'
        riesgo = 'Contenido (1294 retenido para todos)' if i == '1294' else 'Bajo: instrucciones que el GIF no permite verificar'
        pregunta = '¿Las instrucciones vigentes describen el movimiento del GIF? Si no, ¿cual es el texto correcto?'
    else:
        alt = r['decision_pendiente']
        diferencia = '-'
        riesgo = 'Ver la decision'
        pregunta = r['decision_pendiente']
    c = CAT.get(i, {})
    m = MAESTRO.get(i, {})
    return {
        'clave': r['clave'], 'id': i, 'nombre_vigente': V52.get((i, 'name_es')) or c.get('name_es') or r['nombre'],
        'instrucciones_vigentes': V52.get((i, 'instructions_es')) or c.get('instr_es', ''),
        'gif': c.get('gif', ''), 'hoja_fotogramas': HOJA % i if i else '-',
        'bloque': bloque, 'clasificacion_vigente': r['valor_vigente'], 'alternativa_propuesta': alt,
        'diferencia': diferencia, 'evidencia': evidencia(i) if i else r['evidencia'],
        'riesgo_de_mantener_vigente': riesgo, 'impacto': r['uso_real'], 'pregunta': pregunta,
        'estado_final': r['estado_final'], 'quien_decide': r['quien_decide'],
        'decision': '', 'valor': '', 'nombre_responsable': '', 'credenciales_o_funcion': '', 'fecha': '', 'observaciones': '',
    }


FICHAS = [ficha(r) for r in REG if r['estado_final'] in PENDIENTE]
wb = Workbook()
ws = wb.active
ws.title = 'Fichas'
cols = list(FICHAS[0].keys())
ws.append(cols)
for f in FICHAS:
    ws.append([f[k] for k in cols])
for n, k in enumerate(cols, 1):
    ws.cell(1, n).font = Font(bold=True)
    ws.cell(1, n).fill = PatternFill('solid', fgColor='DDDDDD')
    ws.column_dimensions[get_column_letter(n)].width = 60 if k in ('instrucciones_vigentes', 'evidencia', 'pregunta', 'riesgo_de_mantener_vigente') else 22
for row in ws.iter_rows(min_row=2):
    for cell in row:
        cell.alignment = Alignment(wrap_text=True, vertical='top')
ws.freeze_panes = 'C2'
lee = wb.create_sheet('Leeme')
for linea in [
    'Fichas para la revision profesional: orden de cierre de pendientes (14/11/2026).',
    'Una fila por decision pendiente: %d. Ninguna tiene validacion profesional registrada.' % len(FICHAS),
    'GIF: ruta dentro del conjunto de datos del catalogo. hoja_fotogramas: orden para generar la animacion completa (f1..fN).',
    'Rellenar decision, valor, nombre_responsable, credenciales_o_funcion, fecha y observaciones. Nada se aplica sin esa respuesta.',
    'Valor vigente mientras no se decida: el conservador, salvo A-V53 y 1113 (ver riesgo_de_mantener_vigente).',
]:
    lee.append([linea])
wb.save(BASE + 'pendientes/FICHAS_PROFESIONALES.xlsx')
with open(BASE + 'pendientes/FICHAS_PROFESIONALES.tsv', 'w', newline='', encoding='utf-8') as f:
    w = csv.DictWriter(f, cols, delimiter='\t', lineterminator='\n')
    w.writeheader()
    w.writerows(FICHAS)
import collections
print('fichas', len(FICHAS), dict(collections.Counter(f['bloque'] for f in FICHAS)))
