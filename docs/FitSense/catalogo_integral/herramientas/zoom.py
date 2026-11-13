"""Fotogramas grandes de un GIF. Uso: zoom.py <catalogo.tsv> <dataset> <salida.png> <id> [n=6] [tam=300]"""
import csv, os, sys
from PIL import Image, ImageSequence
rows = {r['id']: r for r in csv.DictReader(open(sys.argv[1], encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE)}
r = rows[sys.argv[4]]; n = int(sys.argv[5]) if len(sys.argv) > 5 else 6; t = int(sys.argv[6]) if len(sys.argv) > 6 else 300
fr = [f.convert('RGB').copy() for f in ImageSequence.Iterator(Image.open(os.path.join(sys.argv[2], r['gif'])))]
o = Image.new('RGB', (n * t, t), 'white')
for k in range(n): o.paste(fr[int(k * len(fr) / n)].resize((t, t)), (k * t, 0))
o.save(sys.argv[3])
