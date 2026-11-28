set -u
S=/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad; R=$S/evo/run_v11
cd /home/user/wt_catrun; git checkout -q --detach cb18381; SHA=$(git rev-parse HEAD)
dk() { docker info >/dev/null 2>&1 || { (nohup dockerd > $S/dockerd_o.log 2>&1 &); sleep 20; }; }
N=docs/FitSense/rediseno_politicas/cohorte_N/N_PERFILES_INDEPENDIENTES.csv
O=$PWD/docs/FitSense/evaluacion_integral_O/cohorte/O_PERFILES.csv
OSHA=9b996825a6c146f156b7bd8d585a024cb1ccc44adf2054b11f1badb5de2b2106
G=O01,O02,O03,O04,O05,O06,O07,O08,O09,O10,O11,O12,O13,O14; P=O15,O16,O17,O18,O19,O20
k20="sh ./mvnw -o -B -ntp verify -Pintegration -DskipUTs=true -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=EvaluacionK20IT -Dfitsense.evaluacion.k20=true -Dfitsense.evaluacion.k20.commit=$SHA -Dfitsense.planning.policy.allow-candidates=true"
run() { nombre=$1; shift; dk; $k20 "$@" -Dfitsense.evaluacion.k20.salida=$R/$nombre > $R/$nombre.log 2>&1; echo "RUN $nombre $(grep -E 'BUILD (SUCCESS|FAILURE)' $R/$nombre.log) $(grep -o 'usuarios=[0-9]* llamadas=[0-9]* incidencias=[0-9]*' $R/$nombre.log)"; }
for x in "$@"; do case $x in
 REG)
  run REG_REV2_D_U -Dfitsense.planning.policy=PLAN-REV-2.0 -Dfitsense.evaluacion.k20.modo=DETERMINISTA
  run REG_REV2_D_N -Dfitsense.planning.policy=PLAN-REV-2.0 -Dfitsense.evaluacion.k20.modo=DETERMINISTA -Dfitsense.evaluacion.k20.perfiles=$N
  run REG_HIST_D_U -Dfitsense.planning.policy=PLAN-HIST-1 -Dfitsense.evaluacion.k20.modo=DETERMINISTA
  run REG_REV2_S_U -Dfitsense.planning.policy=PLAN-REV-2.0 -Dfitsense.evaluacion.k20.modo=SIMULADA -Dfitsense.evaluacion.k20.prompt=v2.1
  run REG_REV2_S_N -Dfitsense.planning.policy=PLAN-REV-2.0 -Dfitsense.evaluacion.k20.modo=SIMULADA -Dfitsense.evaluacion.k20.prompt=v2.1 -Dfitsense.evaluacion.k20.perfiles=$N ;;
 S|Sp|D|HIST)
  pol=PLAN-REV-2.0; modo="-Dfitsense.evaluacion.k20.modo=SIMULADA -Dfitsense.evaluacion.k20.prompt=v2.1"
  [ $x = D ] && modo="-Dfitsense.evaluacion.k20.modo=DETERMINISTA"
  [ $x = HIST ] && { pol=PLAN-HIST-1; modo="-Dfitsense.evaluacion.k20.modo=DETERMINISTA"; }
  base="-Dfitsense.planning.policy=$pol $modo -Dfitsense.evaluacion.k20.integral=true -Dfitsense.evaluacion.k20.perfiles=$O -Dfitsense.evaluacion.k20.perfiles-sha256=$OSHA"
  run O_${x}_G $base -Dfitsense.evaluacion.k20.solo=$G
  run O_${x}_P $base -Dfitsense.evaluacion.k20.solo=$P -Dfitsense.evaluacion.k20.progresion=true ;;
esac; done
echo FIN_O
