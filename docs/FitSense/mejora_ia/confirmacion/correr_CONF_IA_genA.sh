# CONFIRMACION IA (orden del 10/11/2026). Uso: COMMIT=<sha> R=<salida> FASE=GEN|ADA|INT|RES [RONDA=..] [SOLO_LISTA=..]
#   bash correr_CONF_IA.sh <bloque> ...
# Presupuesto INCREMENTAL PROPIO: USD 10, separado de MEJ ronda 2 (sus USD 7,9051 no se mezclan). Topes acumulados:
# generacion <= 5,50; + adaptacion <= 7,50; + integral <= 9,00; reserva hasta 10,00.
# El gasto se suma SOLO de las carpetas CONF_* de $R. Antes de cada bloque se comprueba el remanente de la fase y se
# pasa al arnes como tope duro (max-llamadas / max-usd); el arnes detiene todo ante un timeout o una prediccion sin
# metricas (que se reserva a 0,05 USD).
set -u
S=/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad
cd /home/user/wt_det; git checkout -q --detach $COMMIT; SHA=$(git rev-parse HEAD); mkdir -p $R
docker info >/dev/null 2>&1 || { (nohup dockerd > $S/dockerd_r2.log 2>&1 &); sleep 20; }
O=$PWD/docs/FitSense/evaluacion_integral_O/cohorte/O_PERFILES.csv; OSHA=9b996825a6c146f156b7bd8d585a024cb1ccc44adf2054b11f1badb5de2b2106
TODOS=O01,O02,O03,O04,O05,O06,O07,O08,O09,O10,O11,O12,O13,O14,O15,O16,O17,O18,O19,O20
G=O01,O02,O03,O04,O05,O06,O07,O08,O09,O10,O11,O12,O13,O14; P=O15,O16,O17,O18,O19,O20
k20="sh ./mvnw -o -B -ntp verify -Pintegration -DskipUTs=true -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=EvaluacionK20IT -Dfitsense.evaluacion.k20=true -Dfitsense.evaluacion.k20.commit=$SHA -Dfitsense.planning.policy.allow-candidates=true"
bo="-Dfitsense.planning.policy=PLAN-REV-2.0 -Dfitsense.evaluacion.k20.integral=true -Dfitsense.evaluacion.k20.perfiles=$O -Dfitsense.evaluacion.k20.perfiles-sha256=$OSHA"
MAXUSD=10.00; MAXN=500
case ${FASE:?FASE obligatoria} in GEN) TOPE_FASE=5.50 ;; ADA) TOPE_FASE=7.50 ;; INT) TOPE_FASE=9.00 ;; RES) TOPE_FASE=10.00 ;; *) echo "FASE desconocida"; exit 2 ;; esac
version() { curl -sS -A fitsense-mej/1.0 https://api.replicate.com/v1/models/openai/gpt-5-structured | python3 -I -c "import json,sys;d=json.load(sys.stdin);print(d['latest_version']['id'])"; }
gastado() { python3 -I - $R <<'PY'
import sys,glob,re
n=0;u=0.0
for f in glob.glob(sys.argv[1]+"/CONF_G1*_R*/99_resumen.txt"):
    t=open(f).read()
    if "llamadas_proveedor" in t:
        n+=int(re.search(r"llamadas_proveedor=(\d+)",t).group(1)); u+=float(re.search(r"usd_medido=([\d.]+)",t).group(1))
print(n, round(u,4))
PY
}
run() { nombre=$1; shift; $k20 "$@" -Dfitsense.evaluacion.k20.salida=$R/$nombre > $R/$nombre.log 2>&1; echo "RUN $nombre $(grep -E 'BUILD (SUCCESS|FAILURE)' $R/$nombre.log) $(grep -o 'usuarios=[0-9]* llamadas=[0-9]* incidencias=[0-9]*' $R/$nombre.log)"; }
pagado() { nombre=$1; shift
  read usadas usd <<< "$(gastado)"
  remn=$((MAXN-usadas)); remu=$(python3 -I -c "print(round(min($TOPE_FASE,$MAXUSD)-$usd,4))")
  echo "PRESUPUESTO antes de $nombre (fase ${FASE}, tope acumulado USD $TOPE_FASE): usadas=$usadas usd=$usd -> disponible $remn / USD $remu" | tee -a $R/presupuesto.txt
  if [ $remn -le 0 ] || python3 -I -c "import sys;sys.exit(0 if $remu<0.30 else 1)"; then echo "DETENIDO: presupuesto de la fase agotado antes de $nombre" | tee -a $R/presupuesto.txt; return 1; fi
  echo "VERSION antes de $nombre: $(version) $(date -u +%FT%TZ)" | tee -a $R/versiones.txt
  run $nombre "$@" -Dfitsense.evaluacion.k20.modo=REPLICATE \
    -Dfitsense.evaluacion.k20.autorizacion-replicate=SI -Dfitsense.evaluacion.k20.integral-replicate=SI \
    -Dfitsense.evaluacion.k20.credencial=proxy -Dfitsense.evaluacion.k20.sin-metricas=RESERVA_SIN_BLOQUEO \
    -Dfitsense.evaluacion.k20.max-llamadas=$remn -Dfitsense.evaluacion.k20.max-usd=$remu -Dfitsense.evaluacion.k20.coste-por-llamada=0.05
  grep -h "llamadas_proveedor\|usd_medido\|bloqueo" $R/$nombre/99_resumen.txt | tee -a $R/presupuesto.txt
}
# Generacion: solo el plan inicial (sin semanas ni ciclos). Adaptacion de los planes de la IA: no se mide aqui.
gen() { echo "-Dfitsense.evaluacion.k20.generacion-ia=SI -Dfitsense.evaluacion.k20.solo-generacion=true -Dfitsense.evaluacion.k20.prompt-generacion=$1 -Dfitsense.evaluacion.k20.prompt=v2.1"; }
# Adaptacion: generacion determinista (mismos planes y situaciones en todas las variantes), los 20 perfiles.
ada() { echo "-Dfitsense.evaluacion.k20.prompt=$1"; }
RONDA=${RONDA:-1}
for x in "$@"; do case $x in
 G17|G18A|G18C)
  v=$(case $x in G17) echo pr-1.7;; G18A) echo pr-1.8a;; G18C) echo pr-1.8c;; esac)
  pagado CONF_${x}_R${RONDA} $bo $(gen $v) -Dfitsense.evaluacion.k20.solo=${SOLO_LISTA:-$TODOS} || break ;;
 A21|A22A)
  v=$(case $x in A21) echo v2.1;; A22A) echo v2.2a;; esac)
  pagado CONF_${x}_R${RONDA}_G $bo $(ada $v) -Dfitsense.evaluacion.k20.solo=${SOLO_LISTA:-$G} || break
  [ -n "${SOLO_LISTA:-}" ] && continue   # compleccion: solo la fase G de los perfiles indicados
  pagado CONF_${x}_R${RONDA}_P $bo $(ada $v) -Dfitsense.evaluacion.k20.solo=$P -Dfitsense.evaluacion.k20.progresion=true || break ;;
 A21P|A22AP)  # compleccion de la fase P
  y=${x%P}; v=$(case $y in A21) echo v2.1;; A22A) echo v2.2a;; esac)
  pagado CONF_${y}_R${RONDA}_P $bo $(ada $v) -Dfitsense.evaluacion.k20.solo=${SOLO_LISTA:?SOLO_LISTA} -Dfitsense.evaluacion.k20.progresion=true || break ;;
 INT|INTP)
  # Flujo conjunto: generacion inicial con IA (PROMPT_GEN) -> validacion -> publicacion experimental -> adherencia
  # simulada del guion -> adaptacion con IA (v2.2a) -> V8C corregido. Mismos perfiles y guiones; nada nuevo.
  gi="-Dfitsense.evaluacion.k20.generacion-ia=SI -Dfitsense.evaluacion.k20.prompt-generacion=${PROMPT_GEN:?PROMPT_GEN} -Dfitsense.evaluacion.k20.prompt=v2.2a"
  if [ $x = INT ]; then pagado CONF_INT_R${RONDA}_G $bo $gi -Dfitsense.evaluacion.k20.solo=${SOLO_LISTA:-$G} || break
  else pagado CONF_INT_R${RONDA}_P $bo $gi -Dfitsense.evaluacion.k20.solo=${SOLO_LISTA:-$P} -Dfitsense.evaluacion.k20.progresion=true || break; fi ;;
 *) echo "bloque desconocido: $x"; exit 2 ;;
esac; done
[ -f $R/versiones.txt ] && echo "VERSION despues: $(version) $(date -u +%FT%TZ)" | tee -a $R/versiones.txt
echo "GASTO CONF-IA (bloque incremental): $(gastado)"
