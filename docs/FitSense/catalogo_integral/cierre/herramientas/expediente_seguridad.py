"""Expediente individual de las decisiones de SEGURIDAD (orden de cierre de pendientes, 14/11/2026, fase 3).

Una fila por decision: las 43 de prioridad 1 del registro unico, mas los casos que la orden pide revisar
expresamente y no tenian fila propia (1113, 1323, 1133, 1175, 1374 y los 10 de apoyo suspendido fuera de INC-CAT-C39).
Para cada una: movimiento (R2), evidencia visual (V3 y la revision de esta orden), nombre e instrucciones vigentes,
banderas vigentes, restriccion en riesgo, clasificacion propuesta, justificacion, impacto en elegibilidad (rejilla de
168 perfiles), si exige profesional y estado de contencion. NO registra ninguna validacion profesional.

Uso (raiz del repositorio): python3 -I -B docs/FitSense/catalogo_integral/cierre/herramientas/expediente_seguridad.py
"""
import csv, collections, os

B = 'docs/FitSense/catalogo_integral/cierre/'
csv.field_size_limit(10**8)
def rd(p, d='\t'):
    with open(p, newline='', encoding='utf-8') as f:
        return list(csv.DictReader(f, delimiter=d, quoting=csv.QUOTE_NONE if d == '\t' else csv.QUOTE_MINIMAL))

H = {r['id']: r for r in rd('src/test/resources/catalogo/huella_catalogo.tsv')}
M = {r['id']: r for r in rd(B + 'registro_maestro_final.tsv')}
R2 = {r['id']: r for r in rd(B + 'adjudicacion/revision2_consolidada.tsv')}
V3 = collections.defaultdict(list)
for r in rd(B + 'adjudicacion/verificacion3_consolidada.tsv'):
    V3[r['id']].append(f"{r['tipo']} {r['decision']}: {r['evidencia']}")
REG = rd(B + 'REGISTRO_DECISIONES_PENDIENTES.tsv')
ELEG = collections.Counter()
for r in rd(B + 'impacto/despues_V52_sin_V53.csv', ';'):
    for i in filter(None, r['elegibles'].split(',')): ELEG[i] += 1

# Texto nuevo de V52.1 (1323) para que la ficha muestre el estado final.
TEXTO_V521 = {'1323': 'Siéntate en el banco situado entre las dos poleas altas, con los pies apoyados en el suelo. Sujeta las '
              'dos asas por encima de la cabeza con los brazos extendidos. Tira de las asas hacia la parte alta del pecho '
              'llevando los codos hacia abajo. Sube las asas despacio hasta extender los brazos. Repite el número de '
              'repeticiones indicado.'}
RETENIDOS = {'813', '858', '861', '873', '885', '1192', '1294', '1298'}
C39 = {'1153', '1301', '1303', '1304', '1306', '131', '400', '448'}

