"""Cotejo reproducible de los GIF de V36 (contactos con el suelo).

Uso: python3 -I cotejo_gif_v36.py <carpeta del dataset hasaneyldrm/exercises-dataset>

Los GIF son los de exercises.gif_path (180 x 180 px, (c) Gym visual). Para cada
fotograma mide la fila mas baja con pixeles de la figura (gris < 200): si baja la
figura entera del suelo, hay fase de vuelo.
- 1296 (videos/0803-4GqRrAk.gif): pies (x 0-34) y manos (x 140-179) por separado.
- 1333 (videos/3637-km2Ljzj.gif): la figura entera.
"""
import sys

from PIL import Image, ImageSequence

DATASET = sys.argv[1]


def fondo(gif, zonas):
    filas = []
    for fotograma in ImageSequence.Iterator(Image.open(f'{DATASET}/{gif}')):
        g = fotograma.convert('L')
        px, (ancho, alto) = g.load(), g.size
        filas.append([max([y for y in range(alto) for x in range(x0, x1) if px[x, y] < 200] or [None])
                      for x0, x1 in zonas])
    return filas


pies_manos = fondo('videos/0803-4GqRrAk.gif', [(0, 35), (140, 180)])
print('1296 fondo de pies y manos por fotograma:', pies_manos)
print('1296 rango pies', min(f[0] for f in pies_manos), '-', max(f[0] for f in pies_manos),
      '| rango manos', min(f[1] for f in pies_manos), '-', max(f[1] for f in pies_manos))
figura = [f[0] for f in fondo('videos/3637-km2Ljzj.gif', [(0, 180)])]
print('1333 fondo de la figura por fotograma:', figura)
print('1333 fotogramas sin apoyo (fondo < 160):', [i for i, y in enumerate(figura) if y < 160])
