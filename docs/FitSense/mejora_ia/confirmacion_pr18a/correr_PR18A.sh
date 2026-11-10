# CONFIRMACION FINAL pr-1.8a (orden del 10/11/2026). Solo GENERACION inicial (modo solo-generacion), sin adaptacion.
# Uso: COMMIT=<sha> RONDA=A2|A3 [MITAD=1|2] [SOLO_LISTA=..] [SUFIJO=..] bash correr_PR18A.sh G17|G18A ...
#
# Presupuesto: el saldo del bloque de USD 10 de CONF-IA (gastado USD 7,0950; saldo USD 2,9050). El gasto se suma de
# TODAS las carpetas CONF_* de $R (CONF-IA + esta campaña), con tope total USD 10,00: el saldo nunca se supera.
# Cada bloque recibe como tope duro (max-usd) el saldo real que queda. El arnes reserva USD 0,05 por cada llamada
# posible (puedeLlamar), asi que cada condicion se ejecuta en dos MITADES de perfiles (O01-O10 y O12-O20): cada perfil
# se ejecuta una sola vez y la reserva de peor caso de cada mitad cabe en el saldo.
# Version del modelo: se comprueba antes de cada bloque; si no es la congelada, el bloque NO se ejecuta.
set -u
S=/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad
R=/home/user/conf_runs
W=/home/user/wt_p18arun
VERSION_CONGELADA=81cb6179c0b94d630ee207cba05f83fdcc5726404a70b001abce4ff860dde3dd
cd $W; git fetch -q origin experimento/confirmacion-pr18a-final; git checkout -q --detach ${COMMIT:?COMMIT}; SHA=$(git rev-parse HEAD)
docker info >/dev/null 2>&1 || { rm -f /var/run/docker.pid; (nohup dockerd > $S/dockerd_pr18a.log 2>&1 &); sleep 20; }
O=$PWD/docs/FitSense/evaluacion_integral_O/cohorte/O_PERFILES.csv; OSHA=9b996825a6c146f156b7bd8d585a024cb1ccc44adf2054b11f1badb5de2b2106
M1=O01,O02,O03,O04,O05,O06,O07,O08,O09,O10; M2=O12,O13,O14,O15,O16,O17,O18,O19,O20
k20="sh ./mvnw -o -B -ntp verify -Pintegration -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=EvaluacionK20IT -Dfitsense.evaluacion.k20=true -Dfitsense.evaluacion.k20.commit=$SHA -Dfitsense.planning.policy.allow-candidates=true"
bo="-Dfitsense.planning.policy=PLAN-REV-2.0 -Dfitsense.evaluacion.k20.integral=true -Dfitsense.evaluacion.k20.perfiles=$O -Dfitsense.evaluacion.k20.perfiles-sha256=$OSHA"
MAXUSD=10.00; MAXN=600
version() { curl -sS -A fitsense-mej/1.0 https://api.replicate.com/v1/models/openai/gpt-5-structured | python3 -I -c "import json,sys;d=json.load(sys.stdin);print(d['latest_version']['id'])"; }
gastado() { python3 -I - $R <<'PY'
import sys,glob,re
n=0;u=0.0
for f in glob.glob(sys.argv[1]+"/CONF_*/99_resumen.txt"):
    t=open(f).read()
    if "llamadas_proveedor" in t:
        n+=int(re.search(r"llamadas_proveedor=(\d+)",t).group(1)); u+=float(re.search(r"usd_medido=([\d.]+)",t).group(1))
print(n, round(u,4))
PY
}
run() { nombre=$1; shift; $k20 "$@" -Dfitsense.evaluacion.k20.salida=$R/$nombre > $R/$nombre.log 2>&1; echo "RUN $nombre $(grep -E 'BUILD (SUCCESS|FAILURE)' $R/$nombre.log) $(grep -o 'usuarios=[0-9]* llamadas=[0-9]* incidencias=[0-9]*' $R/$nombre.log)"; }
pagado() { nombre=$1; shift
  read usadas usd <<< "$(gastado)"
  remn=$((MAXN-usadas)); remu=$(python3 -I -c "print(round($MAXUSD-$usd,4))")
  echo "PRESUPUESTO antes de $nombre (tope total USD $MAXUSD): usadas=$usadas usd=$usd -> saldo $remn / USD $remu" | tee -a $R/presupuesto_pr18a.txt
  if [ $remn -le 0 ] || python3 -I -c "import sys;sys.exit(0 if $remu<0.30 else 1)"; then echo "DETENIDO: saldo insuficiente antes de $nombre" | tee -a $R/presupuesto_pr18a.txt; return 1; fi
  v=$(version); echo "VERSION antes de $nombre: $v $(date -u +%FT%TZ)" | tee -a $R/versiones_pr18a.txt
  if [ "$v" != "$VERSION_CONGELADA" ]; then echo "DETENIDO: la version del modelo cambio ($v); $nombre no se ejecuta" | tee -a $R/presupuesto_pr18a.txt; return 1; fi
  run $nombre "$@" -Dfitsense.evaluacion.k20.modo=REPLICATE \
    -Dfitsense.evaluacion.k20.autorizacion-replicate=SI -Dfitsense.evaluacion.k20.integral-replicate=SI \
    -Dfitsense.evaluacion.k20.credencial=proxy -Dfitsense.evaluacion.k20.sin-metricas=RESERVA_SIN_BLOQUEO \
    -Dfitsense.evaluacion.k20.max-llamadas=$remn -Dfitsense.evaluacion.k20.max-usd=$remu -Dfitsense.evaluacion.k20.coste-por-llamada=0.05
  grep -h "llamadas_proveedor\|usd_medido\|bloqueo" $R/$nombre/99_resumen.txt | tee -a $R/presupuesto_pr18a.txt
  echo "VERSION despues de $nombre: $(version) $(date -u +%FT%TZ)" | tee -a $R/versiones_pr18a.txt
}
# Mismos parametros que las rondas C1-C3/A1: generacion inicial con IA, solo-generacion, PLAN-REV-2.0.
gen() { echo "-Dfitsense.evaluacion.k20.generacion-ia=SI -Dfitsense.evaluacion.k20.solo-generacion=true -Dfitsense.evaluacion.k20.prompt-generacion=$1 -Dfitsense.evaluacion.k20.prompt=v2.1"; }
for x in "$@"; do case $x in
 G17|G18A)
  v=$(case $x in G17) echo pr-1.7;; G18A) echo pr-1.8a;; esac)
  if [ -n "${SOLO_LISTA:-}" ]; then
    pagado CONF_P18A_${x}_R${RONDA:?RONDA}${SUFIJO:?SUFIJO} $bo $(gen $v) -Dfitsense.evaluacion.k20.solo=$SOLO_LISTA || break
  else
    for m in ${MITAD:-1 2}; do L=$([ $m = 1 ] && echo $M1 || echo $M2)
      pagado CONF_P18A_${x}_R${RONDA:?RONDA}_M$m $bo $(gen $v) -Dfitsense.evaluacion.k20.solo=$L || break 2
    done
  fi ;;
 *) echo "bloque desconocido: $x"; exit 2 ;;
esac; done
echo "GASTO TOTAL bloque USD 10 (CONF-IA + PR18A): $(gastado)"
