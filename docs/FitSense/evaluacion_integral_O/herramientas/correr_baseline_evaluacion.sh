# Uso: COMMIT=<sha> R=<salida> bash correr_BASE.sh EVAL <nombre> <dir_propuestas>   (GRATUITO: sin Replicate, solo lectura)
set -u
S=/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad
cd /home/user/wt_catrun; git checkout -q --detach $COMMIT; SHA=$(git rev-parse HEAD); mkdir -p $R
docker info >/dev/null 2>&1 || { (nohup dockerd > $S/dockerd_o.log 2>&1 &); sleep 20; }
O=$PWD/docs/FitSense/evaluacion_integral_O/cohorte/O_PERFILES.csv; OSHA=9b996825a6c146f156b7bd8d585a024cb1ccc44adf2054b11f1badb5de2b2106
k20="sh ./mvnw -o -B -ntp verify -Pintegration -DskipUTs=true -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=EvaluacionK20IT -Dfitsense.evaluacion.k20=true -Dfitsense.evaluacion.k20.commit=$SHA -Dfitsense.planning.policy.allow-candidates=true"
nombre=$2; dir=$3
$k20 -Dfitsense.planning.policy=PLAN-REV-2.0 -Dfitsense.evaluacion.k20.integral=true -Dfitsense.evaluacion.k20.perfiles=$O \
  -Dfitsense.evaluacion.k20.perfiles-sha256=$OSHA -Dfitsense.evaluacion.k20.modo=DETERMINISTA \
  -Dfitsense.evaluacion.k20.baseline-evaluar=$dir -Dfitsense.evaluacion.k20.salida=$R/$nombre > $R/$nombre.log 2>&1
echo "RUN $nombre $(grep -E 'BUILD (SUCCESS|FAILURE)' $R/$nombre.log)"
