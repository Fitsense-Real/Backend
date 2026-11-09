#!/usr/bin/env python3
"""MEJ ronda 2 · MATRIZ_REGLAS_Y_VALIDADORES.xlsx (Componente | Politica vigente | Implementacion | Prueba | Resultado)."""
import sys
from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill

G = "Generacion inicial"
A = "Adaptacion"
P = "Progresion / recuperacion / reincorporacion"
I = "Prompts e instrumento"
FILAS = [
    (G, "Frecuencia", "min(dias, 4 BEGINNER / 6 resto); fechas en dias disponibles", "PlanGenerationContext.effectiveDaysPerWeek; WeeklySplitPlanner", "V2, V3, V12 (PlanDraftValidator.validateExercisesAndDays); FrecuenciaSemanal*Test", "Correcto"),
    (G, "Division y recuperacion", "Mismo enfoque / mismos grupos no en dias seguidos", "WeeklySplitPlanner.focusesFor", "V13, V18", "Correcto (el enfoque de la IA no tiene que seguir suggested_split: observacion)"),
    (G, "Cobertura del enfoque (V15)", "Minimo 2 grupos prescribibles; mitad con >= 3 grupos y >= 4 de trabajo", "PlanDraftValidator.validateFocusCoverage, prescribibleParaV15", "V15; pruebas de V15", "Validador correcto. PROMPT: max_per_body_part=null con REV2 (H-G2); PR-1.8/pr-1.8a lo corrigen"),
    (G, "Slots 4C (V23)", "GM, IS, GF (no LW); solo a la IA; sin mirar dosis", "validateWeeklySlotCoverage; GoalFeasibilityPolicy.exigeSlotsResistidos", "V23", "Correcto. H-G5 (GF sin texto), H-G9 (GF 1x30 infactible, fuera de cohorte), H-G10 (exigible sobre conjunto completo)"),
    (G, "Dosis GM (V24 4D)", "Clases fijas; tope de colocaciones; presupuesto 85 %; sin serie extra con REV2", "validateMvpDose; MvpDosePolicy.ClaseDosis", "V24; ProgresionV24V8CTest.generacionRechazaDosisNoExacta", "Correcto. Ningun prompt puede subir series fijadas (V24 rechaza)"),
    (G, "Dosis IS/LW/GF (V24 capacidades)", "Dosis exacta de una capacidad admisible; <= 8 colocaciones", "validateCapabilityDose; CapabilityPrescriptionPolicy.dosis", "V24", "Correcto. H-G6 (reglas genericas del prompt contradicen), H-G8 (hasta 3 series sin V20), H-G12 (pista de reintento GM)"),
    (G, "Autorizaciones en generacion inicial", "P-A1, progresion, reduccion y herencia NO aplican", "validate(3 args) -> ConservationReport.vacio()", "ProgresionV24V8CTest.generacionSinPermisos; RedisenoAdaptacionTest.generacionIntacta", "Correcto (V24 intacto en los 4 objetivos)"),
    (G, "Capacidades principales (V25)", "Semanales; LW CARDIO por sesion; IS slots con STRENGTH", "validateCapabilityCoverage, cardioPorSesion, slotsConFuerza", "V25", "Correcto. H-G7: el prompt exige por sesion en GF/IS (solo prompt)"),
    (G, "Volumen por grupo (V26)", "6/10 por sesion; 12/20/24 por semana; ESTR+RESIST sin complementos", "validateWeeklyVolume; DosisSemanalPolicy", "V26; PoliticaRedisenoTest.v26*", "Correcto. H-G3 (no en PR-1.7). H-G16 (relajacion por datos previos)"),
    (G, "PULL real (V27)", "PULL con traccion si la hay", "validateRealPull", "V27", "Correcto. H-G4 (no se comunica en ningun prompt)"),
    (G, "Duracion maxima (V4)", "Segundos estimados <= M x 60", "validateSessions; PlanningPolicy.excedeTecho", "V4", "Correcto. H-G1: regla 4 del prompt dice +15 %"),
    (G, "Duracion minima (V20)", "Sin piso (P-G1)", "minimumSessionMinutes", "V20 inactivo", "Correcto"),
    (G, "Declarado vs contenido (V17)", "Tolerancia 20 %", "validatePrescriptionRanges; PlanDraftNormalizer", "V17", "Redundante (H-G14): el normalizador fija lo declarado"),
    (G, "Repeticiones, descanso, series", "min_reps por zona/nivel, max 30; descanso <= 180; >= 2 series; >= 20 s", "validatePrescriptionRanges; validatePrescription", "V7, V9, V10, V16, V21", "Correcto"),
    (G, "Restricciones y elegibles", "Seguridad, equipamiento, dificultad, bloqueos, dolor, filtro funcional", "ExternalCatalogService; CatalogEligibilityPolicy", "V1, V6", "Correcto"),
    (G, "Calidad de sesion (CalidadDosis)", "INSUFICIENTE (<3 ejercicios o <6 series) solo informa", "CalidadDosis.evaluar -> PlanningPolicyTrace", "Ninguno (traza); oraculo QINS usa el mismo umbral", "Correcto por diseno. O10 (2/6) valido: observacion de calidad"),
    (G, "Ejercicio repetido en una sesion", "Sin regla", "-", "-", "Observacion (no se crea regla)"),
    (A, "Motivo y evidencia", "Dolor manda; cobertura >= 50 %", "SessionOutcomeIdentifier; EvidenceRule", "EvidenceRuleTest; AdaptationOrderDeciderTest", "Correcto"),
    (A, "Decision (dolor primero, localizada/ampliada)", "Dolor aunque la adherencia sea buena; <moderado todas; si no, la peor", "AdaptationOrderDecider.decide", "AdaptationOrderDeciderTest.*", "Correcto"),
    (A, "Conservacion de no senaladas", "Identicas; ninguna desaparece", "ConservationValidator.revisarNoSenalada, revisarParticion", "ConservationValidatorTest.noSenalada*", "Correcto"),
    (A, "La IA no sube dosis ni carga", "Solo el backend progresa", "revisarDosisDeLaIa, revisarCarga", "DosisIaV8CTest, CargaV8CTest", "Correcto"),
    (A, "Descanso inmutable", "'El descanso no se cambia nunca' (prompt)", "revisarDosisDeLaIa; revisarSenalada", "ninguna", "DEFECTO H-A2 (V8C acepta cambio solo de descanso); vigilado por identidad en la ronda 2"),
    (A, "ACORTAR: una retirada de relleno", "Exactamente una sin MandatorySlot, resto intacto; tope M", "ConservationValidator.revisarSustitucion; revisar (tope M)", "ConservationValidatorQuitarRellenoTest (10)", "Correcto"),
    (A, "VARIANTE (dificultad)", "Variante del mismo patron o carga; impacto no sube; NO retirar", "revisarSustitucion, revisarMulticonjunto", "V8CConservacionPatronH1Test; RedisenoAdaptacionTest.varianteNoSubeImpacto", "DEFECTO H-A1 (retirada pura aceptada); vigilado por identidad en la ronda 2"),
    (A, "Fatiga (P-A1)", "Solo bajar; GM solo mantenimiento 3x10->2x10", "revisarDosisDeLaIa; ProgresionDosisPolicy", "RedisenoAdaptacionTest; RedisenoFatigaGmIT", "Correcto (sin cota propia fuera de GM: observacion)"),
    (A, "Desmotivacion (P-A2)", "Una sustitucion lateral estricta, sin dosis", "revisarSustitucionPorDesmotivacion", "RedisenoAdaptacionTest.sustitucion*", "Correcto (varias bajadas aceptadas: observacion)"),
    (A, "Dolor identificado / de sesion (O12)", "Lo que dolio no vuelve; no mas trabajo", "revisarDolor; rama dolor; PainMemoryService", "ConservationValidatorTest.dolor*; DolorSinMasTrabajoV8CTest; RespaldoConDolorIT", "Tecnicamente correcto; pendiente de revision profesional"),
    (A, "SIN_EFECTO y respaldo", "Sin cambios = sin efecto; 3 rechazos = respaldo", "AdaptedWeekProducer; AdaptedWeekCommandService.corregir", "AdaptedWeekProducerTest; RespaldoConDolorIT; CopiaSinIaRev2IT", "Correcto"),
    (P, "Progresion: 2 semanas al 100 %", "Cumplimiento real 100 % dos semanas", "ProgresionResolver.dosSemanasPerfectas", "ProgresionCienPorCienIT; ProgresionIT", "Correcto"),
    (P, "Techo 1,10x y paso <= 5 %", "Acumulado sobre V_base del linaje", "ProgresionResolver (TECHO=1.10)", "ProgresionReservaIT; ProgresionFactibilidadIT", "Correcto"),
    (P, "Un paso por ciclo", "A lo sumo una modificacion", "AdaptedWeekCommandService.continuidad; pasoEstrecho", "ProgresionV24V8CTest.unaModificacionPorCiclo", "Correcto"),
    (P, "Recuperacion", "<= 10 %, hacia la referencia sin superarla", "ProgresionResolver.recuperacion", "RedisenoFatigaGmIT; ProgresionRev2IT", "Correcto (p02 falla por fecha, no por la regla)"),
    (P, "Reincorporacion", "Solo tras retirada ACORTAR y >= 1 progresion; dosis inicial; dentro de techo y M", "ProgresionResolver.retirado/progresionesTrasAcortar/exacta", "ReincorporacionTrasProgresionTest; Reincorporacion4DTest", "Correcto"),
    (P, "Sin restauracion automatica", "Exclusion por dolor no caduca; NO_CHANGE no progresa", "PainMemoryService; AdaptationOrderDecider", "pruebas de P-02", "Correcto"),
    (I, "Traza de version de prompt", "Debe registrarse la enviada", "PlanInputSnapshot.of; ReplicateTrainingPlanGenerator", "-", "H-G11: variantes registradas como PR-1.7 (la ronda 2 lo registra en el arnes)"),
    (I, "Factibilidad V2/V2.1 (GM+ACORTAR)", "Debe describir lo que V8C acepta", "FactibilidadAdaptacion", "MejoraIaPromptsTest.v21SinOpcionDeRetirar", "Incoherente (corregido en V2.2 / v2.2a)"),
    (I, "Factibilidad V2.2: tope M", "[cabe] solo si respeta M", "FactibilidadAdaptacion (conTope)", "MejoraIaPromptsTest.r2RetiradaRespetaElTope", "H-A3: corregido solo en v2.2a/v2.4; V2.2 historica intacta"),
    (I, "Oraculo v1.4 (emparejamiento)", "Instrumento", "oraculo_k20.comparar_sesion", "pruebas_oraculo_v1_3/v1_4", "Artefacto posicional (O04); no se modifica"),
    (I, "Regresion", "Todo verde salvo pruebas dependientes de fecha", "-", "1316 unitarias + 593 IT", "3 fallos por LocalDate.now() (faketime 05/11 los pasa); identicos con y sin las variantes"),
]


