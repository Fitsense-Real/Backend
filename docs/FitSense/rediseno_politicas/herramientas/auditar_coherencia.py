#!/usr/bin/env python3
"""Auditoria de coherencia previa a Replicate: recalcula, DESDE LAS EVIDENCIAS ARCHIVADAS, los oraculos y las huellas
de las corridas K20 del cierre, las cifras de los barridos finales y la inmutabilidad de la evidencia historica, y lo
compara con lo que afirman 17_VERIFICACION_FINAL.md y oraculos_f8.txt. No modifica nada del repositorio (extrae en un
directorio temporal). Uso: python3 -I auditar_coherencia.py <raiz_del_repo> <dir_temporal>"""
import csv, gzip, json, os, re, subprocess, sys, tarfile

sys.dont_write_bytecode = True
R, T = sys.argv[1], sys.argv[2]
RP = os.path.join(R, "docs/FitSense/rediseno_politicas")
SP = os.path.join(R, "docs/FitSense/short_paper_ieee")
Q = os.path.join(RP, "herramientas/oraculo_k20_politica.py")
H = os.path.join(SP, "k20_adaptacion_real/herramientas/oraculo_k20.py")
PERF = {"U": [os.path.join(SP, "evaluacion_final_k20/entrada/03_PERFILES_U01_U20.csv"),
              os.path.join(SP, "evaluacion_final_k20/entrada/04_ORACULO_TEORICO_HISTORICO.csv")],
        "J": [os.path.join(SP, "k20_adaptacion_real/entrada/J_PERFILES_INDEPENDIENTES.csv")],
        "K": [os.path.join(R, "docs/FitSense/auditoria_post_rrp/cohorte_K/K_PERFILES_INDEPENDIENTES.csv")],
        "L": [os.path.join(RP, "cohorte_L/L_PERFILES_INDEPENDIENTES.csv")],
        "M": [os.path.join(RP, "cohorte_M/M_PERFILES_INDEPENDIENTES.csv")]}
problemas, filas = [], []
env = dict(os.environ, PYTHONDONTWRITEBYTECODE="1")


def oraculo(script, extra, carpeta, c):
    out = subprocess.run(["python3", "-I", script] + extra + [carpeta] + PERF[c], capture_output=True, text=True,
                         env=env).stdout.strip().splitlines()[-1]
    js, huella = out.rsplit(" ", 1)
    return json.loads(js), huella


# 1 · K20 del cierre: extraer cada tar y recalcular los tres oraculos
K = os.path.join(RP, "evidencia/k20_cierre")
ref = {}
for l in open(os.path.join(K, "oraculos_f8.txt"), encoding="utf-8"):
    m = re.match(r"(\S+) \| .* huella (\w{12})", l)
    if m: ref[m.group(1)] = m.group(2)
for tar in sorted(f for f in os.listdir(K) if f.endswith(".tar.gz")):
    nombre = tar[:-7]
    run = nombre.replace("postfix_", "")
    c = "U" if run.endswith("FALLO") else run.rstrip("v21")[-1]
    dest = os.path.join(T, nombre)
    if not os.path.exists(dest):
        os.makedirs(dest)
        with tarfile.open(os.path.join(K, tar)) as t:
            t.extractall(dest, filter="data")
    carpeta = os.path.join(dest, run)
    cfg = open(os.path.join(carpeta, "00_configuracion.txt"), encoding="utf-8").read()
    commit = re.search(r"commit: (\w+)", cfg).group(1)[:7]
    pol = re.search(r"politica_por_defecto: (\S+)", cfg).group(1)
    llam = re.search(r"llamadas_proveedor=(\d+)", open(os.path.join(carpeta, "99_resumen.txt")).read()).group(1)
    h, hh = oraculo(H, [], carpeta, c)
    v, hv = oraculo(Q, ["PLAN-REV-2.0"], carpeta, c)
    d, hd = oraculo(Q, ["PLAN-REV-2.0+DIAS-1"], carpeta, c)
    hdh, _ = oraculo(Q, ["PLAN-HIST-1+DIAS-1"], carpeta, c)
    if not (hh == hv == hd): problemas.append(f"{nombre}: huellas distintas entre oraculos")
    if nombre in ref and ref[nombre] != hh[:12]: problemas.append(f"{nombre}: huella {hh[:12]} != oraculos_f8 {ref[nombre]}")
    if llam != "0": problemas.append(f"{nombre}: llamadas_proveedor={llam}")
    filas.append([nombre, commit, pol, json.dumps(h), json.dumps(v), json.dumps(d), json.dumps(hdh), hh[:12], llam])