# (restriccion en riesgo, clasificacion propuesta, justificacion, profesional, contencion, riesgo)
def regla(clave, i, origen):
    if i in RETENIDOS and i != '1294':
        salida = 'propuesta de liberacion' in REG_BY.get(clave, {}).get('decision_pendiente', '')
        return ('Todas (no se prescribe a nadie)',
                'Mantener RETENIDO' + (' hasta aprobar la liberacion' if salida else ' (identidad AMBIGUA)'),
                'Retencion INC-4C-03 vigente: el ejercicio no llega al generador, a la IA ni a planes nuevos; si ya esta '
                'publicado, la sesion se bloquea. ' + ('Su criterio tecnico de salida (nivel, requisitos y unidad verificados) '
                'se cumple, pero liberar un ejercicio de dificultad 3 con posiciones invertidas o sostenidas exige criterio '
                'profesional.' if salida else 'El GIF no permite confirmar el ejercicio (R2 y V3).'),
                'SI: aprobar o no la liberacion' if salida else 'SI: confirmar identidad antes de cualquier liberacion',
                'RETENIDO', 'CONTENIDO')
    if origen == 'CONTENCION INC-CAT-C39':
        return ('Brazos por encima de la cabeza (OH)', 'Mantener contencion C39; decidir si el curl con el brazo a la altura '
                'del hombro es OH', 'R2 ALTA: brazo a la altura del hombro, no por encima. Dentro del limite de la definicion; '
                'la contencion lo retira solo a quien restringe brazos arriba.', 'SI: fijar si cuenta como OH',
                'CONTENIDO (INC-CAT-C39, solo para quien restringe OH)', 'CONTENIDO')
    if origen == 'DEC-APOYO-SUSPENDIDO (V53)' or clave in ('A-V53', 'A-1301') or clave.startswith('X-AP-'):
        en15 = i in {'457', '458', '459', '462', '777', '920', '934', '1069', '1153', '1184', '1303', '1304', '1306', '1317', '1335'}
        hg = 'HG' in H.get(i, {}).get('banderas', '')
        cont = 'CONTENIDO (INC-CAT-C39) para «no colgarse»' if i in C39 else 'CONTENIDO por HG (cuelga de la barra)' if hg else (
            'SIN CONTENCION: elegible con «no colgarse» mientras V53 siga apagado' if i else 'Interruptor V53 APAGADO')
        return ('«No colgarse» (apoyo suspendido sobre aparato)',
                ('Marcado en V53 (apoyo suspendido sin soporte de pies)' if en15 else 'No marcado en V53 (manos en el suelo / pies en correas)') if i else
                'Aprobar el significado conservador de «no colgarse»',
                'Decision de SIGNIFICADO del permiso, no de dato: el dato esta verificado (V53, auditoria V1-V53 = 15). '
                'Detalle y propuesta en V53_DEFINITIVO.md.', 'SI: propietario + profesional (significado del permiso)',
                cont, 'CONTENIDO' if (i in C39 or hg or not i) else 'ABIERTO (decision de significado)')
    if origen in ('DEC-HI-LANZAMIENTOS', 'ORDEN 14/11 punto 2'):
        return ('Sin impacto (edad >= 60, principiante o declarado)', 'Mantener HI (1083-1087)',
                'Lanzamiento balistico con balon medicinal: si es pliometria es un criterio sin fijar. Con HI quedan fuera '
                'de quien no admite impacto: es el valor conservador. 1083 ademas tiene CHECK «ALTO implica HI».',
                'SI: fijar si el lanzamiento balistico es pliometria (solo podria relajar)', 'CONTENIDO por la bandera HI', 'CONTENIDO')
    if origen == 'DEC-HI-CARRERA' or i in ('1133', '1175'):
        return ('Sin impacto', 'Mantener HI en 1133, 1134 y 1175', 'Carrera por debajo del sprint: HI o solo MODERADO es un '
                'criterio sin fijar; las tres conservan HI y no se relaja una sola.', 'SI: criterio comun para carrera (solo podria relajar)',
                'CONTENIDO por la bandera HI', 'CONTENIDO')
    if origen == 'DEC-INV-DECLINADO':
        return ('Sin inversion (edad >= 65)', 'Mantener INV', 'Banco declinado con el peso sobre la espalda: el catalogo es '
                'desigual (540, 566, 1224 y 1241 de referencia). Unificar es cambiar el significado de «sin inversion».',
                'SI: criterio de banco declinado (solo podria relajar)', 'CONTENIDO por la bandera INV', 'CONTENIDO')
    if origen == 'DECISION PREVIA D2 (V37.1)':
        return ('Sin impacto', 'Mantener HI (decision historica D2)', 'R2 y V3 ven el pie de apoyo siempre en el suelo, pero D2 '
                'exige una segunda revision HUMANA antes de quitar HI. La revision de IA no la sustituye.',
                'SI: la segunda revision humana que exige D2', 'CONTENIDO por la bandera HI', 'CONTENIDO')
    if i == '868':
        return ('Sin inversion (edad >= 65)', 'ANADIR INV (aplicado en V52.1)', 'f5-f8: las piernas pasan por encima de la barra y '
                'la cadera queda por encima de la cabeza. Anadir INV solo restringe; HG y OH ya lo excluian de «no colgarse».',
                'Opcional: solo podria quitar INV (relajar)', 'CONTENIDO por INV (V52.1) + HG + OH', 'CERRADO (conservador)')
    if i == '1177':
        return ('Sin impacto', 'Mantener HI e impacto ALTO', 'Fundido entre poses: el GIF no demuestra que nunca haya vuelo; '
                'verificadores en desacuerdo (V07 NO_DECIDIBLE, X01 BAJO). Identidad (paso o salto) no confirmable.',
                'SI: confirmar si hay salto (solo podria relajar)', 'CONTENIDO por la bandera HI', 'CONTENIDO')
    if i in ('922', '930', '971'):
        return ('Sin impacto y sin carga axial', 'Mantener HI y AX', 'Derivados olimpicos (clean) por nombre y fuente: ALTO por '
                'definicion. El GIF es un fundido y no distingue un clean de un curl hasta el rack.',
                'SI: confirmar identidad (clean o curl); solo podria relajar', 'CONTENIDO por HI y AX; dificultad 3', 'CONTENIDO')
    if i in ('937', '1068'):
        return ('No colgarse, brazos arriba, sin impacto', 'Mantener HI, HG y OH', 'El GIF solo muestra el balanceo o la '
                'traccion; la transicion del muscle-up no aparece. Con HI, HG y OH y dificultad 3 queda fuera de cualquier '
                'perfil con esas restricciones.', 'SI: confirmar identidad', 'CONTENIDO por HI, HG y OH; dificultad 3', 'CONTENIDO')
    if i == '1294':
        return ('Todas', 'RETENER (aplicado en esta orden, INC-4C-03)', 'Texto de circulos de brazos de pie frente a un balanceo '
                'acrobatico de 360 grados colgado de la barra (f1, f7, f13, f19). Instruccion contradictoria: no puede '
                'publicarse a nadie con ese texto.', 'SI: fijar identidad, texto y prescripcion', 'RETENIDO', 'CONTENIDO')
    if i == '1113':
        return ('Dificultad (perfil principiante)', 'Nivel 2 propuesto por R2 y V3; se conserva 1 por el dictamen de V39',
                'R2 L15 y V3 W06: empuje del peso corporal desde la plancha baja (pike a cobra). El dictamen historico V39 fija '
                '«1113 conserva los valores de V39» hasta su verificacion: no se cambia sin autorizacion expresa. No es una '
                'restriccion de seguridad del perfil (FL e INV ya lo retiran de quien restringe suelo o inversion).',
                'SI: verificacion que exige V39 (nivel 1 o 2)', 'SIN CONTENCION (decision historica vigente)', 'ABIERTO (dificultad)')
    if i == '1323':
        return ('Brazos por encima de la cabeza (OH ya marcada)', 'Corregir el texto (aplicado en V52.1)',
                'El texto pedia sentarse con los muslos bajo unos rodillos que el GIF no tiene (banco plano entre dos torres). '
                'La bandera OH es correcta (asas por encima de la cabeza en f1).', 'NO', 'No necesaria: texto corregido', 'CERRADO')
    if i == '1374':
        return ('No colgarse / brazos arriba / barra', 'Mantener HG y OH y la contencion INC-CAT-1374',
                'Elevacion alterna de rodillas colgado de una barra; el registro ya lleva HG, OH y barra (V47) y la contencion '
                'solo lo admite con barra declarada y sin restringir colgarse ni brazos arriba.',
                'NO para el dato; SI para retirar la contencion', 'CONTENIDO (INC-CAT-1374)', 'CONTENIDO')
    raise SystemExit('sin regla: %s %s %s' % (clave, i, origen))

