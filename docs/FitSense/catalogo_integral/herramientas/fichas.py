"""Fichas de texto (instrucciones) para acompanar las hojas. Uso: fichas.py <catalogo.tsv> <salida.txt> <id>..."""
import csv, sys
rows = {r['id']: r for r in csv.DictReader(open(sys.argv[1], encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE)}
with open(sys.argv[2], 'w', encoding='utf-8') as o:
    for i in sys.argv[3:]:
        r = rows[i]
        o.write(f"#{i} | {r['name_es']} | src: {r['name_en']} | {r['bp']}/{r['target']}/{r['pattern']} d{r['dif']} {r['presc']} iso={r['iso']}\n")
        o.write(f"  equipo={r['equip']} req={r['reqs'] or '-'} | HI={r['hi']} FL={r['floor']} AX={r['axial']} KN={r['kneel']} HG={r['hang']} OH={r['overhead']} INV={r['inv']}\n")
        o.write(f"  ES: {r['instr_es']}\n  EN: {r['instr_en']}\n\n")
