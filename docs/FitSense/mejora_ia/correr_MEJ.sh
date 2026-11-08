# Uso: COMMIT=<sha> R=<salida> bash correr_MEJ.sh <paso> ...   SIM (gratis) | pagados en MEJ-2
set -u
S=/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad
cd /home/user/wt_catrun; git fetch -q origin experimento/mejora-ia; git checkout -q --detach $COMMIT; SHA=$(git rev-parse HEAD); mkdir -p $R
docker info >/dev/null 2>&1 || { (nohup dockerd > $S/dockerd_o.log 2>&1 &); sleep 20; }
O=$PWD/docs/FitSense/evaluacion_integral_O/cohorte/O_PERFILES.csv; OSHA=9b996825a6c146f156b7bd8d585a024cb1ccc44adf2054b11f1badb5de2b2106
G=O01,O02,O03,O04,O05,O06,O07,O08,O09,O10,O11,O12,O13,O14; P=O15,O16,O17,O18,O19,O20
k20="sh ./mvnw -o -B -ntp verify -Pintegration -DskipUTs=true -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=EvaluacionK20IT -Dfitsense.evaluacion.k20=true -Dfitsense.evaluacion.k20.commit=$SHA -Dfitsense.planning.policy.allow-candidates=true"
bo="-Dfitsense.planning.policy=PLAN-REV-2.0 -Dfitsense.evaluacion.k20.integral=true -Dfitsense.evaluacion.k20.perfiles=$O -Dfitsense.evaluacion.k20.perfiles-sha256=$OSHA"
# Bloque MEJ autorizado: 120 predicciones / USD 3,00 (aparte de las 168 anteriores). MEJ-G1 con tope propio de la mitad
# del remanente al empezar; MEJ-G2 el resto.
MAXN=120; MAXUSD=3.00
version() { curl -sS -A fitsense-mej/1.0 https://api.replicate.com/v1/models/openai/gpt-5-structured | python3 -c "import json,sys;d=json.load(sys.stdin);print(d['latest_version']['id'])"; }
gastado() { python3 - $R <<'PY'
import sys,glob,re
n=0;u=0.0
for f in glob.glob(sys.argv[1]+"/MEJ_*/99_resumen.txt"):
    t=open(f).read()
    if "llamadas_proveedor" in t:
        n+=int(re.search(r"llamadas_proveedor=(\d+)",t).group(1)); u+=float(re.search(r"usd_medido=([\d.]+)",t).group(1))
print(n, round(u,4))
PY
}
TOPE_N=""; TOPE_U=""
pagado() { nombre=$1; shift
  read usadas usd <<< "$(gastado)"
  remn=$((MAXN-usadas)); remu=$(python3 -c "print(round($MAXUSD-$usd,4))")
  if [ -n "$TOPE_N" ]; then remn=$(( TOPE_N - usadas < remn ? TOPE_N - usadas : remn )); remu=$(python3 -c "print(round(min($TOPE_U-$usd,$remu),4))"); fi
  echo "PRESUPUESTO antes de $nombre: usadas=$usadas usd=$usd -> disponible $remn / USD $remu"
  if [ $remn -le 0 ] || python3 -c "import sys;sys.exit(0 if $remu<0.05 else 1)"; then echo "DETENIDO: presupuesto agotado antes de $nombre"; return 1; fi
  echo "VERSION antes de $nombre: $(version) $(date -u +%FT%TZ)" | tee -a $R/versiones.txt
  run $nombre "$@" -Dfitsense.evaluacion.k20.modo=REPLICATE -Dfitsense.evaluacion.k20.prompt=${PADA:-v2.2} \
    -Dfitsense.evaluacion.k20.autorizacion-replicate=SI -Dfitsense.evaluacion.k20.integral-replicate=SI \
    -Dfitsense.evaluacion.k20.credencial=proxy -Dfitsense.evaluacion.k20.sin-metricas=RESERVA_SIN_BLOQUEO \
    -Dfitsense.evaluacion.k20.max-llamadas=$remn -Dfitsense.evaluacion.k20.max-usd=$remu -Dfitsense.evaluacion.k20.coste-por-llamada=0.05
  grep -h "llamadas_proveedor\|usd_medido\|bloqueo" $R/$nombre/99_resumen.txt
}
gen="-Dfitsense.evaluacion.k20.generacion-ia=SI -Dfitsense.evaluacion.k20.prompt-generacion=pr-1.8"
run() { nombre=$1; shift; $k20 "$@" -Dfitsense.evaluacion.k20.salida=$R/$nombre > $R/$nombre.log 2>&1; echo "RUN $nombre $(grep -E 'BUILD (SUCCESS|FAILURE)' $R/$nombre.log) $(grep -o 'usuarios=[0-9]* llamadas=[0-9]* incidencias=[0-9]*' $R/$nombre.log)"; }
for x in "$@"; do case $x in
 P18) run PROMPTS_PR18 $bo -Dfitsense.evaluacion.k20.modo=DETERMINISTA -Dfitsense.evaluacion.k20.prompt-generacion=pr-1.8 -Dfitsense.evaluacion.k20.baseline-evaluar=$S/evo/baseline/vacio ;;
 P17) run PROMPTS_PR17 $bo -Dfitsense.evaluacion.k20.modo=DETERMINISTA -Dfitsense.evaluacion.k20.baseline-evaluar=$S/evo/baseline/vacio ;;
 SIMA) run SIM_ADA_V22 $bo -Dfitsense.evaluacion.k20.modo=SIMULADA -Dfitsense.evaluacion.k20.prompt=v2.2 -Dfitsense.evaluacion.k20.solo=$G ;;
 SIMP) run SIM_ADA_V22_P $bo -Dfitsense.evaluacion.k20.modo=SIMULADA -Dfitsense.evaluacion.k20.prompt=v2.2 -Dfitsense.evaluacion.k20.solo=$P -Dfitsense.evaluacion.k20.progresion=true ;;
 A1|A2)
  pagado MEJ_${x}_G $bo -Dfitsense.evaluacion.k20.solo=$G || break
  pagado MEJ_${x}_P $bo -Dfitsense.evaluacion.k20.solo=$P -Dfitsense.evaluacion.k20.progresion=true || break ;;
 G1)
  read usadas usd <<< "$(gastado)"; TOPE_N=$(( usadas + (MAXN-usadas)/2 )); TOPE_U=$(python3 -c "print(round($usd+($MAXUSD-$usd)/2,4))")
  pagado MEJ_G1_G $bo $gen -Dfitsense.evaluacion.k20.solo=$G || break
  pagado MEJ_G1_P $bo $gen -Dfitsense.evaluacion.k20.solo=$P -Dfitsense.evaluacion.k20.progresion=true || break
  TOPE_N=""; TOPE_U="" ;;
 G2)
  pagado MEJ_G2_G $bo $gen -Dfitsense.evaluacion.k20.solo=$G || break
  pagado MEJ_G2_P $bo $gen -Dfitsense.evaluacion.k20.solo=$P -Dfitsense.evaluacion.k20.progresion=true || break ;;
 A0)
  PADA=v2.1 pagado MEJ_A0_G $bo -Dfitsense.evaluacion.k20.solo=$G || break
  PADA=v2.1 pagado MEJ_A0_P $bo -Dfitsense.evaluacion.k20.solo=$P -Dfitsense.evaluacion.k20.progresion=true || break ;;
 C)
  ctl="-Dfitsense.evaluacion.k20.generacion-ia=SI -Dfitsense.evaluacion.k20.prompt-generacion=pr-1.7"
  PADA=v2.1 pagado MEJ_C_G $bo $ctl -Dfitsense.evaluacion.k20.solo=$G || break
  PADA=v2.1 pagado MEJ_C_P $bo $ctl -Dfitsense.evaluacion.k20.solo=$P -Dfitsense.evaluacion.k20.progresion=true || break ;;
 G1C)
  pagado MEJ_G1C_G $bo $gen -Dfitsense.evaluacion.k20.solo=${SOLO_LISTA:-O04,O05,O06,O07,O08,O09,O10,O12,O13,O14} || break ;;
 G1R)
  pagado MEJ_G1R_G $bo $gen -Dfitsense.evaluacion.k20.solo=O03 || break ;;
 SIM) run SIM_G $bo -Dfitsense.evaluacion.k20.modo=SIMULADA -Dfitsense.evaluacion.k20.prompt=v2.2 -Dfitsense.evaluacion.k20.prompt-generacion=pr-1.8 -Dfitsense.evaluacion.k20.generacion-ia=SI -Dfitsense.evaluacion.k20.solo=$G ;;
esac; done
[ -f $R/versiones.txt ] && echo "VERSION despues: $(version) $(date -u +%FT%TZ)" | tee -a $R/versiones.txt
echo "GASTO MEJ: $(gastado)"
