#!/usr/bin/env bash
# =====================================================================================================================
# FitSense · B4.2 (P0-06) · COPIA VERIFICABLE Y RESTAURACION EN UNA BASE AISLADA
#
#   scripts/respaldo/verificar_respaldo.sh ORIGEN_URL DESTINO_URL [DIRECTORIO_SALIDA]
#
#   ORIGEN_URL   base de la que se hace la copia (preproduccion). Se lee en modo SOLO LECTURA
#                (default_transaction_read_only=on): el script no puede escribir en ella.
#   DESTINO_URL  base AISLADA y VACIA donde se restaura (una base nueva, nunca la de produccion ni la de origen).
#   Formato de las URL: postgresql://usuario@host:puerto/base?sslmode=require (la clave, en PGPASSWORD o ~/.pgpass).
#
# Que hace:
#   1. pg_dump --format=custom del origen, con su SHA-256.
#   2. Comprueba que el destino es otra base y que esta VACIA (ninguna tabla en public): si no, se detiene.
#   3. pg_restore en el destino y mide el tiempo real de recuperacion.
#   4. Compara, tabla por tabla, el numero de filas del origen y del destino, y la ultima version de Flyway.
#   5. Escribe un informe (informe.txt) y termina con codigo 0 solo si todo coincide.
#
# Que NO hace: no restaura nunca sobre una base con datos; no modifica el origen; no borra copias; no sube nada.
# Requisitos: bash, pg_dump/pg_restore/psql de la misma version mayor que el servidor (16), sha256sum.
# =====================================================================================================================
set -euo pipefail

ORIGEN="${1:?Uso: verificar_respaldo.sh ORIGEN_URL DESTINO_URL [DIRECTORIO_SALIDA]}"
DESTINO="${2:?Uso: verificar_respaldo.sh ORIGEN_URL DESTINO_URL [DIRECTORIO_SALIDA]}"
SALIDA="${3:-respaldo-$(date -u +%Y%m%dT%H%M%SZ)}"

host_y_base() { # quita usuario, clave y parametros: host:puerto/base
  local u="${1#*://}"; u="${u#*@}"; echo "${u%%\?*}"
}
if [[ "$(host_y_base "$ORIGEN")" == "$(host_y_base "$DESTINO")" ]]; then
  echo "ABORTADO: el destino es la misma base que el origen." >&2; exit 2
fi

leer_origen() { PGOPTIONS='-c default_transaction_read_only=on' psql "$ORIGEN" -X -A -t -v ON_ERROR_STOP=1 -c "$1"; }
leer_destino() { psql "$DESTINO" -X -A -t -v ON_ERROR_STOP=1 -c "$1"; }

TABLAS_DESTINO=$(leer_destino "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'")
if [[ "$TABLAS_DESTINO" != "0" ]]; then
  echo "ABORTADO: la base de destino no esta vacia ($TABLAS_DESTINO tablas en public). Usa una base nueva y aislada." >&2
  exit 3
fi

mkdir -p "$SALIDA"
COPIA="$SALIDA/fitsense.dump"
INFORME="$SALIDA/informe.txt"

echo "1/4 Copia del origen (solo lectura)..."
T0=$(date +%s)
PGOPTIONS='-c default_transaction_read_only=on' pg_dump "$ORIGEN" --format=custom --no-owner --no-acl --file="$COPIA"
T1=$(date +%s)
HUELLA=$(sha256sum "$COPIA" | cut -d' ' -f1)
TAMANO=$(stat -c %s "$COPIA")

echo "2/4 Restauracion en la base aislada..."
pg_restore --dbname="$DESTINO" --no-owner --no-acl --exit-on-error "$COPIA"
T2=$(date +%s)

echo "3/4 Comparacion de filas por tabla..."
TABLAS=$(leer_origen "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'
                      AND table_type = 'BASE TABLE' ORDER BY table_name")
DIFERENCIAS=0
{
  echo "FitSense · verificacion de copia y restauracion"
  echo "fecha_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "origen=$(host_y_base "$ORIGEN")"
  echo "destino=$(host_y_base "$DESTINO")"
  echo "copia=$COPIA bytes=$TAMANO sha256=$HUELLA"
  echo "segundos_copia=$((T1 - T0)) segundos_restauracion=$((T2 - T1)) segundos_total=$((T2 - T0))"
  echo
  printf '%-45s %12s %12s %s\n' tabla origen destino estado
} > "$INFORME"
for t in $TABLAS; do
  o=$(leer_origen "SELECT count(*) FROM public.\"$t\"")
  d=$(leer_destino "SELECT count(*) FROM public.\"$t\"")
  estado=OK; [[ "$o" == "$d" ]] || { estado=DISTINTO; DIFERENCIAS=$((DIFERENCIAS + 1)); }
  printf '%-45s %12s %12s %s\n' "$t" "$o" "$d" "$estado" >> "$INFORME"
done

echo "4/4 Version de Flyway..."
if leer_origen "SELECT 1 FROM information_schema.tables WHERE table_name = 'flyway_schema_history'" | grep -q 1; then
  FO=$(leer_origen "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1")
  FD=$(leer_destino "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1")
  echo "flyway_origen=$FO flyway_destino=$FD" >> "$INFORME"
  [[ "$FO" == "$FD" ]] || DIFERENCIAS=$((DIFERENCIAS + 1))
fi

echo >> "$INFORME"
if [[ $DIFERENCIAS -eq 0 ]]; then
  echo "RESULTADO=OK (todas las tablas coinciden)" >> "$INFORME"
  cat "$INFORME"; exit 0
else
  echo "RESULTADO=FALLO ($DIFERENCIAS diferencias)" >> "$INFORME"
  cat "$INFORME"; exit 1
fi
