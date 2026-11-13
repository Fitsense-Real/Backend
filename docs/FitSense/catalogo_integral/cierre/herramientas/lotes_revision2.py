"""Lotes de la segunda revision visual: 20 lotes intercalados (indice % 20) con manifiesto de atributos FINALES (V50),
sin veredictos previos. Uso: python3 -I -B lotes_revision2.py <catalogo_final.tsv> <dir salida>"""
import csv, os, re, sys
csv.field_size_limit(10**8)
cat, out = sys.argv[1], sys.argv[2]
rows = [r for r in csv.DictReader(open(cat, encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE) if r['active'] == 't']
rows.sort(key=lambda r: int(r['id']))
MOD = re.compile(r"\b(trote|trotar|trotando|correr|carrera|corriendo|skipping|rodillas altas|jumping jacks?|piernas separadas|"
                 r"comba|saltar la cuerda|salto de cuerda|saltos? a la cuerda|tijeras|escaladores? r[aá]pidos?)\b")
ALTO = re.compile(r"\b(salto|saltos|saltando|pliom[eé]trico|burpee|burpees|sprint|esprint|caja|rebote|zancada con salto|"
                  r"a una pierna con salto|patinador)\b")
def impacto(r):  # replica ImpactoPolicy.nivel (regla actual por nombre + high_impact)
    n = r['name_es'].lower(); m = bool(MOD.search(n))
    if r['hi'] == 't':
        return 'MODERADO' if m and not ALTO.search(n.replace('saltos con las piernas separadas', '')) else 'ALTO'
    return 'MODERADO' if m else 'BAJO'
B = [('hi', 'HI'), ('floor', 'FL'), ('axial', 'AX'), ('kneel', 'KN'), ('hang', 'HG'), ('overhead', 'OH'), ('inv', 'INV')]
N = 20
for k in range(N):
    lote = rows[k::N]
    d = os.path.join(out, f'L{k + 1:02d}'); os.makedirs(d, exist_ok=True)
    with open(os.path.join(d, 'manifiesto.md'), 'w', encoding='utf-8') as f:
        f.write(f'# Lote L{k + 1:02d} ({len(lote)} ejercicios)\n\nHojas: `../../hojas/ej_NNNN.png`. Salida: `revision2.tsv` en esta carpeta.\n\n')
        for r in lote:
            fl = ' '.join(t for c, t in B if r[c] == 't') or '-'
            f.write(f"## {r['id']}\n- nombre_es: {r['name_es']}\n- nombre de la fuente (puede ser erroneo): {r['name_en']}\n"
                    f"- zona: {r['bp']} · objetivo: {r['target']} · sinergistas: {r['synergist'] or '-'} · secundarios: {r['secondary'] or '-'}\n"
                    f"- patron: {r['pattern']} · aislamiento: {r['iso']} · prescripcion: {r['presc']} · dificultad: {r['dif']}\n"
                    f"- equipo principal: {r['equip']} · requisitos: {r['reqs'] or '- (sin requisitos: solo el principal)'}\n"
                    f"- banderas: {fl}\n- impacto derivado por regla actual: {impacto(r)} · estiramiento derivado por nombre: "
                    f"{'SI' if 'estiramiento' in r['name_es'].lower() else 'NO'}\n"
                    f"- instrucciones_es: {r['instr_es']}\n\n")
    open(os.path.join(d, 'ids.txt'), 'w').write('\n'.join(r['id'] for r in lote) + '\n')
print(N, 'lotes', [len(rows[k::N]) for k in range(N)])
