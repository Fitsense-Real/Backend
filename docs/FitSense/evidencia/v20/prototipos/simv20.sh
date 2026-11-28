#!/bin/bash
# Simulacion de variantes V20. Uso: simv20.sh <salida> <variante>...   variante = nombre:catalogo:experimentos
cd /home/user/Backend
OUT=$1; shift
BASE="-o -q verify -Pintegration -Dtest=NoUnit -Dsurefire.failIfNoSpecifiedTests=false"
asegura() { docker info >/dev/null 2>&1 || { (dockerd > /tmp/claude-0/dockerd.log 2>&1 &); for i in $(seq 1 60); do docker info >/dev/null 2>&1 && break; sleep 1; done; }; }
for V in "$@"; do
  IFS=: read NOMBRE CAT EXP <<< "$V"
  LOC=""; [ "$CAT" = v37b ] && LOC="-Dspring.flyway.locations=classpath:db/migration,filesystem:docs/FitSense/auditoria_catalogo/v37/separables"
  X=""; [[ "$EXP" == *A* ]] && X="$X -Dfitsense.experimento.v20a=true"; [[ "$EXP" == *E* ]] && X="$X -Dfitsense.experimento.v20exacto=true"
  D=$OUT/$NOMBRE; mkdir -p $D; echo "== $NOMBRE ($CAT $EXP) $(date +%T)"
  asegura; mvn $BASE -Dit.test=GeneracionReferenciaCandidataIT -Dfitsense.candidata.referencia=$D/generacion_referencia.txt -Dfitsense.candidata.capturar=true $LOC $X > $D/ref.log 2>&1; echo "ref $?"
  asegura; mvn $BASE -Dit.test=EvaluacionFrecuenciaIT -Dfitsense.evaluacion=true -Dfitsense.evaluacion.dir=$D $LOC $X > $D/eval.log 2>&1; echo "eval $?"
  asegura; mvn $BASE -Dit.test=ImpactoRestriccionesIT -Dfitsense.impacto.dir=$D $LOC $X > $D/impacto.log 2>&1; echo "impacto $?"
  asegura; mvn $BASE -Dit.test=ContinuidadSemanalIT $LOC $X > $D/continuidad.log 2>&1; echo "continuidad $?"; cp target/failsafe-reports/TEST-*ContinuidadSemanalIT.xml $D/ 2>/dev/null
  asegura; mvn $BASE '-Dit.test=DiagnosticoV20ComposicionIT#diagnostico' -Dfitsense.v20.diagnostico.dir=$D $LOC $X > $D/diag.log 2>&1; echo "diag $?"
done
echo FIN $(date +%T)
