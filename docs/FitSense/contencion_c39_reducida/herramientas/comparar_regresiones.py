"""Compara por clase de prueba dos logs de 'mvnw verify' (solo lectura).
Uso: python3 -I -B comparar_regresiones.py <log_referencia> <log_variante>"""
import re, sys
PAT = re.compile(r"Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+), Time elapsed: [^-]+-- in (.+)$")
def leer(p):
    d = {}
    for linea in open(p, encoding='utf-8', errors='replace'):
        m = PAT.search(linea.rstrip())
        if m:
            d[m.group(5).strip()] = tuple(int(x) for x in m.groups()[:4])
    return d
a, b = leer(sys.argv[1]), leer(sys.argv[2])
solo_a = sorted(set(a) - set(b)); solo_b = sorted(set(b) - set(a))
distintas = sorted(k for k in set(a) & set(b) if a[k] != b[k])
fallos_b = sorted(k for k, v in b.items() if v[1] or v[2])
print(f"clases: referencia {len(a)}, variante {len(b)}; comunes con el mismo resultado: "
      f"{sum(1 for k in set(a) & set(b) if a[k] == b[k])}")
print("solo en la referencia:", solo_a)
print("solo en la variante:", solo_b)
print("comunes con recuento distinto (tests, fallos, errores, omitidas):")
for k in distintas: print("  ", k, a[k], "->", b[k])
print("clases con fallos o errores en la variante:", fallos_b)
