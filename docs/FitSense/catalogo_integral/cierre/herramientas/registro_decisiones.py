"""Registro unico y verificable de las decisiones pendientes del catalogo (orden de desbloqueo, 14/11/2026).

Parte de las 313 filas de REVISION_PROFESIONAL_LISTA.tsv (cierre FIN-3) y les asigna, sin inventar ninguna aprobacion:
  - bloque y prioridad (1 seguridad, 2 identidad y textos, 3 dificultad, 4 equipo);
  - quien debe decidir (PROFESIONAL, PROPIETARIO o ninguno si una comprobacion tecnica ya lo resolvio);
  - estado y si el valor VIGENTE del catalogo es el conservador mientras no se decida;
  - una comprobacion reproducible (columna de la huella, prueba o consulta).
Anade las decisiones que nacen de la orden de desbloqueo (1083, V53, 775) con clave A-*.
Los campos de respuesta (decision, valor, nombre, credenciales_o_funcion, fecha, observaciones) quedan VACIOS.

Uso (desde la raiz del repositorio):
  python3 -I -B docs/FitSense/catalogo_integral/cierre/herramientas/registro_decisiones.py
"""
import csv, collections, os, sys

BASE = 'docs/FitSense/catalogo_integral/cierre/'
HUELLA = 'src/test/resources/catalogo/huella_catalogo.tsv'
REJILLA = BASE + 'impacto/despues_V52_sin_V53.csv'
REFERENCIA = 'docs/FitSense/evidencia/progresion/generacion_referencia.txt'

def rd(p, d='\t'):
    with open(p, newline='', encoding='utf-8') as f:
        return list(csv.DictReader(f, delimiter=d))

H = {r['id']: r for r in rd(HUELLA)}
NOMBRE = {r['id']: r['nombre_es'] for r in rd(BASE + 'registro_maestro_final.tsv')}
LISTA = rd(BASE + 'REVISION_PROFESIONAL_LISTA.tsv')
assert len(LISTA) == 313, len(LISTA)

# Uso real: en que planes de la rejilla de 168 perfiles y de la referencia de 72 aparece cada ejercicio.
EN_REJILLA, ELEGIBLE_REJILLA = collections.Counter(), collections.Counter()
for r in rd(REJILLA, ';'):
    for i in {t.split(':')[2] for t in r['ejercicios'].split() if t.count(':') >= 3}: EN_REJILLA[i] += 1
    for i in filter(None, r['elegibles'].split(',')): ELEGIBLE_REJILLA[i] += 1
EN_REF = collections.Counter()
for l in open(REFERENCIA, encoding='utf-8'):
    for tok in l.split():
        if ':' in tok and tok.split(':')[0].isdigit(): EN_REF[tok.split(':')[0]] += 1

def vigente(i):
    h = H.get(i)
    if not h: return 'INACTIVO'
    return 'dificultad=%s; banderas=%s; impacto=%s; equipo=%s' % (h['dificultad'], h['banderas'], h['impacto'], h['requisitos'])

def uso(i):
    return 'elegible en %d/168 perfiles; en %d planes de la rejilla; %d apariciones en la referencia de 72' % (
        ELEGIBLE_REJILLA[i], EN_REJILLA[i], EN_REF[i])

CLARA_V3 = {'64', '620', '1261'}
ANADIR_SIN_APLICAR = {'868'}            # la bandera propuesta (INV) no esta: el vigente NO es el conservador
SUBIR_SIN_APLICAR = {'1113'}            # dificultad propuesta 2, vigente 1: NO conservador
OUT = []

def fila(clave, i, nombre, bloque, prio, quien, estado, conservador, decision, comprobacion, evidencia, origen):
    OUT.append(dict(clave=clave, id=i, nombre=nombre, bloque=bloque, prioridad=prio, quien_decide=quien,
                    estado=estado, vigente_conservador=conservador, valor_vigente=vigente(i) if i else '-',
                    uso_real=uso(i) if i else '-', decision_pendiente=decision, comprobacion=comprobacion,
                    evidencia=evidencia, origen=origen, decision='', valor='', nombre_responsable='',
                    credenciales_o_funcion='', fecha='', observaciones=''))

