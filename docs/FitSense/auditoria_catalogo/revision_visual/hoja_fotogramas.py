"""Hojas de fotogramas para la revision visual (fase 2).

Uso: python3 -I hoja_fotogramas.py <catalogo.tsv> <carpeta del dataset> <salida_dir> <id> [<id> ...]

catalogo.tsv sale de exportar_catalogo.sql; la carpeta del dataset es un clon de
hasaneyldrm/exercises-dataset con los GIF de exercises.gif_path (180 x 180 px,
(c) Gym visual). Cada hoja lleva cinco ejercicios, una fila por ejercicio con
ocho fotogramas equiespaciados y una cabecera con id, nombre de la fuente,
body_part/target/pattern, nivel, requisitos y banderas (HI FL AX KN HG OH INV).
Las hojas no se versionan: son derivados de los GIF con copyright.
"""
import csv
import os
import sys

from PIL import Image, ImageDraw, ImageSequence

BANDERAS = ['HI', 'FL', 'AX', 'KN', 'HG', 'OH', 'INV']


def fila(dataset, r):
    fr = [f.convert('RGB').copy() for f in ImageSequence.Iterator(Image.open(os.path.join(dataset, r[22])))]
    out = Image.new('RGB', (8 * 150, 172), 'white')
    for k in range(8):
        out.paste(fr[int(k * len(fr) / 8)].resize((150, 150)), (k * 150, 22))
    banderas = ' '.join(b for b, v in zip(BANDERAS, r[15:22]) if v == 't')
    texto = f'{r[0]} {r[3]} | {r[5]}/{r[6]}/{r[13]} d{r[11]} | {r[10] or "bw"} | {banderas}'
    ImageDraw.Draw(out).text((4, 4), texto[:150], fill='black')
    return out


def main():
    catalogo, dataset, salida, ids = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4:]
    cat = {r[0]: r for r in csv.reader(open(catalogo, newline=''), delimiter='\t', quoting=csv.QUOTE_NONE)}
    os.makedirs(salida, exist_ok=True)
    for b in range(0, len(ids), 5):
        filas = [fila(dataset, cat[i]) for i in ids[b:b + 5]]
        hoja = Image.new('RGB', (1200, 172 * len(filas)), 'white')
        for k, f in enumerate(filas):
            hoja.paste(f, (0, 172 * k))
        hoja.save(os.path.join(salida, f'hoja_{b // 5:02d}.png'))


if __name__ == '__main__':
    main()
