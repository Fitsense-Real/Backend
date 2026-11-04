# 06 · Reproducibilidad

La **huella normalizada** del oráculo es el SHA-256 de una representación canónica de cada usuario: planes S1/S2/S3 con sesiones, ejercicios, dosis y descansos; ejecución registrada; métricas; filas V32 y textos. Se calcula sin ids sustitutos ni marcas de tiempo; los ids se sustituyen por (semana, posición).

| Corrida | Tipo | SHA | Huella normalizada |
|---|---|---|---|
| **D** | Evaluación congelada | `a31c36b` | `f9f67973759d60cd9aa47d5a8b14d7dfcaa4d546108def255277ad97e0c15c2e` |
| **D′** | Evaluación congelada | `a31c36b` | **`f9f67973…` (idéntica)** |
| P3_D | Preparación | `99a02da` | `f9f67973…` (idéntica) |
| S_control | Control (IA simulada) | `a31c36b` | `c0eab17e4366f779ad5426f0006379751105afa8b06d69c4e82b42653f9055ec` |
| P2_S, P4_S, P5_S, P6_S | Preparación | — | `c0eab17e…` (idénticas; cambiaron las propuestas inválidas, que no se publican) |

**Conclusiones:**
- **D = D′**, byte a byte en la representación canónica.
- El `userId` de cada usuario y la semilla del motor (`userId × 1.000.003 + epochDay`) son los mismos en todas las corridas: U01 = 2 … U20 = 21.
- Los cambios del arnés entre P3 y la congelación no alteraron lo que se publica.

## Control de regresión de la generación (960 perfiles) y REPLAY de 100 semanas

Sobre `7ee69c2`, con el mismo `src/main` que el SHA congelado:

| Configuración | 960 perfiles · `planes.csv` / `colocaciones.csv` | REPLAY 100 semanas |
|---|---|---|
| Por defecto (off/off) | `d109a3da…` / `d227848b…` = **referencia** | Solo marcas de tiempo de `exclusiones_dolor.csv` |
| Candidata (preferencia/on) | **idénticos** | Solo N08: el respaldo pone el **1003** (dif. 1) en lugar del 124 (dif. 2) de la IA. Es **idéntica**, sin marcas de tiempo, a la de la configuración D del PR #39. P3 no altera la REPLAY |

Evidencia: `evidencia/regresion/960_replay_resumen.txt`, `replay_*.diff` y `k20_960.sh`.