for n, r in enumerate(LISTA, 1):
    i, cat, dec, ev = r['id'], r['categoria'], r['decision_pendiente'], r['evidencia']
    clave = 'P%03d' % n
    comp = 'huella_catalogo.tsv (CatalogoCalidadIT) id ' + i
    if cat == 'DIFICULTAD (V27)':
        fila(clave, i, r['nombre_V50'], 'DIFICULTAD', 3, 'PROFESIONAL', 'ABIERTA', 'SI',
             'R2 propone BAJAR un nivel; V27 exige aprobacion humana para bajar. Vigente: el nivel mas alto (llega a menos perfiles)',
             comp + ', columna dificultad', ev, cat)
    elif cat == 'IDENTIDAD':
        if i == '675':
            fila(clave, i, r['nombre_V50'], 'IDENTIDAD', 2, 'NINGUNO (visto bueno opcional)', 'RESUELTA_TECNICA', 'SI',
                 'V52 renombro la ficha a lo que muestra el GIF (sentadilla sumo con mancuerna, sin vuelo) con R2 y V3: la ficha ya no contradice el GIF',
                 comp + ' y decisiones_v52.tsv (name_es, high_impact, impact_level)', ev, cat)
        else:
            prio = 1 if i in {'922', '930', '937', '1068', '1177', '1192', '1294', '1298'} else 2
            extra = ' La V3 confirmo la identidad (CLARA): solo visto bueno.' if i in CLARA_V3 else ''
            fila(clave, i, r['nombre_V50'], 'IDENTIDAD', prio, 'PROFESIONAL', 'ABIERTA', 'SI',
                 'Confirmar que ejercicio es; el GIF no lo permite.' + extra, comp, ev, cat)
    elif cat == 'TEXTO NO DECIDIBLE':
        conservador = 'NO' if i == '1294' else 'SI'
        nota = (' El texto vigente (circulos de brazos de pie) es ERRONEO frente al GIF (colgado de barra). Comprobacion tecnica:'
                ' no es elegible para ninguno de los 168 perfiles ni aparece en la referencia; HG y OH ya lo excluyen de'
                ' quien restringe colgarse o brazos arriba.') if i == '1294' else ''
        fila(clave, i, r['nombre_V50'], 'TEXTO', 2, 'PROFESIONAL', 'ABIERTA', conservador,
             'Instrucciones que el GIF no permite verificar.' + nota, comp + ', columna textos', ev, cat)
    elif cat.startswith('CONTENCION INC-4C-03'):
        fila(clave, i, r['nombre_V50'], 'SEGURIDAD', 1, 'PROFESIONAL', 'ABIERTA', 'SI',
             dec + '. Vigente: RETENIDO, no se prescribe a nadie',
             'ExternalCatalogService.RETENIDOS_INC_4C_03 y CatalogoElegibleIT', ev, cat)
    elif cat == 'CONTENCION INC-CAT-C39':
        fila(clave, i, r['nombre_V50'], 'SEGURIDAD', 1, 'PROFESIONAL', 'ABIERTA', 'SI',
             'Si el curl con el brazo a la altura del hombro cuenta como «brazos sobre la cabeza». Vigente: contenido para quien lo restringe',
             'ContencionTextoBandera y ContencionTextoBanderaIT', ev, cat)
    elif cat == 'DEC-APOYO-SUSPENDIDO (V53)':
        n15 = 'NO esta entre los 15 de V53 (manos en el suelo, pies en correas)' if i == '1301' else 'esta entre los 15 de V53'
        fila(clave, i, r['nombre_V50'], 'SEGURIDAD', 1, 'PROPIETARIO y PROFESIONAL', 'ABIERTA', 'SI',
             'Significado de «no colgarse» (ver A-V53). El ejercicio ' + n15 + '. Vigente: contenido por INC-CAT-C39 para quien restringe colgarse',
             'ApoyoSuspendidoInterruptorIT y Plan78ContencionReducidaIT', ev + ' / V53_APOYO_SUSPENDIDO.md', cat)
    elif cat == 'DEC-FACTIBILIDAD-775':
        fila(clave, i, r['nombre_V50'], 'EQUIPO', 4, 'NINGUNO (consecuencia documentada para el propietario)', 'RESUELTA_TECNICA', 'SI',
             'Aplicado en V52 (PR A) por la orden de 14/11/2026: exige banco. Consecuencia: 14 perfiles GAIN_MUSCLE en casa solo con peso corporal (3-4 dias, 58-75 min) quedan sin plan (V20)',
             'huella columna requisitos = 1:bench; Hipertrofia4Dv34IT fija los 14 perfiles y su motivo', ev, cat)
    elif cat in ('DEC-HI-LANZAMIENTOS', 'DEC-HI-CARRERA', 'DECISION PREVIA D2 (V37.1)', 'DEC-INV-DECLINADO'):
        fila(clave, i, r['nombre_V50'], 'SEGURIDAD', 1, 'PROFESIONAL', 'ABIERTA', 'SI',
             dec + ' (se conserva la bandera: el vigente es el conservador)', comp + ', columna banderas', ev, cat)
    elif cat == 'SEGURIDAD':
        fila(clave, i, r['nombre_V50'], 'SEGURIDAD', 1, 'PROFESIONAL', 'ABIERTA', 'SI',
             dec + ' (se conserva el valor vigente, que es el conservador)', comp, ev, cat)
    elif cat == 'BANDERA_ANADIR':
        fila(clave, i, r['nombre_V50'], 'SEGURIDAD', 1, 'PROFESIONAL', 'ABIERTA', 'NO' if i in ANADIR_SIN_APLICAR else 'SI',
             'Anadir INV: la cabeza no se ve claramente bajo la cadera. Vigente SIN INV; HG ya lo excluye de quien restringe colgarse',
             comp + ', columna banderas', ev, cat)
    elif cat == 'BANDERA_QUITAR':
        fila(clave, i, r['nombre_V50'], 'SEGURIDAD', 1, 'PROFESIONAL', 'ABIERTA', 'SI',
             'Quitar HI: el GIF no demuestra que nunca haya salto. Vigente con HI', comp + ', columna banderas', ev, cat)
    elif cat == 'DECISION PREVIA V39':
        fila(clave, i, r['nombre_V50'], 'DIFICULTAD', 3, 'PROFESIONAL', 'ABIERTA', 'NO' if i in SUBIR_SIN_APLICAR else 'SI',
             'R2 y V3 proponen SUBIR a nivel 2; el dictamen de V39 lo fija en 1 hasta su verificacion. Vigente nivel 1 (NO conservador)',
             comp + ', columna dificultad', ev, cat)
    else:
        sys.exit('categoria sin regla: ' + cat)

