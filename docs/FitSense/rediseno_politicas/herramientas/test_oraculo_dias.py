#!/usr/bin/env python3
"""Pruebas del contrato de dias DIAS-1 del oraculo versionado. Uso: python3 -I test_oraculo_dias.py"""
import importlib.util
import os
import sys
import unittest

sys.dont_write_bytecode = True
AQUI = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("oraculo_politica", os.path.join(AQUI, "oraculo_k20_politica.py"))
O = importlib.util.module_from_spec(spec)
spec.loader.exec_module(O)
c = O.comprobar_dias


class ContratoDias(unittest.TestCase):

    def test_positivos(self):
        self.assertEqual([], c(4, 5, "BEGINNER", 5, False, 4, "S1"), "L05: principiante con 5 dias -> 4")
        self.assertEqual([], c(4, 7, "BEGINNER", 7, False, 4, "S1"))
        self.assertEqual([], c(6, 7, "INTERMEDIATE", 7, False, 6, "S1"))
        self.assertEqual([], c(6, 7, "ADVANCED", 7, False, 6, "S1"))
        self.assertEqual([], c(3, 3, "ADVANCED", 7, False, 3, "S1"), "frecuencia pedida menor que la disponibilidad")
        self.assertEqual([], c(2, 4, "INTERMEDIATE", 4, True, 2, "S2"), "REDUCE_DAYS justifica menos sesiones")
        self.assertEqual([], c(1, 1, "BEGINNER", 1, False, None, "S1"))

    def test_negativos(self):
        self.assertTrue(any("superan el tope" in p for p in c(5, 5, "BEGINNER", 5, False, 5, "S1")))
        self.assertTrue(any("superan el tope" in p for p in c(7, 7, "ADVANCED", 7, False, 7, "S1")))
        self.assertTrue(any("espera 3" in p for p in c(4, 3, "ADVANCED", 7, False, 4, "S1")))
        self.assertTrue(any("sin justificacion" in p for p in c(3, 5, "BEGINNER", 5, False, 3, "S1")))
        self.assertTrue(any("sin justificacion" in p for p in c(0, 2, "BEGINNER", 2, True, 0, "S1")),
                        "REDUCE_DAYS no justifica una semana vacia")
        self.assertTrue(any("planned_days_count" in p for p in c(4, 4, "BEGINNER", 4, False, 3, "S1")))


if __name__ == "__main__":
    unittest.main()
