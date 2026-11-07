# Uso: COMMIT=<sha> R=<carpeta salida> bash correr_GRA.sh <pasos...>   (GRA: generacion inicial + adaptacion con Replicate real)
#   REG | S | D | F35D | F35S   (gratuitos)
#   R | Rp                      (PAGADOS: Replicate real; presupuesto global 168 predicciones / USD 8.40)
set -u
S=/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad
cd /home/user/wt_catrun; git checkout -q --detach $COMMIT; SHA=$(git rev-parse HEAD); mkdir -p $R
dk() { docker info >/dev/null 2>&1 || { (nohup dockerd > $S/dockerd_o.log 2>&1 &); sleep 20; }; }
N=docs/FitSense/rediseno_politicas/cohorte_N/N_PERFILES_INDEPENDIENTES.csv
O=$PWD/docs/FitSense/evaluacion_integral_O/cohorte/O_PERFILES.csv; OSHA=9b996825a6c146f156b7bd8d585a024cb1ccc44adf2054b11f1badb5de2b2106
F=$PWD/docs/FitSense/evaluacion_integral_O/replicate_real/cohorte/O11F35_COMPLEMENTARIO.csv; FSHA=1720227e9191c54af67b08669fcfcaf38444334d8fcc4c245832cdb531b93b54
G=O01,O02,O03,O04,O05,O06,O07,O08,O09,O10,O11,O12,O13,O14; P=O15,O16,O17,O18,O19,O20
# Remanente GLOBAL tras R_O/R'_O (20 predicciones, USD 0,3033): 148 / USD 8,0967. GRA-R1 tiene un tope propio de la
# MITAD (74 / USD 4,04) para que GRA-R2 conserve siempre su parte; GRA-R2 usa el resto.
MAXN=148; MAXUSD=8.0967; R1N=74; R1USD=4.04
k20="sh ./mvnw -o -B -ntp verify -Pintegration -DskipUTs=true -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=EvaluacionK20IT -Dfitsense.evaluacion.k20=true -Dfitsense.evaluacion.k20.commit=$SHA -Dfitsense.planning.policy.allow-candidates=true"
run() { nombre=$1; shift; dk; $k20 "$@" -Dfitsense.evaluacion.k20.salida=$R/$nombre > $R/$nombre.log 2>&1; echo "RUN $nombre $(grep -E 'BUILD (SUCCESS|FAILURE)' $R/$nombre.log) $(grep -o 'usuarios=[0-9]* llamadas=[0-9]* incidencias=[0-9]*' $R/$nombre.log)"; }
version() { curl -sS https://api.replicate.com/v1/models/openai/gpt-5-structured | python3 -c "import json,sys;d=json.load(sys.stdin);print(d['latest_version']['id'])"; }
gastado() { python3 - $R <<'PY'
import sys,glob,re
n=0;u=0.0
for f in glob.glob(sys.argv[1]+"/*/99_resumen.txt"):
    t=open(f).read()
    if "llamadas_proveedor" in t:
        n+=int(re.search(r"llamadas_proveedor=(\d+)",t).group(1)); u+=float(re.search(r"usd_medido=([\d.]+)",t).group(1))
print(n, round(u,4))
PY
}
pagado() { nombre=$1; shift
  read usadas usd <<< "$(gastado)"
  remn=$((MAXN-usadas)); remu=$(python3 -c "print(round($MAXUSD-$usd,4))")
  case $nombre in GRA_R1_*) remn=$((R1N-usadas)); remu=$(python3 -c "print(round(min($R1USD-$usd, $remu),4))");; esac
  echo "PRESUPUESTO antes de $nombre: usadas=$usadas usd=$usd -> disponible $remn predicciones / USD $remu"
  if [ $remn -le 0 ] || python3 -c "import sys;sys.exit(0 if $remu<0.05 else 1)"; then echo "DETENIDO: presupuesto agotado antes de $nombre"; return 1; fi
  echo "VERSION antes de $nombre: $(version) $(date -u +%FT%TZ)" | tee -a $R/versiones.txt
  run $nombre "$@" -Dfitsense.evaluacion.k20.modo=REPLICATE -Dfitsense.evaluacion.k20.prompt=v2.1 \
    -Dfitsense.evaluacion.k20.autorizacion-replicate=SI -Dfitsense.evaluacion.k20.integral-replicate=SI \
    -Dfitsense.evaluacion.k20.generacion-ia=SI \
    -Dfitsense.evaluacion.k20.credencial=proxy -Dfitsense.evaluacion.k20.sin-metricas=RESERVA_SIN_BLOQUEO \
    -Dfitsense.evaluacion.k20.max-llamadas=$remn -Dfitsense.evaluacion.k20.max-usd=$remu -Dfitsense.evaluacion.k20.coste-por-llamada=0.05
  grep -h "llamadas_proveedor\|usd_medido\|bloqueo" $R/$nombre/99_resumen.txt
}
base="-Dfitsense.planning.policy=PLAN-REV-2.0 -Dfitsense.evaluacion.k20.integral=true"
bo="$base -Dfitsense.evaluacion.k20.perfiles=$O -Dfitsense.evaluacion.k20.perfiles-sha256=$OSHA"
bf="$base -Dfitsense.evaluacion.k20.perfiles=$F -Dfitsense.evaluacion.k20.perfiles-sha256=$FSHA"
sim="-Dfitsense.evaluacion.k20.modo=SIMULADA -Dfitsense.evaluacion.k20.prompt=v2.1"; det="-Dfitsense.evaluacion.k20.modo=DETERMINISTA"
for x in "$@"; do case $x in
 REG)
  run REG_REV2_D_U -Dfitsense.planning.policy=PLAN-REV-2.0 $det
  run REG_REV2_D_N -Dfitsense.planning.policy=PLAN-REV-2.0 $det -Dfitsense.evaluacion.k20.perfiles=$N
  run REG_HIST_D_U -Dfitsense.planning.policy=PLAN-HIST-1 $det
  run REG_REV2_S_U -Dfitsense.planning.policy=PLAN-REV-2.0 $sim
  run REG_REV2_S_N -Dfitsense.planning.policy=PLAN-REV-2.0 $sim -Dfitsense.evaluacion.k20.perfiles=$N ;;
 S) run O_S_G $bo $sim -Dfitsense.evaluacion.k20.solo=$G; run O_S_P $bo $sim -Dfitsense.evaluacion.k20.solo=$P -Dfitsense.evaluacion.k20.progresion=true ;;
 D) run O_D_G $bo $det -Dfitsense.evaluacion.k20.solo=$G; run O_D_P $bo $det -Dfitsense.evaluacion.k20.solo=$P -Dfitsense.evaluacion.k20.progresion=true ;;
 F35D) run F35_D $bf $det ;;
 F35S) run F35_S $bf $sim ;;
 R1G) pagado GRA_R1_G $bo -Dfitsense.evaluacion.k20.solo=$G || break ;;
 R1P) pagado GRA_R1_P $bo -Dfitsense.evaluacion.k20.solo=$P -Dfitsense.evaluacion.k20.progresion=true || break ;;
 R1F) pagado GRA_R1_F35 $bf || break ;;
 R2)
  pagado GRA_R2_G $bo -Dfitsense.evaluacion.k20.solo=$G || break
  pagado GRA_R2_P $bo -Dfitsense.evaluacion.k20.solo=$P -Dfitsense.evaluacion.k20.progresion=true || break
  pagado GRA_R2_F35 $bf || break ;;
esac; done
[ -f $R/versiones.txt ] && echo "VERSION despues: $(version) $(date -u +%FT%TZ)" | tee -a $R/versiones.txt
echo "GASTO TOTAL: $(gastado)"
echo FIN_RO
