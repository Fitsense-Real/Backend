"""Lista priorizada de revision humana del catalogo integral (solo lectura).

Entrada: el registro maestro (docs/FitSense/catalogo_integral/registro_maestro.tsv), el detalle de textos de V49
(correcciones_texto_detalle.tsv) y la rejilla final de 168 perfiles (evidencia/cmp_despues.csv) para la exposicion.
Salida: revision_humana_priorizada.tsv y un resumen por consola. Uso:
    python3 -I -B lista_revision_humana.py <dir catalogo_integral> <dir salida>
"""
import csv, re, sys, collections

csv.field_size_limit(10**8)
src, out = sys.argv[1], sys.argv[2]
R = {r['ID']: r for r in csv.DictReader(open(f'{src}/registro_maestro.tsv', encoding='utf-8'), delimiter='\t')}
T = {r['id']: r for r in csv.DictReader(open(f'{src}/correcciones_texto_detalle.tsv', encoding='utf-8'), delimiter='\t')}

# Exposicion: en cuantos de los 168 perfiles de la rejilla final aparece el ejercicio en la semana.
expo = collections.Counter()
for r in csv.DictReader(open(f'{src}/evidencia/cmp_despues.csv', encoding='utf-8'), delimiter=';'):
    ids = {tok.split(':')[2] for tok in r['ejercicios'].split() if tok.count(':') >= 3}
    expo.update(ids)

FLAGS = {'HI', 'FL', 'AX', 'KN', 'HG', 'OH', 'INV'}
RIESGO_TXT = re.compile(r'salt|explosiv|colg|suspend|invert|cabeza abajo|por encima de la cabeza|sobre la cabeza|'
                        r'peso muerto|anillas|barra (fija|de dominadas)|impulso|balanceo|kipping|pino', re.I)

def banderas(r, col):
    v = (r.get(col) or '-').strip()
    return set() if v in ('', '-') else set(v.split())

filas = []
for i, r in R.items():
    if r['Activo'] != 'Sí':
        continue
    fin, duda = banderas(r, 'Banderas (final)'), banderas(r, 'Banderas en duda')
    apoyo = r['Apoyo suspendido'] not in ('NO', '', '-')
    v49 = i in T
    texto = (T[i]['instrucciones_despues'] if v49 else '') + ' ' + r['Nombre (final)']
    e = expo.get(i, 0)
    motivos, score = [], 0
    if v49:
        score += 1; motivos.append('texto reescrito por V49')
        if T[i]['nombre_antes'] != T[i]['nombre_despues']:
            score += 1; motivos.append('nombre cambiado')
        if RIESGO_TXT.search(texto):
            score += 2; motivos.append('el texto nuevo describe una accion de riesgo')
    if fin & FLAGS:
        score += 2; motivos.append('banderas: ' + ' '.join(sorted(fin & FLAGS)))
    if duda:
        score += 2; motivos.append('banderas en duda: ' + ' '.join(sorted(duda)))
    if apoyo:
        score += 3; motivos.append('apoyo suspendido: ' + r['Apoyo suspendido'])
    if r['Gravedad'] == 'ALTA':
        score += 2; motivos.append('gravedad ALTA')
    elif r['Gravedad'] == 'MEDIA':
        score += 1
    if e >= 10:
        score += 2; motivos.append(f'exposicion alta ({e} de 168 perfiles)')
    elif e > 0:
        score += 1; motivos.append(f'exposicion ({e} de 168 perfiles)')
    estado = r['Estado final']
    if estado == 'PENDIENTE_VISUAL':
        grupo = ('1-SUSPENSION' if ('HG' in fin | duda or apoyo) else '2-INVERSION' if 'INV' in fin | duda
                 else '3-IMPACTO' if 'HI' in fin | duda else '4-OTROS')
        lista = 'B-PENDIENTE_VISUAL'
    elif (duda - fin) or apoyo or 'de m' in r['Motivo del estado'].lower():
        # Solo lo que sigue abierto: una bandera en duda que NO esta marcada (riesgo sin mitigar), apoyo suspendido
        # sin clase, o una bandera de mas que no se relajo (factibilidad, no seguridad). Una duda sobre una bandera
        # ya marcada esta resuelta del lado conservador y no entra.
        grupo = ('1-APOYO_SUSPENDIDO' if apoyo else '2-DUDA_SIN_MARCAR' if (duda - fin) else '3-BANDERA_DE_MAS')
        lista = 'C-BANDERAS_PENDIENTES'
        if duda - fin:
            motivos.append('duda sin marcar: ' + ' '.join(sorted(duda - fin)))
    elif v49:
        grupo, lista = '', 'A-TEXTO_V49'
    else:
        continue
    if v49 and lista != 'A-TEXTO_V49':
        motivos.append('(tambien en la lista A)')
    filas.append({'lista': lista, 'grupo': grupo, 'prioridad': score, 'id': i, 'nombre_final': r['Nombre (final)'],
                  'estado_final': estado, 'banderas_final': ' '.join(sorted(fin)) or '-',
                  'banderas_en_duda': ' '.join(sorted(duda)) or '-', 'apoyo_suspendido': r['Apoyo suspendido'],
                  'gravedad': r['Gravedad'], 'exposicion_168': e, 'motivos': '; '.join(motivos),
                  'motivo_estado': r['Motivo del estado'], 'que_revisar': ''})

def nivel(p):
    return 'P1' if p >= 7 else 'P2' if p >= 4 else 'P3'

for f in filas:
    f['nivel'] = nivel(f['prioridad'])
    f['que_revisar'] = {
        'A-TEXTO_V49': 'Comparar instrucciones nuevas con el GIF; confirmar que no falta ninguna advertencia',
        'B-PENDIENTE_VISUAL': 'Decidir el movimiento real (otra fuente visual o profesional); mientras, texto y banderas anteriores',
        'C-BANDERAS_PENDIENTES': 'Decidir si la bandera en duda aplica (o si sobra una existente); no relajar sin decision',
    }[f['lista']]
filas.sort(key=lambda f: (f['lista'], f['grupo'], -f['prioridad'], -f['exposicion_168'], int(f['id'])))
cols = ['lista', 'grupo', 'nivel', 'prioridad', 'id', 'nombre_final', 'estado_final', 'banderas_final', 'banderas_en_duda',
        'apoyo_suspendido', 'gravedad', 'exposicion_168', 'motivos', 'motivo_estado', 'que_revisar']
with open(f'{out}/revision_humana_priorizada.tsv', 'w', encoding='utf-8', newline='') as fh:
    w = csv.DictWriter(fh, fieldnames=cols, delimiter='\t')
    w.writeheader()
    w.writerows(filas)
c = collections.Counter((f['lista'], f['nivel']) for f in filas)
for k in sorted(c):
    print(k, c[k])
print('grupos', collections.Counter((f['lista'], f['grupo']) for f in filas if f['grupo']))
