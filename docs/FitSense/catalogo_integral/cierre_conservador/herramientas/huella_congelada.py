"""Huella normalizada del volcado congelado (CatalogoCongeladoIT) de FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1.

El volcado en bruto incluye dos columnas con la hora de la reconstruccion (exercises.created_at y
exercise_difficulty_approvals.recorded_at), asi que su SHA-256 cambia en cada base nueva. Esta huella quita SOLO esas
columnas (por nombre, de la cabecera) y calcula el SHA-256 del resto, fila por fila y en el orden del volcado. Dos
reconstrucciones desde un PostgreSQL vacio deben dar la misma huella. Uso:
  python3 -I -B herramientas/huella_congelada.py <carpeta_volcado> [<carpeta_volcado_2>]
"""
import hashlib, os, sys

VARIABLES = {'created_at', 'recorded_at'}
ARCHIVOS = ['exercises.tsv', 'exercise_equipment_requirements.tsv', 'exercise_difficulty_approvals.tsv', 'flyway.tsv']

def huella(carpeta):
    resultado = {}
    for nombre in ARCHIVOS:
        with open(os.path.join(carpeta, nombre), encoding='utf-8') as f:
            lineas = f.read().split('\n')
        cabecera = lineas[0].split('\t')
        quitar = {i for i, c in enumerate(cabecera) if c in VARIABLES}
        h = hashlib.sha256()
        filas = 0
        for linea in lineas:
            if not linea:
                continue
            campos = linea.split('\t')
            h.update(('\t'.join(c for i, c in enumerate(campos) if i not in quitar) + '\n').encode('utf-8'))
            filas += 1
        resultado[nombre] = (h.hexdigest(), filas - 1, sorted(cabecera[i] for i in quitar))
    return resultado

uno = huella(sys.argv[1])
for nombre, (sha, filas, quitadas) in uno.items():
    print(f'{sha}  {nombre}  filas={filas}  sin={",".join(quitadas) or "-"}')
if len(sys.argv) > 2:
    dos = huella(sys.argv[2])
    iguales = all(uno[n][0] == dos[n][0] for n in ARCHIVOS)
    print('REPRODUCIBLE' if iguales else 'DISTINTO: ' + ', '.join(n for n in ARCHIVOS if uno[n][0] != dos[n][0]))
    sys.exit(0 if iguales else 1)
