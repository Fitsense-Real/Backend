# 06 · Continuidad multisemana

## Laboratorio de 21 perfiles, dos semanas (05)

En el laboratorio, cada perfil pasa por esta secuencia:
1. semana medida con dolor en el 135;
2. ciclo;
3. semana nueva ejecutada entera, sin dolor;
4. otro ciclo.

Si la semana nueva no tenía plan, se ejecuta igualmente el ciclo siguiente.

| Comprobación | A | B | C | D | D con B |
|---|---|---|---|---|---|
| El 135 reaparece (semana nueva o siguiente) | 0 | 0 | 0 | 0 | 0 |
| Más de un plan activo en una semana | 0 | 0 | 0 | 0 | 0 |
| Semana siguiente con plan | 21/21 | 21/21 | 21/21 | 21/21 | 21/21 |
| Semana siguiente tras una retirada | — | — | — | 3/3 `NO_CHANGE/SIN_ORDEN`, sin el 135, con el resto conservado | ídem |
| Semana siguiente tras una semana sin plan | — | 7/7 plan nuevo generado por el ciclo, sin el 135 | ídem | 5/5 | 5/5 |
| Reintentos sin fin | no | no | no | no | no |

Las invariantes del laboratorio son aserciones: un fallo haría fallar la prueba. Las cinco corridas pasaron (`evidencia/lab22_*_pruebas.txt`).

## Pruebas permanentes (04)

`RetiradaLocalizadaLimiteIT` y `RetiradaLocalizadaPreferenciaIT`, perfil R0521:
- **semana nueva:** retirada publicada;
- **ciclo repetido:** el mismo plan, un solo activo;
- **semana siguiente** ejecutada y nuevo ciclo: plan activo, sin el 135 y un único activo.

## Linaje en continuidad

- Tras una retirada, la semana siguiente deriva de la publicada, que conservaba el linaje 6/6.
- Las sesiones que no se tocaron siguen siendo copias.
- La sesión retirada sigue sin el 135, porque la exclusión P-02 persiste hasta una reactivación explícita.

## Semana del dolor y semana posterior, por configuración

| Configuración | Semana del dolor (7 perfiles) | Semana siguiente |
|---|---|---|
| B / C | 7 sin plan | plan nuevo del ciclo (sin V32) |
| D | 2 con la semana medida sin el 135; 5 sin plan | 2 en continuidad; 5 con plan nuevo del ciclo |
