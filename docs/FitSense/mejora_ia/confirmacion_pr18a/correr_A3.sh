# A3 (pr-1.8a -> PR-1.7) en tandas de 5 perfiles (I-P-4): con el saldo que queda, la reserva de peor caso del arnes
# (USD 0,05 por llamada posible) no deja ejecutar una mitad completa al final. Cada perfil se ejecuta una vez.
# Regla fijada antes de A3: O09 de pr-1.8a se intenta UNA vez (tres timeouts seguidos en A2, I-P-3).
C=/home/user/wt_p18a/docs/FitSense/mejora_ia/confirmacion_pr18a/correr_PR18A.sh
cd /home/user/conf_runs
SALDO=$(python3 -I - <<'PY'
import glob,re
u=sum(float(re.search(r"usd_medido=([\d.]+)",open(f).read()).group(1)) for f in glob.glob("/home/user/conf_runs/CONF_*/99_resumen.txt") if "usd_medido" in open(f).read())
print(round(10-u,4))
PY
)
echo "SALDO al empezar A3: $SALDO"
python3 -I -c "import sys;sys.exit(0 if $SALDO>=1.45 else 1)" || { echo "A3 NO se inicia: saldo < 1,45 (protocolo §4)"; exit 0; }
for x in G18A G17; do
  for t in "T1 O01,O02,O03,O04,O05" "T2 O06,O07,O08,O09,O10" "T3 O12,O13,O14,O15,O16" "T4 O17,O18,O19,O20"; do
    set -- $t
    COMMIT=5f48dd5 RONDA=A3 SUFIJO=_$1 SOLO_LISTA=$2 bash $C $x
  done
done
