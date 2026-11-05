#!/usr/bin/env python3
"""Pruebas propias del oraculo K20.

1. Funciones puras con casos calculados a mano (redondeos, V20, duracion, adherencia, decision, pertinencia).
2. MUTACIONES: sobre una corrida real (por defecto evidencia/preparacion/P2_S) se altera un dato cada vez y el
   oraculo DEBE marcar FAIL en la fase correcta. Un oraculo que no detecta estas alteraciones no sirve.

Uso:  python3 -I -m unittest test_oraculo_k20      (desde herramientas/)
      K20_CORRIDA=<carpeta> para usar otra corrida.
"""
import copy
import csv
import os
import sys
import unittest
from decimal import Decimal

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import oraculo_k20 as O  # noqa: E402

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.dirname(AQUI)
CONGELADA = os.path.join(os.path.dirname(RAIZ), "evaluacion_final_k20")
# Solo LECTURA de la evidencia congelada (las mutaciones se hacen en memoria; no se escribe nada en ella).
CORRIDA = os.environ.get("K20_CORRIDA", os.path.join(CONGELADA, "evidencia", "final", "S_control"))
PERFILES = os.path.join(CONGELADA, "entrada", "03_PERFILES_U01_U20.csv")

P = {"warmup_minutes": 5, "seconds_per_rep": 3, "transition_seconds": 60, "default_rest_seconds": 60}


def ej(i, tipo="SETS_REPS", sets=3, reps=10, dur=None, load=None, rest=60):
    return {"id": i, "type": tipo, "sets": sets, "reps": reps, "dur": dur, "load": load, "rest": rest, "pe": i}


