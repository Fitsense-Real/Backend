# Tablas GRA (generación inicial + adaptación con Replicate real)

## Generación inicial con Replicate (agregado)

| Corrida | Perfiles | Factibles (con llamada) | Sin plan (esperado) | Llamadas de generación | Válidas al 1.er intento | Propuestas rechazadas | Errores del proveedor | Planes publicados desde la IA | Planes por respaldo | Motivos de rechazo |
|---|---|---|---|---|---|---|---|---|---|---|
| GRA-R1 | 21 | 20 | 1 | 30 | 13 | 10 | 0 | 20 | 0 | V15×6; V23×2; V24×1; V26×6; V4×1 |
| GRA-R2 | 21 | 20 | 1 | 26 | 14 | 6 | 0 | 20 | 0 | V13×1; V15×4; V18×1; V26×5 |

## Adaptación con Replicate (agregado)

| Corrida | Ciclos con orden de IA | Aceptadas al 1.er intento | Aceptadas tras reintento | Intentos rechazados | SIN_EFECTO propuesto por la IA | Respaldo | Publicadas desde la IA | ACC/ALT/CONS-J/INC |
|---|---|---|---|---|---|---|---|---|
| GRA-R1 | 10 | 10 | 0 | 0 | 0 | 0 | 10/10 | 11/0/1/0 |
| GRA-R2 | 10 | 9 | 0 | 0 | 1 | 0 | 9/10 | 9/1/2/0 |

## Diez aspectos (20 perfiles; O11-F35 aparte)

| Corrida | APROBADO | FALLO | NO APLICA | REQUIERE REVISIÓN | Aprobación técnica |
|---|---|---|---|---|---|
| GRA-R1 | 150 | 0 | 44 | 6 | 150/150 = 100.0 % |
| GRA-R2 | 148 | 0 | 45 | 7 | 148/148 = 100.0 % |

## Rendimiento y costes por etapa

| Corrida | Etapa | Predicciones | Tokens entrada | Tokens salida | Latencia mediana (s) | Latencia máx. (s) | USD |
|---|---|---|---|---|---|---|---|
| GRA-R1 | GENERACION | 30 | 1048225 | 220508 | 75.5 | 145.2 | 0.7031 |
| GRA-R1 | ADAPTACION | 10 | 334742 | 26491 | 31.9 | 44.3 | 0.1367 |
| GRA-R2 | GENERACION | 26 | 893012 | 171309 | 66.6 | 148.0 | 0.5659 |
| GRA-R2 | ADAPTACION | 10 | 334492 | 23778 | 28.7 | 47.7 | 0.1312 |

