# 09 · Resultados de las pruebas

**Fecha:** 27/11/2026. **Entorno:** contenedor efímero con Maven 3.9.11, OpenJDK 21.0.13, Docker 29.9.3 y `postgres:16-alpine` (Testcontainers).
**Cero llamadas a Replicate.** Las pruebas con IA usan un servidor Replicate **local** o *mocks*. En el entorno no hay `REPLICATE_API_TOKEN` y el `pom` fuerza `fitsense.ai.enabled=false` salvo en las pruebas que apuntan al servidor local.

## 1. Regresión completa de la COMBINACIÓN de las PR

Las ramas B1, B2, B3, B5 y B6 se fusionaron en una rama local **temporal y sin subir** (`tmp/combinada`, `cb05354`, sobre `generation` `77b9df3`). La fusión fue limpia, y después se ejecutó `mvn -B verify -Pintegration`.

| Fase | Ejecutadas | Aprobadas | Fallidas | Errores | Omitidas |
|---|---|---|---|---|---|
| Unitarias (surefire) | 1.679 | 1.678 | 0 | 0 | 1 |
| Integración (failsafe) | 927 | 855 | 0 | 0 | 72 |
| **Total** | **2.606** | **2.533** | **0** | **0** | **73** |

**BUILD SUCCESS**, 31 min 22 s.

**Comparación con la auditoría:**
- unitarias: 1.666 → 1.679 (+13);
- integración: 887 → 927 (+40);
- **omitidas: 73 → 73.** Son las mismas pruebas *opt-in*: Replicate real, réplica P2-D, Neon y laboratorios, cada una con su propiedad. Ninguna prueba existente se desactivó ni se omitió.

**Pruebas nuevas por PR:**

| PR | Unitarias | Integración |
|---|---|---|
| B1 #90 | `DetalleSensibleTest`, `GuardasDeSeguridadEnProduccionTest` | `LogsSinDatosSensiblesIT` (5), `LimitesDeAutenticacionIT` (7), `ProxyDeConfianzaLimitesIT`, `ProxyDeConfianzaRailwayIT` |
| B2 #91 | `DocumentosLegalesTest` | `ConsentimientoExigidoIT` (reescrita, 5), `ParticipacionOpcionAIT`, `ParticipacionOpcionBIT` (2) |
| B3 #92 | — | `CierreSemanalRecuperableIT` (8), `PublicacionConcurrenteIT` (4) |
| B5 #93 | `AdminExportControllerTest` | `OperacionYExportacionIT` (6) |
| B6 #94 | — | `Estudio12SemanasIT` (1, con 18 escenarios) |

**Pruebas existentes cuyo valor esperado cambió a propósito:**

| Prueba | Antes | Ahora | Por qué |
|---|---|---|---|
| `ConsentimientoYEdadIT.registroDeParticipacion` | `CASILLA_REGISTRO` | `CASILLA_SIN_DOCUMENTO_APROBADO` | La orden pide que `MVP-1.0` no pase por prueba válida |
| `ConsentimientoExigidoIT` | — | Reescrita | Con la exigencia encendida se exige también el consentimiento informado publicado |

En los dos casos es el comportamiento nuevo pedido, no una prueba relajada para ocultar un fallo. Además, `BaseDeProduccionBloqueada` lee el host protegido de un recurso de pruebas, porque la configuración ya no lo lleva (P1-04).

## 2. Pruebas de alcance por PR (en local, antes de la combinación)

