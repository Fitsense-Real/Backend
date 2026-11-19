"""Guarda texto/bandera sobre name_es + instructions_es (activos). Uso: guarda_texto.py <catalogo.tsv>"""
import csv, re, sys, collections
R = {
 'hang':  r'cu[eé]lga(te|rse)|colg(ado|ad[oa]s|ándote|andote)|suspendid[oa] de (la|una) barra|dominad',
 'overhead': r'por encima de la cabeza|sobre la cabeza|hacia el techo con los brazos|brazos (completamente )?extendidos (hacia )?arriba',
 'floor': r'(t[uú]mbate|acu[eé]state|si[eé]ntate|tumbad[oa]|sentad[oa]|rodillas|manos|antebrazos)[^.]{0,40}(en|sobre) el suelo|en el suelo con|esterilla|colchoneta|cuatro patas|posici[oó]n de (flexi[oó]n|plancha)',
 'kneel': r'arrod[ií]ll|de rodillas|rodilla (apoyada|en el suelo|sobre el banco)',
}
rows = [r for r in csv.DictReader(open(sys.argv[1], encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE) if r['active'] == 't']
hits = collections.defaultdict(list)
for r in rows:
    t = (r['name_es'] + ' . ' + r['instr_es']).lower()
    for k, p in R.items():
        if re.search(p, t) and r[k] != 't':
            hits[k].append(r['id'])
for k, v in hits.items(): print(k, len(v), ' '.join(v))