# Decisiones nacidas de la orden de desbloqueo.
fila('A-1083', '1083', 'Empuje de pecho con balon medicinal (respuesta unica)', 'SEGURIDAD', 1, 'PROFESIONAL', 'CONTENIDA', 'SI',
     'Contradiccion R03 RESUELTA de forma conservadora: HI anadido (coordinador APLICAR_CONSERVADOR) y CHECK ALTO implica high_impact. '
     'Queda la cuestion de clase DEC-HI-LANZAMIENTOS (1083, 1084, 1085, 1087): si el profesional decide que el lanzamiento no es pliometria, '
     'quitar HI exige tambien revisar el impacto ALTO. No se baja el impacto para recuperar elegibilidad',
     'huella (HI FL KN, ALTO); ck_exercises_alto_implica_high_impact en V52; CatalogoElegibleIT', 'coordinador.tsv fila 1083', 'ORDEN 14/11 punto 2')
fila('A-V53', '', 'Significado de «no colgarse» (apoyo suspendido sobre un aparato)', 'SEGURIDAD', 1, 'PROPIETARIO y PROFESIONAL', 'ABIERTA', 'SI',
     'Aprobar o no: (1) el significado conservador; (2) la lista de 15 (457, 458, 459, 462, 777, 920, 934, 1069, 1153, 1184, 1303, 1304, 1306, 1317, 1335); '
     '(3) que hacer con los perfiles que ya tienen el permiso guardado. Interruptor apagado hasta entonces',
     'ApoyoSuspendidoInterruptorIT; cierre/v53/impacto_permisos_v53_solo_lectura.sql', 'V53_APOYO_SUSPENDIDO.md', 'ORDEN 14/11 punto 3')