# 2 · Huellas HIST frente a la congelacion (08_CONGELACION §8 y la evidencia previa)
CONG = {"HIST_D_U": "f9f67973", "HIST_D_J": "276d535d", "HIST_D_K": "54a5bda0", "HIST_D_L": "a1379048"}
for f in filas:
    if f[0] in CONG and not f[7].startswith(CONG[f[0]]): problemas.append(f"{f[0]}: HIST no coincide con la congelada")
    if f[0].startswith("REV2_Dp_"):
        par = next((g for g in filas if g[0] == f[0].replace("Dp", "D")), None)
        if par and par[7] != f[7]: problemas.append(f"{f[0]}: D != D'")

# 3 · Barridos finales
B = os.path.join(RP, "evidencia/barridos")
def leer(d): return list(csv.DictReader(open(os.path.join(B, d, "barrido_generacion.csv"))))
hist_ref = gzip.open(os.path.join(B, "HIST_final/planes.csv.gz"), "rt").read()
hist_fin = gzip.open(os.path.join(B, "HIST_cierre/planes.csv.gz"), "rt").read()
if hist_ref != hist_fin: problemas.append("barrido HIST: planes distintos de la evidencia")
rev = [x for x in leer("REV2_cierre") if x["resultado"] == "VALIDO"]
cif = {"hist_validos": sum(x["resultado"] == "VALIDO" for x in leer("HIST_cierre")), "rev2_validos": len(rev),
       "dosis_limitada": sum(x["estado_dosis"] == "DOSIS_LIMITADA" for x in rev),
       "dosis_objetivo": sum(x["estado_dosis"] == "DOSIS_OBJETIVO" for x in rev),
       "mas_de_24": sum(int(x["series_grupo_max"] or 0) > 24 for x in rev),
       "pull_sin_traccion": sum(int(x["pull_sin_traccion"] or 0) > 0 for x in rev),
       "ej_min_menos_3": sum(int(x["ej_min"] or 9) < 3 for x in rev),
       "moderado": sum(int(x["moderado_impacto"] or 0) > 0 for x in rev)}
esperado = {"hist_validos": 3641, "rev2_validos": 3776, "dosis_limitada": 1898, "dosis_objetivo": 1878, "mas_de_24": 0,
            "pull_sin_traccion": 0, "ej_min_menos_3": 0, "moderado": 500}
for k, v in esperado.items():
    if cif[k] != v: problemas.append(f"barrido: {k}={cif[k]} y el informe dice {v}")

# 4 · Inmutabilidad de la evidencia historica desde el cierre anterior (d4556b1) y desde la congelacion (89a1e4f)
def cambios(desde, rutas):
    out = subprocess.run(["git", "-C", R, "diff", "--name-status", desde, "HEAD", "--"] + rutas, capture_output=True,
                         text=True).stdout.strip().splitlines()
    return [l for l in out if not l.startswith("A")]
for desde, rutas in [("d4556b1", ["docs/FitSense/short_paper_ieee", "docs/FitSense/auditoria_post_rrp",
                                  "docs/FitSense/rediseno_politicas/evidencia/k20", "docs/FitSense/rediseno_politicas/evidencia/barridos/HIST",
                                  "docs/FitSense/rediseno_politicas/evidencia/barridos/REV2", "docs/FitSense/rediseno_politicas/evidencia/barridos/HIST_final",
                                  "docs/FitSense/rediseno_politicas/evidencia/barridos/REV2_final", "docs/FitSense/rediseno_politicas/cohorte_L"]),
                     ("2dc4abe", ["docs/FitSense/short_paper_ieee"])]:
    c = cambios(desde, rutas)
    if c: problemas.append(f"evidencia modificada desde {desde}: {c}")

with open(os.path.join(T, "auditoria_coherencia.csv"), "w", newline="") as f:
    w = csv.writer(f); w.writerow(["corrida", "commit", "politica", "historico", "rev2", "rev2_dias", "hist_dias", "huella", "llamadas"])
    w.writerows(filas)
print(json.dumps({"corridas": len(filas), "barridos": cif, "problemas": problemas}, ensure_ascii=False, indent=1))
