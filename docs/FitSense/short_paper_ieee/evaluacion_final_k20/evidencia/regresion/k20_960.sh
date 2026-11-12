#!/bin/sh
S=/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad
cd /home/user/wt_k20reg
R=$S/k20_960_resumen.txt
echo "HEAD $(git rev-parse HEAD)" > $R
for cfg in off:off preferencia:on; do
  g=${cfg%%:*}; ret=${cfg#*:}; n=$g-$ret
  git checkout -q -- docs
  rm -rf $S/k20_c960_$n
  sh ./mvnw -o -B -ntp verify -Pintegration -DskipUTs=true -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=ComparacionPerfilesIT,CampanaP123Cohorte02IT -Dfitsense.comparacion.perfiles=true -Dfitsense.campana.p123=true -Dfitsense.comparacion.salida=$S/k20_c960_$n -Dfitsense.adaptation.difficulty-guard=$g -Dfitsense.adaptation.pain-localized-removal=$ret > $S/k20_${n}_960.log 2>&1; echo "$n 960+replay exit=$?" >> $R
  grep -E "Tests run: [0-9]+, Failures" $S/k20_${n}_960.log | tail -1 >> $R
  (cd $S/k20_c960_$n && sha256sum planes.csv colocaciones.csv) >> $R
  git diff -- docs > $S/k20_replay_$n.diff
  git diff --stat -- docs | tail -1 | sed "s/^/$n replay: /" >> $R
  git checkout -q -- docs
done
echo FIN >> $R
