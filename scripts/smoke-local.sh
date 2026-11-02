#!/usr/bin/env bash
# Prueba de humo de la API con un cliente HTTP independiente (curl), contra un
# backend LOCAL (perfil «local»). No usa Flutter. Crea un usuario nuevo con un
# correo de dominio reservado (.invalid) y recorre la primera ruta del MVP.
#
#   ./scripts/smoke-local.sh [URL_BASE]      (por defecto http://localhost:8080/api/v1)
#
# Requisitos: bash, curl y python3. Sale con codigo != 0 al primer fallo.
set -euo pipefail
API="${1:-http://localhost:8080/api/v1}"
case "$API" in
  http://localhost*|http://127.0.0.1*) ;;
  *) echo "Solo contra un backend local (localhost/127.0.0.1): $API" >&2; exit 2 ;;
esac
EMAIL="smoke-$(date +%s)@pruebas.invalid"
CLAVE="ClaveSegura123"
TMP="$(mktemp)"; trap 'rm -f "$TMP"' EXIT
pasos=0

llamar() { # metodo ruta codigo_esperado [cuerpo] [token]
  local m="$1" r="$2" esperado="$3" cuerpo="${4:-}" token="${5:-}"
  local args=(-s -o "$TMP" -w "%{http_code}" -X "$m" "$API$r" -H "Content-Type: application/json")
  [[ -n "$token" ]] && args+=(-H "Authorization: Bearer $token")
  [[ -n "$cuerpo" ]] && args+=(-d "$cuerpo")
  local codigo; codigo="$(curl "${args[@]}")"
  if [[ "$codigo" != "$esperado" ]]; then
    echo "FALLO $m $r: esperado $esperado y fue $codigo"; cat "$TMP"; echo; exit 1
  fi
  pasos=$((pasos + 1)); echo "ok  $m $r -> $codigo"
}
campo() { python3 -c "import json,sys; d=json.load(open('$TMP')); print(eval(sys.argv[1]))" "$1"; }

llamar POST /auth/register 201 "{\"email\":\"$EMAIL\",\"password\":\"$CLAVE\",\"firstName\":\"Smoke\",\"lastName\":\"Local\",\"timezone\":\"America/Lima\",\"acceptsStudyParticipation\":true}"
llamar POST /auth/login 200 "{\"email\":\"$EMAIL\",\"password\":\"$CLAVE\"}"
TOKEN="$(campo "d['token']")"
llamar GET /users/me 200 "" "$TOKEN"
llamar GET /users/me/profile 404 "" "$TOKEN"
PERFIL='"birthDate":"1994-03-01","biologicalSex":"FEMALE","heightCm":165,"currentWeightKg":62,"fitnessLevel":"BEGINNER","goalType":"GENERAL_FITNESS","goalText":"Smoke","trainingLocation":"HOME","daysPerWeek":3,"availableDays":[1,3,5],"equipmentCodes":["body weight","dumbbell"]'
llamar POST /users/me/profile 422 "{$PERFIL,\"sessionMinutes\":15}" "$TOKEN"
echo "    rango del backend: $(campo "d['message']")"
llamar POST /users/me/profile 201 "{$PERFIL,\"sessionMinutes\":45}" "$TOKEN"
llamar GET /catalog/equipment-types 200 "" "$TOKEN"
llamar POST /users/me/plan/generate 201 "" "$TOKEN"
llamar GET /users/me/plan/current 200 "" "$TOKEN"
W="$(campo "d['workouts'][0]['plannedWorkoutId']")"
PE="$(campo "d['workouts'][0]['exercises'][0]['plannedExerciseId']")"
W2="$(campo "d['workouts'][1]['plannedWorkoutId']")"
llamar GET /users/me/plans 200 "" "$TOKEN"
llamar GET /users/me/plan/adaptations 200 "" "$TOKEN"
llamar POST "/workouts/$W/start" 201 "" "$TOKEN"
S="$(campo "d['sessionId']")"
llamar GET /sessions/current 200 "" "$TOKEN"
llamar PUT "/sessions/$S/exercises/$PE" 200 '{"actualSets":0,"skipReason":"PAIN_OR_DISCOMFORT"}' "$TOKEN"
llamar POST "/sessions/$S/finish" 200 '{"sessionRpe":6,"satisfaction":4}' "$TOKEN"
llamar GET /sessions/current 204 "" "$TOKEN"
llamar POST "/workouts/$W2/skip" 204 '{"reason":"LACK_OF_TIME","lackOfTimeContext":"IMPREVISTO"}' "$TOKEN"
llamar GET /users/me/metrics/weekly 200 "" "$TOKEN"
llamar GET /users/me/pain-exclusions 200 "" "$TOKEN"
llamar GET /users/me 401 "" "token-invalido"
echo "Prueba de humo correcta: $pasos llamadas contra $API (usuario $EMAIL)."
