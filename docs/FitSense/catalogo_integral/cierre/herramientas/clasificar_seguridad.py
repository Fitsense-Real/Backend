"""Clasificacion de las decisiones de seguridad pendientes (orden de 14/11/2026, fase 3).

Lee el registro y el expediente vigentes; no cambia ninguno. Uso:
  python3 -I -B docs/FitSense/catalogo_integral/cierre/herramientas/clasificar_seguridad.py <cierre> <salida.tsv>
Categorias (una principal por decision; 'profesional' se indica aparte porque casi todas lo necesitan):
  ERROR_TECNICO      el GIF o los datos demuestran un error y ya se corrigio (V52.1); no queda decision de seguridad
  CONTENCION_APLICADA el valor vigente es el conservador (bandera, retencion o contencion) y protege a quien restringe
  PROPIETARIO        decide el propietario (significado de un permiso)
  INCERTIDUMBRE      ejercicio ELEGIBLE para quien declara la restriccion pertinente, sin contencion: riesgo material
"""
import csv, sys, os

cierre, salida = sys.argv[1], sys.argv[2]
reg = list(csv.DictReader(open(os.path.join(cierre, 'REGISTRO_DECISIONES_PENDIENTES.tsv')), delimiter='\t'))
exp = {x['clave']: x for x in csv.DictReader(open(os.path.join(cierre, 'pendientes', 'EXPEDIENTE_SEGURIDAD.tsv')), delimiter='\t')}
APOYO_ELEGIBLES = {'457', '458', '459', '462', '777', '920', '934', '1184', '1317', '1335'}

def categoria(clave, ident, r, e):
    if clave.startswith('X-AP-') and ident in APOYO_ELEGIBLES:
        return 'INCERTIDUMBRE', 'Elegible para «no colgarse» mientras no se decida A-V53 (apoyo suspendido sin contencion)'
    if clave == 'X-1113':
        return 'INCERTIDUMBRE', 'Dificultad 1 vigente (V39); R2 y V3 ven nivel 2; elegible para principiantes'
    if clave == 'X-1323':
        return 'ERROR_TECNICO', 'Texto corregido en V52.1 (los «rodillos» no estan en el GIF)'
    if clave == 'A-V53' or (r and r['quien_decide'].startswith('PROPIETARIO')):
        return 'PROPIETARIO', 'Significado de «no colgarse» (A-V53); contenido mientras tanto' if clave != 'A-V53' else 'Opcion A/B de V53; interruptor apagado'
    cont = (e or {}).get('contencion', '') + ' ' + ((r or {}).get('estado_final', ''))
    if 'RETENIDO' in cont or 'CONTENIDO' in cont or 'Contenido' in cont:
        return 'CONTENCION_APLICADA', (e or {}).get('contencion', '') or (r or {}).get('estado_final', '')
    return 'REVISAR', ''

claves = [x['clave'] for x in reg if x['prioridad'] == '1'] + [k for k in exp if k.startswith('X-')]
porclave = {x['clave']: x for x in reg}
filas = []
for k in dict.fromkeys(claves):
    r, e = porclave.get(k), exp.get(k)
    ident = (r or e)['id']
    cat, motivo = categoria(k, ident, r, e)
    prof = 'SI' if ((r and 'PROFESIONAL' in r['quien_decide']) or k.startswith('X-')) else 'NO'
    filas.append({'clave': k, 'id': ident, 'bloque': (r or {}).get('bloque', 'SEGURIDAD'),
                  'categoria': cat, 'necesita_profesional': prof,
                  'banderas_vigentes': (e or {}).get('banderas_vigentes', ''),
                  'elegible_rejilla_168': (e or {}).get('elegible_en_rejilla_168', ''),
                  'motivo': motivo, 'validacion_profesional': 'NINGUNA'})
with open(salida, 'w', newline='') as f:
    w = csv.DictWriter(f, fieldnames=list(filas[0].keys()), delimiter='\t')
    w.writeheader(); w.writerows(filas)
from collections import Counter
print(len(filas), Counter(x['categoria'] for x in filas))