fila('A-1301', '1301', 'Crunch invertido suspendido', 'SEGURIDAD', 1, 'PROFESIONAL', 'ABIERTA', 'SI',
     'Si «no colgarse» debe cubrir la suspension de pies en correas con las manos en el suelo. No cumple el criterio conservador; sigue contenido por INC-CAT-C39',
     'huella apoyo_suspendido=false; ContencionTextoBandera', 'apoyo_suspendido_v53.tsv', 'ORDEN 14/11 punto 3')
fila('A-SUELO8', '', 'Posiciones con las manos en el suelo (855, 857, 860, 861, 885, 974, 1298, 1299)', 'SEGURIDAD', 2, 'PROFESIONAL', 'ABIERTA', 'SI',
     'Documentadas y sin marcar en V53. Todas de dificultad 3; 861, 885 y 1298 ademas retenidos (INC-4C-03)',
     'apoyo_suspendido_v53.tsv', 'apoyo_suspendido_v53.tsv', 'ORDEN 14/11 punto 3')

# Hallazgos colaterales de los verificadores (REVISION_VISUAL_COMPLETA.md §5): sin R2 ni V3, no aplicados. Se
# resuelven con una comprobacion tecnica mas (ronda R2 + V3 sobre el GIF), como el resto de V52; no piden un profesional.
TEC = 'TECNICO (siguiente ronda R2 + V3)'
for clave, i, nombre, bloque, prio, cons, dec in [
    ('A-231', '231', 'Press desde pull-over', 'IDENTIDAD', 2, 'NO_DETERMINADO',
     'El GIF es un press de banca cerrado: nombre, patron (HORIZONTAL_PUSH) y zona a revisar'),
    ('A-367', '367', '-', 'IDENTIDAD', 2, 'NO_DETERMINADO', 'Se resalta el deltoides posterior: zona y patron podrian ser otros'),
    ('A-1303T', '1303', 'Elevacion de piernas torsionada', 'TEXTO', 2, 'SI',
     'Las instrucciones dicen «silla romana» y el aparato es una silla del capitan. Contenido por INC-CAT-C39 para quien restringe colgarse'),
    ('A-1323', '1323', '-', 'TEXTO', 2, 'NO', 'Las instrucciones mencionan unos rodillos que no aparecen en el GIF'),
    ('A-1182', '1182', '-', 'EQUIPO', 4, 'SI', 'El equipo «plyo box» probablemente sobra (pies en el suelo); exigirlo es conservador'),
    ('A-1017', '1017', 'Prensa de piernas sentado en maquina', 'CLASIFICACION', 5, 'SI', 'Se resaltan tambien los gemelos: podrian anadirse como secundarios'),
]:
    fila(clave, i, NOMBRE.get(i, nombre), bloque, prio, TEC, 'ABIERTA', cons, dec,
         'huella_catalogo.tsv id ' + i, 'REVISION_VISUAL_COMPLETA.md §5 (verificadores V3)', 'HALLAZGO COLATERAL')

CAMPOS = list(OUT[0].keys())
with open(BASE + 'REGISTRO_DECISIONES_PENDIENTES.tsv', 'w', newline='', encoding='utf-8') as f:
    w = csv.DictWriter(f, CAMPOS, delimiter='\t', lineterminator='\n'); w.writeheader(); w.writerows(OUT)

c = collections.Counter
print('filas', len(OUT), 'ids', len({r['id'] for r in OUT if r['id']}))
print('por estado', dict(c(r['estado'] for r in OUT)))
print('por quien', dict(c(r['quien_decide'] for r in OUT)))
print('por prioridad', dict(sorted(c(r['prioridad'] for r in OUT).items())))
print('por bloque', dict(c(r['bloque'] for r in OUT)))
print('vigente NO conservador', [(r['clave'], r['id']) for r in OUT if r['vigente_conservador'] == 'NO'])
dif = [r for r in OUT if r['bloque'] == 'DIFICULTAD' and r['origen'] == 'DIFICULTAD (V27)']
print('dificultad: elegibles en alguna rejilla', sum(1 for r in dif if ELEGIBLE_REJILLA[r['id']]),
      'en algun plan de la rejilla', sum(1 for r in dif if EN_REJILLA[r['id']]), 'en la referencia', sum(1 for r in dif if EN_REF[r['id']]))
