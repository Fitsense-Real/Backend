#!/bin/sh
# Verifica los DOS comportamientos sin ocultar diferencias (cierre del PR #39).
#
#   1. Configuracion POR DEFECTO (la de generation y CI): mvnw verify -Pintegration debe terminar con 0 fallos.
#   2. Configuracion EXPERIMENTAL (limite preferencia + retirada on, por -D): deben fallar EXACTAMENTE las tres pruebas
#      que fijan el comportamiento por defecto o el historico. Cualquier otro fallo es una regresion; que una de las tres
#      NO falle significa que el cambio funcional esperado no ocurrio. El comportamiento nuevo lo afirman, en positivo,
#      las pruebas que fijan su propia configuracion (RetiradaLocalizada*IT, LimiteDificultad*IT...), que corren en las
#      dos ejecuciones.
#
# Uso: sh docs/FitSense/adaptacion/salida_intermedia_dolor/verificar_configuraciones.sh   (desde la raiz del repo)
set -u
ESPERADAS="main.web.services.fitsense.orchestation.RespaldoConDolorIT.conDolorSinSustitutoNoPublicaNadaInvalido
main.web.services.fitsense.orchestation.RetiradaLocalizadaPorDefectoIT.apagadaPorDefecto
main.web.services.fitsense.p2d.ConservacionPatronH1IT.reglaPorDefecto"

fallidas() {
  python3 -I - target/surefire-reports target/failsafe-reports <<'PY'
import sys, glob, xml.etree.ElementTree as ET
out = set()
for d in sys.argv[1:]:
    for f in glob.glob(d + "/TEST-*.xml"):
        for tc in ET.parse(f).getroot().iter("testcase"):
            if tc.find("failure") is not None or tc.find("error") is not None:
                out.add(tc.get("classname") + "." + tc.get("name").split("(")[0])
print("\n".join(sorted(out)))
PY
}

echo "== 1. Configuracion por defecto"
rm -rf target/surefire-reports target/failsafe-reports
sh ./mvnw -o -B -ntp verify -Pintegration > /tmp/verificar_defecto.log 2>&1
F1=$(fallidas)
if [ -n "$F1" ]; then echo "REGRESION con la configuracion por defecto:"; echo "$F1"; RES=1; else echo "OK: 0 fallos"; RES=0; fi

git checkout -q -- docs/FitSense/campana_p123 2>/dev/null || true
echo "== 2. Configuracion experimental (difficulty-guard=preferencia, pain-localized-removal=on)"
rm -rf target/surefire-reports target/failsafe-reports
sh ./mvnw -o -B -ntp verify -Pintegration -Dfitsense.adaptation.difficulty-guard=preferencia \
   -Dfitsense.adaptation.pain-localized-removal=on > /tmp/verificar_experimental.log 2>&1
F2=$(fallidas)
if [ "$F2" = "$ESPERADAS" ]; then echo "OK: fallan exactamente las 3 pruebas esperadas"; else
  echo "DIFERENCIA con lo esperado."; echo "-- fallidas:"; echo "$F2"; echo "-- esperadas:"; echo "$ESPERADAS"; RES=1; fi
git checkout -q -- docs/FitSense/campana_p123 2>/dev/null || true
exit $RES
