#!/bin/bash
# Reproduce todas las cifras de fase5_calidad.md (solo lectura del repositorio; salida en el scratchpad).
set -euo pipefail
S=/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/aud3
K=/home/user/wt_aud/docs/FitSense/rediseno_politicas/evidencia/k20
mkdir -p $S/ext $S/csv
for t in REV2_D_J REV2_D_L; do [ -d $S/ext/$t ] || tar -xzf $K/$t.tar.gz -C $S/ext --no-same-owner; done
cd $S
python3 -I scripts/s0_verificar.py        > out_s0.txt
python3 -I scripts/s2_dosis_limitada.py   > out_s2.txt
python3 -I scripts/s3_brazos_fullbody.py  > out_s3.txt
python3 -I scripts/s4_causa_mecanica.py   > out_s4.txt
python3 -I scripts/s5_resumen_5_1.py      > out_s5.txt
python3 -I scripts/s6_sesiones_cortas.py  > out_s6.txt
python3 -I scripts/s7_casos_k20.py J03 L14 > out_s7.txt
python3 -I scripts/s8_catalogo_migraciones.py 1262 775 1320 1263 1261 1308 1115 509 853 253 1054 1073 1117 1093 973 318 496 1196 1195 1112 1103 > out_s8.txt
python3 -I scripts/s9_uso_ejercicios.py   > out_s9.txt
python3 -I scripts/s10_dosis_sin_dudosos.py > out_s10.txt
python3 -I scripts/s11_calidad_objetivo.py > out_s11.txt
python3 -I scripts/s12_detalles_5_4.py    > out_s12.txt
python3 -I scripts/s13_impacto_falsos.py  > out_s13.txt
python3 -I scripts/s14_criterios_cortas.py > out_s14.txt
python3 -I scripts/s15_gm_distribucion.py > out_s15.txt
python3 -I scripts/s16_slots_aislamiento.py > out_s16.txt
echo OK
