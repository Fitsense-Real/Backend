"""Hojas de fotogramas para la revision visual integral.
Uso: python3 -I -B hojas.py <catalogo.tsv> <dataset> <salida_dir> <por_hoja> <id> [<id> ...]
Una fila por ejercicio: 8 fotogramas equiespaciados del GIF original + la imagen fija, con dos lineas de cabecera
(id, nombres; grupo/objetivo/patron, dificultad, equipo, requisitos, banderas)."""
import csv, os, sys
from PIL import Image, ImageDraw, ImageSequence, ImageFont
F = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 13)
B = [('hi','HI'),('floor','FL'),('axial','AX'),('kneel','KN'),('hang','HG'),('overhead','OH'),('inv','INV')]
cat, ds, out, n = sys.argv[1], sys.argv[2], sys.argv[3], int(sys.argv[4])
ids = [int(x) for x in sys.argv[5:]]
rows = {int(r['id']): r for r in csv.DictReader(open(cat, encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE)}
os.makedirs(out, exist_ok=True)
W = 160
def fila(r):
    im = Image.open(os.path.join(ds, r['gif']))
    fr = [f.convert('RGB').copy() for f in ImageSequence.Iterator(im)]
    o = Image.new('RGB', (9 * W, W + 34), 'white')
    for k in range(8):
        o.paste(fr[int(k * len(fr) / 8)].resize((W, W)), (k * W, 34))
    o.paste(Image.open(os.path.join(ds, r['image'])).convert('RGB').resize((W, W)), (8 * W, 34))
    fl = ' '.join(t for k, t in B if r[k] == 't') or '-'
    d = ImageDraw.Draw(o)
    d.text((4, 2), f"#{r['id']} {r['name_es']}  //  {r['name_en']}"[:230], fill='black', font=F)
    d.text((4, 17), f"{r['bp']}/{r['target']}/{r['pattern']} d{r['dif']} {r['presc']} | eq={r['equip']} req={r['reqs'] or '-'} | {fl}"[:230], fill='blue', font=F)
    return o
for i in range(0, len(ids), n):
    lote = [rows[x] for x in ids[i:i+n]]
    fs = [fila(r) for r in lote]
    h = Image.new('RGB', (9 * W, sum(f.height for f in fs)), 'white')
    y = 0
    for f in fs:
        h.paste(f, (0, y)); y += f.height
    h.save(os.path.join(out, f"hoja_{int(lote[0]['id']):04d}.png"))