class Puras(unittest.TestCase):
    def test_redondeos(self):
        self.assertEqual(O.r2(Decimal("79.745")), Decimal("79.75"))
        self.assertEqual(O.r2(Decimal("66.665")), Decimal("66.67"))
        self.assertEqual(O.round_java(7.2), 7)
        self.assertEqual(O.round_java(6.5), 7)       # Java redondea .5 hacia arriba
        self.assertEqual(O.round_java(108.0), 108)

    def test_piso_v20(self):
        self.assertEqual(O.piso_v20(45, 70), 1890)
        self.assertEqual(O.piso_v20(75, 70), 3150)
        self.assertEqual(O.piso_v20(60, 70), 2520)

    def test_duracion(self):
        # 300 + (2-1)*60 + [3*10*3 + 2*60] + [2*30 + 1*90]
        e = [ej(1), ej(2, "DURATION", sets=2, reps=None, dur=30, rest=90)]
        self.assertEqual(O.segundos_estimados(e, P), 300 + 60 + (90 + 120) + (60 + 90))
        self.assertEqual(O.minutos_estimados(e, P), 12)
        self.assertEqual(O.segundos_estimados([], P), 0)

    def test_porcentaje_con_tope(self):
        self.assertEqual(O.pct_ejercicio(7, 18), Decimal("38.89"))
        self.assertEqual(O.pct_ejercicio(40, 30), Decimal("100.00"))
        self.assertEqual(O.pct_ejercicio(0, 0), Decimal("0"))

    def test_equipamiento(self):
        grupos = {"1": {"dumbbell", "band"}, "2": {"bench"}}
        self.assertTrue(O.equipo_cumple(grupos, {"band", "bench"}))
        self.assertFalse(O.equipo_cumple(grupos, {"band"}))
        self.assertTrue(O.equipo_cumple({}, set()))   # sin grupos = peso corporal

    def _s(self, i, reg, pct, motivo=None, dolor=False):
        return {"id": i, "registrada": reg, "pct": Decimal(str(pct)), "motivo": motivo, "dolor": dolor}

    def test_decision_patrones(self):
        e1 = [self._s(1, True, 100), self._s(2, True, 100), self._s(3, True, 100)]
        self.assertEqual(O.decidir(e1)[0], "NO_CHANGE")
        e3 = [self._s(1, True, 100), self._s(2, True, 100), self._s(3, True, 0, "FATIGUE")]
        d = O.decidir(e3)
        self.assertEqual((d[0], d[1]), ("ADAPT_LOCALIZED", [(3, "REVISAR_CARGA_Y_RECUPERACION")]))
        self.assertEqual(d[3], Decimal("66.67"))
        e4 = [self._s(1, True, 100), self._s(2, True, 0, "TOO_DIFFICULT"), self._s(3, True, 0, "LACK_OF_MOTIVATION")]
        d = O.decidir(e4)
        self.assertEqual(d[0], "ADAPT_EXPANDED")
        self.assertEqual(d[1], [(2, "VARIANTE_MAS_SENCILLA_O_CARGA"), (3, "INVESTIGAR_CAMBIO_MINIMO")])
        e5 = [self._s(1, True, 100), self._s(2, False, 0), self._s(3, False, 0)]
        d = O.decidir(e5)
        self.assertEqual((d[0], d[2], d[3], d[4]), ("INSUFFICIENT_DATA", Decimal("33.33"), Decimal("100.00"),
                                                     Decimal("33.33")))

    def test_decision_u07_valores_reales(self):
        # U07: S3 = 39,26 % real (P40 con dosis enteras) -> Aobs 79,75 < 80 -> localizada, causa no registrada
        s = [self._s(1, True, 100), self._s(2, True, 100), self._s(3, True, "39.26")]
        d = O.decidir(s)
        self.assertEqual(d[0], "ADAPT_LOCALIZED")
        self.assertEqual(d[1], [(3, "CAUSA_NO_REGISTRADA")])
        self.assertEqual(d[3], Decimal("79.75"))

    def test_dolor_tiene_prioridad(self):
        s = [self._s(1, True, 100), self._s(2, True, 100, dolor=True), self._s(3, False, 0)]
        self.assertEqual(O.decidir(s)[:2], ("ADAPT_LOCALIZED", [(2, "SEGURIDAD_ANTE_DOLOR")]))

    def test_comparacion_y_pertinencia(self):
        dif = {1: 2, 2: 1, 3: 3, 4: 2}
        antes = [ej(1), ej(4)]
        self.assertTrue(O.comparar_sesion(antes, [ej(1), ej(4)], dif, P)["igual"])
        mas_facil = O.comparar_sesion(antes, [ej(2), ej(4)], dif, P)
        self.assertEqual(O.pertinencia("VARIANTE_MAS_SENCILLA_O_CARGA", mas_facil, False), "ACC")
        mas_dificil = O.comparar_sesion(antes, [ej(3), ej(4)], dif, P)
        self.assertEqual(O.pertinencia("VARIANTE_MAS_SENCILLA_O_CARGA", mas_dificil, False), "INC")
        menos = O.comparar_sesion(antes, [ej(1, sets=2), ej(4)], dif, P)
        self.assertEqual(O.pertinencia("REVISAR_CARGA_Y_RECUPERACION", menos, False), "ACC")
        mas = O.comparar_sesion(antes, [ej(1, sets=4), ej(4)], dif, P)
        self.assertEqual(O.pertinencia("REVISAR_CARGA_Y_RECUPERACION", mas, False), "INC")
        self.assertEqual(O.pertinencia("INVESTIGAR_CAMBIO_MINIMO", menos, False), "ACC")
        tres = O.comparar_sesion([ej(1), ej(4), ej(2)], [ej(1, sets=2), ej(4, sets=2), ej(2, sets=2)], dif, P)
        self.assertEqual(O.pertinencia("INVESTIGAR_CAMBIO_MINIMO", tres, False), "INC")
        igual = O.comparar_sesion(antes, antes, dif, P)
        self.assertEqual(O.pertinencia("REVISAR_CARGA_Y_RECUPERACION", igual, True), "CONS-J")
        self.assertEqual(O.pertinencia("CAUSA_NO_REGISTRADA", menos, False), "INC")
        menos_descanso = O.comparar_sesion(antes, [ej(1, rest=30), ej(4)], dif, P)
        self.assertTrue(menos_descanso["aumentos"])

    def test_sustitucion_con_otro_musculo_es_como_mucho_alt(self):
        dif = {1: 2, 2: 1, 4: 2}
        O.MUSCULO.update({1: "pectorals", 2: "triceps", 4: "lats"})
        try:
            otro = O.comparar_sesion([ej(1), ej(4)], [ej(2), ej(4)], dif, P)
            self.assertEqual(O.pertinencia("VARIANTE_MAS_SENCILLA_O_CARGA", otro, False), "ALT")
            O.MUSCULO[2] = "pectorals"
            mismo = O.comparar_sesion([ej(1), ej(4)], [ej(2), ej(4)], dif, P)
            self.assertEqual(O.pertinencia("VARIANTE_MAS_SENCILLA_O_CARGA", mismo, False), "ACC")
        finally:
            for k in (1, 2, 4):
                O.MUSCULO.pop(k, None)


