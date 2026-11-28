# 09 · Adaptaciones correctas por causa

**Categorías del oráculo** (`02` §4):
- **ACC:** adaptación correcta.
- **ALT:** alternativa aceptable.
- **CONS-J:** conservación justificada.
- **CONS-POL:** conservación que la política permite habiendo una modificación factible.
- **INC:** incorrecta.

| Causa (intervención) | Sesiones | D: desenlace | Control S (IA simulada): desenlace | Qué es correcto |
|---|---:|---|---|---|
| Fatiga (REVISAR_CARGA_Y_RECUPERACION) | 4 (S3 de E3) | 4 RESPALDO-COPIA · CONS-J | U03: CONS-J (no factible). U08, U13, U18: IA · **ACC** (repeticiones o series menos, o 5–10 s menos) | Bajar el trabajo o la carga sin añadir nada |
| Demasiado difícil (VARIANTE_MAS_SENCILLA_O_CARGA) | 4 (S2 de E4) | 4 RESPALDO-COPIA · CONS-J | U04, U09: IA · **ACC** (sustituto de dificultad 1 en lugar de 2). U14, U19: IA · **ACC** (dosis menor; no hay nada más fácil) | Variante de menor dificultad o menos dosis. Nunca más difícil |
| Desmotivación (INVESTIGAR_CAMBIO_MINIMO) | 4 (S3 de E4) | 4 RESPALDO-COPIA · CONS-J | U04: CONS-J (no factible). U09, U14, U19: IA · **ACC** (un solo cambio de dosis) | Cambio mínimo: como mucho 1 ejercicio, sin subir la dosis |
| Parcial sin motivo (CAUSA_NO_REGISTRADA) | 2 (S3 de U07 y U12) | SIN_EFECTO · CONS-J | Igual (sin llamada) | Conservar: la política no autoriza cambios sin causa |
| **Total** | **14** | **0 adaptaciones · 14 CONS-J · 0 INC** | **10 ACC (IA) · 4 CONS-J · 0 INC · 0 CONS-POL** | |

## U07 y U12

**Qué pasa.** Con la regla P40 congelada (cada ejercicio al 40 % redondeado), la S3 real queda al 39,26 % en U07 y al 38,33 % en U12. Los totales son Aw = Aobs = **79,75** y **79,44**, por debajo de 80.

**Qué decide la política.** Sobre esos valores reales, la política vigente decide **ADAPT_LOCALIZED** sobre S3, con intervención CAUSA_NO_REGISTRADA (parcial sin motivo), y conserva sin llamar a la IA (SIN_EFECTO, `CONSERVACION_DELIBERADA`). El backend hace exactamente eso: **PASS**.

**Frente al oráculo teórico.** El oráculo teórico histórico esperaba NO_CHANGE porque suponía exactamente el 40 %. La diferencia es de **entrada** (dosis enteras), no de cálculo ni de decisión.

**Qué no se hace.** No se modificaron los porcentajes ni la regla. El artículo debe decir que la decisión usa Aobs, con valores reales, y que P40 no es exacto con dosis enteras.

U02 y U17 dan el 40 % exacto y deciden NO_CHANGE. Coinciden con el teórico.
