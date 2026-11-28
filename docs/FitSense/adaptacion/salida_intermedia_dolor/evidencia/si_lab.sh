#!/bin/sh
cd /home/user/wt_dif
for cfg in A:off:off B:on:off C:preferencia:off D:preferencia:on DB:on:on; do
  n=${cfg%%:*}; r=${cfg#*:}; g=${r%%:*}; ret=${r#*:}
  rm -rf /tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/si22_$n; mkdir -p /tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/si22_$n
  sh ./mvnw -o -B -ntp verify -Pintegration -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=SinSustitutoDolor24CasosIT -Dfitsense.validacion.24=true -Dfitsense.validacion.salida=/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/si22_$n -Dfitsense.adaptation.difficulty-guard=$g -Dfitsense.adaptation.pain-localized-removal=$ret > /tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/si22_$n.log 2>&1
  echo "$n exit=$?" >> /tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/si22_resumen.txt
done
echo FIN >> /tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/si22_resumen.txt