def main(salida):
    wb = Workbook()
    ws = wb.active
    ws.title = "Matriz"
    cab = ["Bloque", "Componente", "Politica vigente", "Implementacion", "Prueba", "Resultado"]
    ws.append(cab)
    for c in ws[1]:
        c.font = Font(bold=True, color="FFFFFF")
        c.fill = PatternFill("solid", fgColor="305496")
    for f in FILAS:
        ws.append(list(f))
        r = ws.max_row
        res = f[5]
        color = "FCE4D6" if res.startswith("DEFECTO") else ("FFF2CC" if ("H-" in res or "Observacion" in res
                                                                        or "Incoherente" in res) else "E2EFDA")
        ws.cell(r, 6).fill = PatternFill("solid", fgColor=color)
    for col, w in zip("ABCDEF", (22, 32, 48, 48, 44, 60)):
        ws.column_dimensions[col].width = w
    for row in ws.iter_rows(min_row=2):
        for c in row:
            c.alignment = Alignment(wrap_text=True, vertical="top")
    ws.freeze_panes = "A2"
    ley = wb.create_sheet("Leyenda")
    for l in (["Color", "Significado"], ["Verde", "Correcto"], ["Amarillo", "Correcto con hallazgo de prompt, observacion o incoherencia documentada"],
              ["Naranja", "Defecto de regla documentado (no corregido por orden; vigilado en la ronda 2)"],
              ["Fuente", "AUDITORIA_REGLAS_FITSENSE.md y evidencia/AUDITORIA_*_detalle.md (commit 42d9dc1, PLAN-REV-2.0)"]):
        ley.append(l)
    wb.save(salida)


if __name__ == "__main__":
    main(sys.argv[1])
