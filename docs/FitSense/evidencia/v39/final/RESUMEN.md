# V38 + V39 oficiales con E1/E2 final: resultados de la simulacion

Directorio Flyway oficial (`src/main/resources/db/migration`), motor de la rama `catalogo-v37-1`.

| Medida | Resultado |
|---|---|
| 72 referencias validas | 72/72 (`../../progresion/generacion_referencia.txt`) |
| Robustez (288) | 288/288 |
| Barrido 4-6 dias con plan valido | 72/72 |
| Barrido: cobertura de las capacidades del objetivo | 72/72 COMPLETO (ninguna capacidad principal perdida) |
| 192 restricciones publicadas | 192/192 |
| 192 con equipo ausente segun GIF | 0 |
| 192 con violaciones en el plan | 0 |
| Continuidad (96 perfiles, IT) | fallos en el informe JUnit: 0 |

## Reproduccion exacta candidata = oficial

- Exportacion del catalogo (1413 lineas) tras `docs/.../v37/separables + v38/candidata` y tras `db/migration` con V38 + V39: **identica** (sha256 `cf30b8bb…8fd0`).
- Referencias, barrido, robustez y 192 restricciones: identicos entre las dos ejecuciones.
- Las migraciones oficiales solo difieren de las candidatas en la cabecera, los mensajes y el nombre de los objetos temporales.