| PR | Ejecución | Resultado |
|---|---|---|
| B1 | 43 unitarias y 41 IT (B1 más `DespliegueHttpsIT`, `ContratoErroresApiIT`, `RecuperacionPorCorreoIT`, `ConsentimientoYEdadIT`, `EliminacionWebIT` y unitarias de Replicate) | 84/84. Un fallo de configuración en una prueba nueva (`max-per-hour` > 100) se corrigió |
| B2 | 2 unitarias y 61 IT (más `SeguridadRolesIT`, `EliminacionDeCuentaIT`, `CicloCompletoHttpIT`, `MigracionCuentasV60aV63IT`, `DespliegueHttpsIT`) | 63/63 |
| B3 | 20 unitarias y 184 IT (más `CicloConcurrenteIT`, `ValidacionMultisemanaIT`, `CopiaSinIaIT`, `ProteccionPlanesPublicadosIT`, `GeneracionAsincronaResilienciaIT`…) | 2 fallos en la primera ejecución, corregidos: se revirtió el *pool* del planificador y se arregló la prueba nueva. Después, 31/31 |
| B3 + #89 | Fusión local con la ampliación V59: `OperacionesIaV66IT`, `MigracionV65aV66IT`, `CicloConcurrenteIT` y las dos IT de B3 | **37/37** |
| B5 | Unitarias y 118 IT (más `SeguridadRolesIT`, `Adherencia*IT`, `GeneracionAsincronaResilienciaIT`, `ReanudacionConReplicateSimuladoIT`, `Gpt5MiniConO20IT`…) | 1 fallo real corregido (`solicitudesSimultaneas`: el límite se consumía antes de resolver la carrera). Después, 41/41 |
| B6 | `Estudio12SemanasIT` | 1/1 (97 s). **Prueba inversa:** sin la recuperación de B3, falla (`evidencia/prueba_inversa_sin_recuperacion.txt`) |
| B4 | `verificar_respaldo.sh` contra dos PostgreSQL sintéticos | OK en los 4 casos (`evidencia/ensayo_respaldo_sintetico.txt`) |

## 3. CI de GitHub (`verify`, `mvn -B -ntp verify -Pintegration`)

| PR | Commit | CI |
|---|---|---|
| #90 B1 | `afe2282` | **success** |
| #91 B2 | `13b17cd` | **success** |
| #92 B3 | `240ccca` | **success** (el commit anterior, `6802691`, quedó cancelado por el *push* siguiente: `cancel-in-progress`) |
| #93 B5 | `cb3a51b` | **success** |
| #94 B6 | `aff80cf` | **success** |
| #95 B4 | `0ecfafb` y este commit | en curso al escribir este documento (solo documentación y un script) |

## 4. Otras verificaciones de la orden (§10)

| Verificación | Resultado |
|---|---|
| Swagger y OpenAPI | Los endpoints nuevos llevan `@Operation`: `/legal-documents`, `/admin/operations`, `/admin/exports/weekly-metrics`, y las descripciones de 429 en login y registro. Swagger sigue cerrado en `prod` |
| Migraciones | **Ninguna nueva.** V66 sigue siendo de #89. La combinación aplica V1–V65 igual que antes |
| Contratos de Mobile | Compatibles hacia atrás con la configuración por omisión (`03` §6, `05` §4, `02` §6) |
| Seguridad | `SeguridadRolesIT`, `DespliegueHttpsIT`, `ContratoErroresApiIT` y las IT nuevas de B1, en verde |
| Concurrencia | `CicloConcurrenteIT`, `CierreSemanalRecuperableIT.dosInstancias`, `PublicacionConcurrenteIT`, `GeneracionAsincronaResilienciaIT.solicitudesSimultaneas` y E12-12 |
| Generación y adaptación | Suites existentes en verde. *Prompts* y validadores sin cambios |
| O20/V65 | `DolorFamiliaEquivalenteIT`, `MigracionV65IT`, `Gpt5MiniConO20IT` y E12-05, en verde |
| V58/V59 | `GeneracionAsincrona*`, `ReanudacionConReplicateSimuladoIT` y `MigracionV58aV59IT`, en verde. E12-11 sobre el libro |
| Integridad de datos | Invariantes semanales de B6: un plan activo, una medición, ninguna predicción ajena, ningún ejercicio inactivo ni excluido |

## 5. Lo que estas pruebas no cubren

- **IA real:** ninguna prueba nueva llama a Replicate.
- **Entrega real** de correo y de push.
- **Railway, Neon y Firebase reales.**
- **Una restauración de la base real.**
- **Un reinicio real de la JVM** a mitad del ciclo: lo cubre #89 con un segundo contexto de Spring, no con un proceso muerto.