@unittest.skipUnless(os.path.isdir(os.path.join(CORRIDA, "tablas")), "sin corrida exportada")
class Mutaciones(unittest.TestCase):
    """Cada alteracion de los datos reales debe producir FAIL en la fase indicada (y solo cambiar ese usuario)."""

    @classmethod
    def setUpClass(cls):
        cls.base = O.Datos(CORRIDA)
        with open(PERFILES, newline="", encoding="utf-8") as f:
            cls.perfiles = {r["id"]: r for r in csv.DictReader(f)}

    def evaluar(self, D, uid):
        return O.evaluar_usuario(D, self.perfiles[uid], None)[0]

    def test_sin_mutacion_pasa(self):
        for uid in ("U01", "U04", "U08", "U14"):
            self.assertEqual(self.evaluar(copy.deepcopy(self.base), uid)["estado"], "PASS", uid)

    def _plan(self, D, uid, semana):
        return D.plan(D.usuarios[uid], semana)[0]

    def _fallo(self, det, fase_):
        self.assertEqual(det["estado"], "FAIL")
        self.assertTrue(det["comprobaciones"].get(fase_), f"esperaba fallo en {fase_}: {det['comprobaciones']}")

    def test_metrica_alterada(self):
        D = copy.deepcopy(self.base)
        m = D.metricas[(D.usuarios["U01"], O.S1.isoformat())]
        m["weighted_adherence_pct"] = str(Decimal(m["weighted_adherence_pct"]) - 1)
        self._fallo(self.evaluar(D, "U01"), "C_adherencia")

    def test_registro_alterado(self):
        D = copy.deepcopy(self.base)
        w = D.sesiones(int(self._plan(D, "U02", O.S1)["plan_id"]))[0]
        ses = [s for s in D.ses[int(w["planned_workout_id"])] if s["counts_toward_adherence"] == "true"][0]
        fila = next(iter(D.wse[int(ses["session_id"])].values()))
        campo = "actual_reps_total" if fila["actual_reps_total"] else "actual_duration_seconds"
        fila[campo] = str(int(fila[campo]) - 1)
        self._fallo(self.evaluar(D, "U02"), "B_registro")

    def test_decision_alterada(self):
        D = copy.deepcopy(self.base)
        fila = [r for r in D.v32[D.usuarios["U03"]] if r["new_week_start_date"] == O.S2.isoformat()][0]
        fila["decision"] = "NO_CHANGE"
        self._fallo(self.evaluar(D, "U03"), "D_decision")

    def test_sesion_no_senalada_tocada(self):
        D = copy.deepcopy(self.base)
        s2 = D.sesiones(int(self._plan(D, "U04", O.S2)["plan_id"]))
        e = D.pwe[int(s2[0]["planned_workout_id"])][0]   # S1 de U04 no esta senalada
        e["planned_sets"] = str(int(e["planned_sets"]) + 1)
        self._fallo(self.evaluar(D, "U04"), "E_adaptacion")

    def test_sustituto_mas_dificil(self):
        D = copy.deepcopy(self.base)
        s2 = D.sesiones(int(self._plan(D, "U04", O.S2)["plan_id"]))
        e = D.pwe[int(s2[1]["planned_workout_id"])][0]   # S2 senalada (demasiado dificil)
        actual = int(e["exercise_id"])
        dificil = next(k for k, v in D.dificultad.items() if v == 3 and D.ejercicio[k]["is_active"] == "true"
                       and D.ejercicio[k]["default_prescription"] == e["prescription_type"] and k != actual)
        e["exercise_id"] = str(dificil)
        self._fallo(self.evaluar(D, "U04"), "E_adaptacion")

    def test_aumento_de_dosis_en_senalada(self):
        D = copy.deepcopy(self.base)
        s2 = D.sesiones(int(self._plan(D, "U08", O.S2)["plan_id"]))
        e = D.pwe[int(s2[2]["planned_workout_id"])][0]
        e["planned_sets"] = str(int(e["planned_sets"]) + 1)
        self._fallo(self.evaluar(D, "U08"), "E_adaptacion")

    def test_plan_duplicado(self):
        D = copy.deepcopy(self.base)
        p = dict(self._plan(D, "U05", O.S2))
        p["plan_id"] = "999999"
        D.planes.append(p)
        self._fallo(self.evaluar(D, "U05"), "F_publicacion")

    def test_texto_incoherente(self):
        D = copy.deepcopy(self.base)
        self._plan(D, "U08", O.S2)["adjustment_reason"] = ("Nos contaste que llegabas con cansancio. En una de las "
                                                          "sesiones no aplicamos cambios esta semana: se mantiene como estaba.")
        self._fallo(self.evaluar(D, "U08"), "F_publicacion")

    def test_v20_incumplido_en_continuidad(self):
        D = copy.deepcopy(self.base)
        for w in D.sesiones(int(self._plan(D, "U11", O.S3)["plan_id"])):
            for e in D.pwe[int(w["planned_workout_id"])]:
                e["planned_sets"] = "1"
        self._fallo(self.evaluar(D, "U11"), "G_continuidad")

    def test_excluido_reaparece(self):
        D = copy.deepcopy(self.base)
        w = D.sesiones(int(self._plan(D, "U06", O.S3)["plan_id"]))[0]
        D.exclusiones.append({"user_id": str(D.usuarios["U06"]), "exercise_id": D.pwe[int(w["planned_workout_id"])][0]["exercise_id"],
                              "status": "ACTIVE"})
        det = self.evaluar(D, "U06")
        self.assertEqual(det["estado"], "FAIL")

    def test_equipamiento_no_disponible(self):
        D = copy.deepcopy(self.base)
        w = D.sesiones(int(self._plan(D, "U11", O.S1)["plan_id"]))[0]
        e = D.pwe[int(w["planned_workout_id"])][0]
        D.grupos[int(e["exercise_id"])] = {"1": {"leverage machine"}}
        self._fallo(self.evaluar(D, "U11"), "A_generacion")

    def test_idempotencia(self):
        D = copy.deepcopy(self.base)
        D.res["U01"] = dict(D.res["U01"], ciclo1_repetido_v32_nuevas="1")
        self._fallo(self.evaluar(D, "U01"), "F_publicacion")


if __name__ == "__main__":
    unittest.main()
