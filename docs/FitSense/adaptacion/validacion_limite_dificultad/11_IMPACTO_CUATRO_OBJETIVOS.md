# 11 · Impacto en los cuatro objetivos

Fuente: `05_COMPARACION_ESTRATEGIAS.csv`, el barrido de 19.562 colocaciones de los 960 perfiles:
- 3 equipamientos: gimnasio, casa con mancuernas y bandas, casa con peso corporal;
- 1, 2, 3, 4, 6 y 7 días;
- 30, 45, 60 y 75 minutos;
- 3 niveles;
- grupos con restricciones y con dolor P-02.

Se completa con la regresión con off, on y preferencia (12) y el laboratorio de los 22 perfiles (08).

> **Exposición simulada:** cada ejercicio colocado se trata como si hubiera dolido. No son incidentes.

## 1. Subidas que corrige el límite y coste, por objetivo

En la tabla, «A sube» son las colocaciones donde el sustituto de A es más difícil.

| Objetivo | Colocaciones | **A sube** | B o C sube | Sin sustituto A | Sin sustituto B o C (nuevos) | B: menor | C: menor | Perfiles reproducidos con semana sin plan en B o C (08) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GAIN_MUSCLE | 4.930 | 763 | 0 | 48 | 53 (+5) | 936 | 115 | 1 (R0545) |
| INCREASE_STRENGTH | 5.080 | 706 | 0 | 48 | 55 (+7) | 888 | 109 | 0 |
| LOSE_WEIGHT | 4.043 | 391 | 0 | 218 | 223 (+5) | 564 | 49 | 0 |
| GENERAL_FITNESS | 5.509 | 609 | 0 | 77 | 84 (+7) | 878 | 116 | 6 |

Por nivel:

| Nivel | Colocaciones | A sube | Nuevos sin sustituto |
|---|---:|---:|---:|
| Principiante | 6.366 | **0** | 0 |
| Intermedio | 6.596 | 1.302 | 13 |
| Avanzado | 6.600 | 1.167 | 11 |

Los principiantes no tienen subidas porque su elegible no supera su dificultad, así que **el límite no les cambia nada**. Todos los «nuevos sin sustituto» son el 135, en casa con mancuernas y bandas, con 6 o 7 días.

Por equipamiento:

| Equipamiento | A sube | Nuevos sin sustituto |
|---|---:|---:|
| Gimnasio | 932 | 0 |
| Casa con mancuernas y bandas | 702 | 24 |
| Casa con peso corporal | 835 | 0 |

Detalle cruzado (objetivo × nivel, objetivo × equipo, días y grupo) en el 05.

## 2. Lo que el límite no cambia (fase 7)

| Aspecto | Evidencia | Resultado |
|---|---|---|
| Generación inicial | 960 perfiles con off, on y preferencia: `planes.csv` `d109a3da…` y `colocaciones.csv` `d227848b…` | **Idénticos** a la referencia en las tres |
| Adherencia | REPLAY: `metricas.csv` sin cambios con on y preferencia; ningún cambio en `WeeklyMetric*`, `AdaptationOrderDecider` ni fórmulas | **Intacta** |
| Cobertura (4C, V23) | Sin cambios en `SlotCoverage*` ni `WeeklySlotDistribution`; sus pruebas pasan en las tres configuraciones | **Intacta** |
| Decisiones semanales | REPLAY: las decisiones de N08 (`ADAPT_LOCALIZED`) y de todos los demás no cambian; solo cambia el desenlace de N08 | **Intactas** |
| Dosis 4D | El sustituto recibe la dosis de su clase y la misma clase por `MISMO_SLOT`; TD-14 rechaza por 4D con el límite encendido | **Intacta** |
| V24 | Sin cambios de código; TD-13 y TD-14 rechazan por V24 con el límite; pruebas V24 en verde | **Intacta** |
| V20, incluidas sesiones con poco margen | La duración estimada depende solo de la dosis (`SessionDurationEstimator`), no del ejercicio: una sustitución 1:1 con la misma dosis no cambia V20. TD-13: 690 s de 1.890 s, rechazado por V20 con el límite encendido | **Intacto** |
| Permisos de adaptación | `TipoIntervencion` sin cambios; el límite solo **añade** un motivo de rechazo en VARIANTE y dolor | **Intactos**; la igualdad sigue permitida |
| Progresión y reincorporación | `ProgresionIT`, `ProgresionReservaIT`, `Reincorporacion4DTest`, `ReincorporacionTrasProgresionTest` y `HerenciaD8IT` en verde en las tres configuraciones | **Intactas** |

## 3. Por objetivo, en una frase

- **GAIN_MUSCLE:**
  - mayor exposición relativa en avanzados (354 subidas de 1.738);
  - 5 nuevos sin sustituto;
  - R0548 (75 min, 7 días, casa) no tiene plan inicial válido con ninguna estrategia (R-9).
- **INCREASE_STRENGTH:** 706 subidas evitadas; 7 nuevos sin sustituto, todos resueltos con regeneración P-03.
- **LOSE_WEIGHT:** la menor exposición (391); 5 nuevos sin sustituto, resueltos con regeneración.
- **GENERAL_FITNESS:** 609 subidas evitadas; **6 de los 7 perfiles que se quedan una semana sin plan** con el límite son GF (08). Es el objetivo donde más pesa el coste restrictivo.
