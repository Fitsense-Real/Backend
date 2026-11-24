# Uso: R=<carpeta salida> bash correr_G5M.sh <pasos...>
#   SMOKE  (PAGADO: O02, generacion inicial + adaptacion reales con openai/gpt-5-mini directo)
#   G | P  (PAGADOS: bloques de la cohorte O como GRA: G = O01-O14, P = O15-O20 con progresion)
#   DG | DP (gratuitos: mismos bloques en modo DETERMINISTA, para comparar)
# Presupuesto GLOBAL de la orden G5M: USD 8. Se descuentan las sondas previas (USD 0,01 de margen) y lo cargado por
# cada corrida (99_resumen.txt: usd_medido, que con COTA_POR_TOPE es la COTA SUPERIOR, no el gasto estimado).
# GC (complemento): O09-O14, cortados en G5M_G por el bloqueo de la reserva (O09: 296.281 caracteres -> 0,106);
#   se corre con RESERVA=0.13 (prompt maximo observado ~300.000 caracteres + retroalimentacion).
# MG | MP: ajuste por resultados medidos (orden G5M): reasoning_effort=medium en los perfiles G que con low acabaron
#   en respaldo (O03, O07, O08, O09, O12) y en el bloque P completo. RESERVA=0.13.
#   MG2: el resto de G con medium (O01, O02, O04, O05, O06, O10, O11, O13, O14), con el presupuesto restante.
# RESERVA 0.10 (antes 0.08): la cota de un prompt de ~200.000 caracteres es ~0,082 USD (O03: 192.454 -> 0,0803).
set -u
cd /home/user/wt_g5m; SHA=$(git rev-parse HEAD); mkdir -p $R
O=$PWD/docs/FitSense/evaluacion_integral_O/cohorte/O_PERFILES.csv; OSHA=9b996825a6c146f156b7bd8d585a024cb1ccc44adf2054b11f1badb5de2b2106
G=O01,O02,O03,O04,O05,O06,O07,O08,O09,O10,O11,O12,O13,O14; P=O15,O16,O17,O18,O19,O20
MAXUSD=7.99; RESERVA=${RESERVA:-0.10}
k20="/opt/maven/bin/mvn -o -B -ntp verify -Pintegration -DskipUTs=true -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=EvaluacionK20IT -Dfitsense.evaluacion.k20=true -Dfitsense.evaluacion.k20.commit=$SHA -Dfitsense.planning.policy.allow-candidates=true"
run() { nombre=$1; shift; $k20 "$@" -Dfitsense.evaluacion.k20.salida=$R/$nombre > $R/$nombre.log 2>&1; echo "RUN $nombre $(grep -E 'BUILD (SUCCESS|FAILURE)' $R/$nombre.log) $(grep -o 'usuarios=[0-9]* llamadas=[0-9]* incidencias=[0-9]*' $R/$nombre.log)"; }
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
pagado() { nombre=$1; tope=$2; shift 2
  read usadas usd <<< "$(gastado)"
  remu=$(python3 -c "print(round(min($MAXUSD-$usd, $tope),4))")
  echo "PRESUPUESTO antes de $nombre: cargado=$usd USD en $usadas predicciones -> disponible USD $remu"
  if python3 -c "import sys;sys.exit(0 if $remu<$RESERVA else 1)"; then echo "DETENIDO: presupuesto agotado antes de $nombre"; return 1; fi
  remn=$(python3 -c "print(int($remu/$RESERVA))")
  run $nombre "$@" -Dfitsense.evaluacion.k20.modo=REPLICATE -Dfitsense.evaluacion.k20.prompt=v2.1 \
    -Dfitsense.evaluacion.k20.autorizacion-replicate=SI -Dfitsense.evaluacion.k20.integral-replicate=SI \
    -Dfitsense.evaluacion.k20.generacion-ia=SI \
    -Dfitsense.evaluacion.k20.credencial=proxy -Dfitsense.evaluacion.k20.sin-metricas=COTA_POR_TOPE \
    -Dfitsense.evaluacion.k20.max-llamadas=$remn -Dfitsense.evaluacion.k20.max-usd=$remu -Dfitsense.evaluacion.k20.coste-por-llamada=$RESERVA
  grep -h "llamadas_proveedor\|usd_medido\|bloqueo" $R/$nombre/99_resumen.txt
}
base="-Dfitsense.planning.policy=PLAN-REV-2.0 -Dfitsense.evaluacion.k20.integral=true"
bo="$base -Dfitsense.evaluacion.k20.perfiles=$O -Dfitsense.evaluacion.k20.perfiles-sha256=$OSHA"
det="-Dfitsense.evaluacion.k20.modo=DETERMINISTA"
for x in "$@"; do case $x in
 SMOKE) pagado G5M_SMOKE_O02 0.80 $bo -Dfitsense.evaluacion.k20.solo=O02 || break ;;
 GC) pagado G5M_G_COMPLEMENTO 2.60 $bo -Dfitsense.evaluacion.k20.solo=O09,O10,O11,O12,O13,O14 || break ;;
 MG) pagado G5M_MEDIUM_G 2.00 $bo -Dfitsense.ai.reasoning-effort=medium -Dfitsense.evaluacion.k20.solo=O03,O07,O08,O09,O12 || break ;;
 MG2) pagado G5M_MEDIUM_G2 2.05 $bo -Dfitsense.ai.reasoning-effort=medium -Dfitsense.evaluacion.k20.solo=O01,O02,O04,O05,O06,O10,O11,O13,O14 || break ;;
 MP) pagado G5M_MEDIUM_P 2.00 $bo -Dfitsense.ai.reasoning-effort=medium -Dfitsense.evaluacion.k20.solo=$P -Dfitsense.evaluacion.k20.progresion=true || break ;;
 G) pagado G5M_G 4.50 $bo -Dfitsense.evaluacion.k20.solo=$G || break ;;
 P) pagado G5M_P 2.50 $bo -Dfitsense.evaluacion.k20.solo=$P -Dfitsense.evaluacion.k20.progresion=true || break ;;
 DG) run G5M_D_G $bo $det -Dfitsense.evaluacion.k20.solo=$G ;;
 DP) run G5M_D_P $bo $det -Dfitsense.evaluacion.k20.solo=$P -Dfitsense.evaluacion.k20.progresion=true ;;
esac; done
echo "CARGADO TOTAL (cota): $(gastado)"
echo FIN_G5M