REG_BY = {r['clave']: r for r in REG}
filas = [r for r in REG if r['prioridad'] == '1']
EXTRA = [('X-1113', '1113'), ('X-1323', '1323'), ('X-1133', '1133'), ('X-1175', '1175'), ('X-1374', '1374')] + \
        [('X-AP-' + i, i) for i in ['457', '458', '459', '462', '777', '920', '934', '1069', '1184', '1317', '1335']]
salida = []
def agregar(clave, i, origen):
    rr, cls, just, prof, cont, riesgo = regla(clave, i, origen)
    h, m, r2 = H.get(i, {}), M.get(i, {}), R2.get(i, {})
    instr = TEXTO_V521.get(i, m.get('instrucciones_es', ''))
    salida.append(dict(
        clave=clave, id=i or '-', nombre=m.get('nombre_es', REG_BY.get(clave, {}).get('nombre', '')),
        movimiento_representado=r2.get('movimiento_observado', '-') or '-',
        evidencia_visual=(' || '.join(V3.get(i, [])) or r2.get('fotogramas_clave', '-') or '-')[:700],
        instrucciones_vigentes=instr[:500] if i else '-',
        banderas_vigentes=h.get('banderas', '-') if i else '-', impacto=h.get('impacto', '-') if i else '-',
        dificultad=h.get('dificultad', '-') if i else '-',
        restriccion_en_riesgo=rr, clasificacion_propuesta=cls, justificacion=just,
        elegible_en_rejilla_168=str(ELEG[i]) if i else '-', revision_profesional=prof, contencion=cont, riesgo=riesgo,
        validacion_profesional='NINGUNA (no registrada)'))
for r in filas:
    agregar(r['clave'], r['id'], r['origen'])
for c, i in EXTRA:
    agregar(c, i, 'EXTRA')

os.makedirs(B + 'pendientes', exist_ok=True)
with open(B + 'pendientes/EXPEDIENTE_SEGURIDAD.tsv', 'w', newline='', encoding='utf-8') as f:
    w = csv.DictWriter(f, list(salida[0].keys()), delimiter='\t', lineterminator='\n'); w.writeheader(); w.writerows(salida)
c = collections.Counter(r['riesgo'] for r in salida)
p = collections.Counter('SI' if r['revision_profesional'].startswith('SI') else 'NO/opcional' for r in salida)
print('filas', len(salida), 'de ellas prioridad 1:', len(filas))
print('riesgo', dict(c)); print('profesional', dict(p))
print('riesgo prioridad 1', dict(collections.Counter(r['riesgo'] for r in salida[:len(filas)])))
print('profesional prioridad 1', dict(collections.Counter('SI' if r['revision_profesional'].startswith('SI') else 'NO/opcional' for r in salida[:len(filas)])))
