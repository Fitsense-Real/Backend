"""Hoja de revision por ejercicio con TODOS los fotogramas del GIF original y la imagen fija (solo lectura).
Uso: python3 -I -B hojas_completas.py <catalogo.tsv> <dir dataset> <dir salida> [ids...]
Cada fotograma va numerado (f1..fN) para que el revisor cite la evidencia; la ultima celda es la imagen fija."""
import csv, os, sys
from PIL import Image, ImageDraw, ImageSequence, ImageFont
csv.field_size_limit(10**8)
F = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 14)
cat, ds, out = sys.argv[1], sys.argv[2], sys.argv[3]
rows = {r['id']: r for r in csv.DictReader(open(cat, encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE)}
ids = sys.argv[4:] or [i for i, r in rows.items() if r['active'] == 't']
os.makedirs(out, exist_ok=True)
W, COLS = 180, 5
for i in ids:
    r = rows[i]
    im = Image.open(os.path.join(ds, r['gif']))
    frames = [f.convert('RGB').copy() for f in ImageSequence.Iterator(im)]
    celdas = [(f'f{k + 1}', fr) for k, fr in enumerate(frames)]
    celdas.append(('imagen fija', Image.open(os.path.join(ds, r['image'])).convert('RGB')))
    filas = (len(celdas) + COLS - 1) // COLS
    hoja = Image.new('RGB', (COLS * W, filas * (W + 18) + 22), 'white')
    d = ImageDraw.Draw(hoja)
    d.text((4, 3), f"#{i}  ({len(frames)} fotogramas, {r['gif']})", fill='black', font=F)
    for k, (et, img) in enumerate(celdas):
        x, y = (k % COLS) * W, 22 + (k // COLS) * (W + 18)
        hoja.paste(img.resize((W, W)), (x, y + 18))
        d.text((x + 4, y + 1), et, fill='blue' if et != 'imagen fija' else 'darkred', font=F)
    hoja.save(os.path.join(out, f'ej_{int(i):04d}.png'))
print(len(ids), 'hojas')
