# Flujo conjunto con Replicate real · ronda I1

**Fecha:** 10/11/2026 · **Código:** `ab04e67`, el mismo de toda la confirmación · **Modelo:** la misma versión en todas las corridas, comprobada antes y después de cada bloque.

## 1. Qué se probó

Una ronda sobre los mismos 20 perfiles y guiones de la cohorte O, con IA real en las dos etapas:

1. **Generación inicial con IA** (Replicate) y prompt de generación **PR-1.7**.
2. **Validación** con los validadores reales (V1–V27, V24 y V25 incluidos).
3. **Publicación experimental** del plan, sea de la IA o el respaldo si la IA agota sus 3 intentos.
4. **Adherencia simulada** del guion de cada perfil: cumplimiento, omisiones, dolor, fatiga, dificultad, falta de tiempo…
5. **Adaptación con IA** usando el prompt **v2.2a**.
6. **V8C corregido** (H-A1 y H-A2) y los demás validadores de adaptación.
7. **Progresión y reincorporación** en los perfiles del bloque P (O15–O20).

**Por qué PR-1.7 y no pr-1.8c:** la regla congelada (§3.3 del protocolo) pide usar pr-1.8c, o pr-1.8a si cumple el §6.1, o PR-1.7 si ninguna lo cumple. pr-1.8c no lo cumple, y pr-1.8a es «favorable no concluyente», que no cuenta como cumplir. Ver `RECOMENDACION_FINAL_ADOPCION.md`.

### Alcance exacto (no es una prueba integral completa)
- **Sí cubre:**
  - las cuatro semanas medidas y los ciclos del guion de cada perfil;
  - progresión y reincorporación dentro de ese horizonte (bloque P).
- **No cubre:**
  - el horizonte de 26 semanas;
  - usuarios reales, la app móvil, Neon ni ningún despliegue;
  - variabilidad entre rondas: es **una sola ronda**, sin repetición;
  - pr-1.8c ni pr-1.8a dentro del flujo conjunto.
- La adherencia es la del guion; no es comportamiento humano real.

## 2. Resultados

### Generación inicial (19 perfiles factibles; O11 es la inviabilidad esperada, sin llamada)

| Medida | Valor |
|---|---|
| Plan válido al primer intento | 12/19 |
| Propuestas totales / rechazadas | 28 / 10 |
| Rechazos por código | V15 11 · V4 3 · V23 2 · V26 2 · **V24 0** |
| Plan publicado de la IA / respaldo | 18 / 1 (O08: 3 rechazos V15, respaldo por rechazos agotados, como prevé el backend) |
| Planes publicados con alguna violación | 0 |
| Estado de dosis | 17 DOSIS_LIMITADA · 2 DOSIS_OBJETIVO |
| Coste / latencia mediana | USD 0,64 / 89 s |

Los resultados son los mismos que con PR-1.7 en las rondas C1–C3 (de 10 a 12 válidos de 19 por ronda). Generar con IA antes de adaptar no cambia nada en la generación.

### Adaptación con v2.2a sobre los planes generados por la IA

| Sesión | Intervención | Resultado | Pertinencia | Qué hizo la IA |
|---|---|---|---|---|
| O03 c1 | Fatiga (revisar carga) | APLICADA | ACC | Baja series y repeticiones; trabajo 288 → 234 |
| O04 c1 | Variante más sencilla | APLICADA | ACC | Sustituye 518 → 119 (mismo patrón) y baja series; 696 → 484 |
| O05 c1 (3 sesiones) | Investigar cambio mínimo / causa no registrada | APLICADA | ACC, ACC, CONS-J | Sustituye 1096 → 494 y 1254 → 294; la tercera se conserva |
| O06 c1 | Dolor | APLICADA | ACC | Retira 755 sin reemplazo (lo permite el dolor); 774 → 720 |
| O12 c1 | Dolor | APLICADA | ACC | Cinco sustituciones; 684 → 466 |
| O14 c2 | Fatiga | APLICADA | ACC | Baja series; 636 → 456 |
| O17 c1 | Fatiga | APLICADA | ACC | Baja series, repeticiones y segundos; 546 → 424 |
| O18 c1 | Acortar | APLICADA | ACC | Retira **una** de relleno (424); 438 → 360 |
| O19 c1 | Acortar | APLICADA | ACC | Baja series de tres ejercicios; 828 → 672 |
| O20 c1 | Dolor | APLICADA | ACC | Retira 1105; 384 → 306 |

- **12/12 aplicadas**, 0 SIN_EFECTO, 0 INC, **0 cambios no autorizados** y 0 rechazos de V8C.
- Ningún descanso cambió, ninguna retirada fuera de lo permitido y ninguna dosis subió.

### Oráculo independiente (v1.4), 20 perfiles × 10 aspectos

| Aspecto | Aprobado | Fallo | No aplica | Requiere revisión |
|---|---|---|---|---|
| A1 generación | 12 | 0 | 0 | 8 |
| A2 personalización y restricciones | 20 | 0 | 0 | 0 |
| A3 registro de ejecución | 19 | 0 | 1 | 0 |
| A4 adherencia | 19 | 0 | 1 | 0 |
| A5 adaptación con IA | 10 | 0 | 10 | 0 |
| A6 mantenimiento | 19 | 0 | 1 | 0 |
| A7 progresión | 8 | 0 | 12 | 0 |
| A8 reincorporación | 3 | 0 | 17 | 0 |
| A9 continuidad | 19 | 0 | 1 | 0 |
| A10 seguridad e integridad | 19 | 0 | 1 | 0 |

- **0 fallos**: aprobación técnica 148/148 sobre los aspectos aplicables.
- Los 8 «requiere revisión» de A1 son avisos de catálogo, no fallos del plan: el nombre o las instrucciones de 853 y 1320, y la dosis de espalda u hombro con ejercicios no demostrados. Son los mismos que en las corridas deterministas.

## 3. Respuesta

**¿Funcionan juntas las piezas?** Sí, dentro del alcance indicado.
- El plan inicial generado por la IA pasa por los mismos validadores que el del motor de reglas.
- La adaptación con v2.2a y el V8C corregido actúan sobre él sin errores.
- La progresión y la reincorporación siguen funcionando.

**Límites:**
- una sola ronda;
- el horizonte de las semanas medidas;
- la generación usó PR-1.7, que sigue siendo la vigente.

Combinar pr-1.8a con v2.2a no se probó en el flujo conjunto: el protocolo solo lo permitía si pr-1.8a cumplía el §6.1.

**Coste del flujo conjunto:** USD 0,78 (38 predicciones), de los 1,50 asignados.
