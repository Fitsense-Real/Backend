#!/usr/bin/env python3
"""Pruebas sinteticas (positivas y negativas) de la pertinencia de evaluar_rev2.py. Uso: python3 -I test_evaluar_rev2.py"""
import importlib.util, json, os, sys, unittest
sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location("ev", os.path.join(os.path.dirname(os.path.abspath(__file__)), "evaluar_rev2.py"))
E = importlib.util.module_from_spec(spec); spec.loader.exec_module(E)
CAT = {1: dict(grupo="chest", patron="HORIZONTAL_PUSH", impacto=False, activo=True, dif=2),
       2: dict(grupo="chest", patron="HORIZONTAL_PUSH", impacto=False, activo=True, dif=1),
       3: dict(grupo="chest", patron="HORIZONTAL_PUSH", impacto=False, activo=True, dif=3),
       4: dict(grupo="chest", patron="HORIZONTAL_PUSH", impacto=True, activo=True, dif=1),
       5: dict(grupo="back", patron="HORIZONTAL_PULL", impacto=False, activo=True, dif=1),
       6: dict(grupo="chest", patron="HORIZONTAL_PUSH", impacto=False, activo=False, dif=1),
       7: dict(grupo="chest", patron="HORIZONTAL_PUSH", impacto=False, activo=True, dif=1),
       8: dict(grupo="chest", patron="HORIZONTAL_PUSH", impacto=False, activo=True, dif=1),
       775: dict(grupo="back", patron="HORIZONTAL_PUSH", impacto=False, activo=True, dif=1)}


def fila(interv, su=(), re_=(), cat="ACC"):
    return {"intervencion": interv, "categoria": cat, "sustituciones": json.dumps([dict(de=a, a=b) for a, b in su]),
            "reducciones": json.dumps([list(x) for x in re_])}


class Pertinencia(unittest.TestCase):
    def c(self, r): return E.pertinencia(r, CAT)[0]

    def test_positivos(self):
        self.assertEqual("PERTINENTE", self.c(fila("VARIANTE_MAS_SENCILLA_O_CARGA", su=[(1, 2)])))
        self.assertEqual("PERTINENTE", self.c(fila("VARIANTE_MAS_SENCILLA_O_CARGA", re_=[(1, "sets", "3", "2")])))
        self.assertEqual("PERTINENTE", self.c(fila("VARIANTE_MAS_SENCILLA_O_CARGA", su=[(2, 7)], cat="ALT")), "ya en dificultad 1")
        self.assertEqual("PERTINENTE", self.c(fila("REVISAR_CARGA_Y_RECUPERACION", re_=[(1, "sets", "3", "2")])))
        self.assertEqual("PERTINENTE", self.c(fila("INVESTIGAR_CAMBIO_MINIMO", su=[(2, 7), (7, 8)])))

    def test_negativos(self):
        self.assertEqual("NO_PERTINENTE", self.c(fila("INVESTIGAR_CAMBIO_MINIMO", su=[(1, 3)])), "sube dificultad")
        self.assertEqual("NO_PERTINENTE", self.c(fila("INVESTIGAR_CAMBIO_MINIMO", su=[(2, 4)])), "sube impacto")
        self.assertEqual("NO_PERTINENTE", self.c(fila("INVESTIGAR_CAMBIO_MINIMO", su=[(2, 5)])), "cambia patron y grupo")
        self.assertEqual("NO_PERTINENTE", self.c(fila("INVESTIGAR_CAMBIO_MINIMO", su=[(2, 6)])), "sustituto inactivo")
        self.assertEqual("NO_PERTINENTE", self.c(fila("REVISAR_CARGA_Y_RECUPERACION", re_=[(1, "sets", "2", "3")])), "sube dosis")
        self.assertEqual("NO_PERTINENTE", self.c(fila("VARIANTE_MAS_SENCILLA_O_CARGA", su=[(1, 1)])), "dificil, pudiendo bajar")
        self.assertEqual("NO_PERTINENTE", self.c(fila("INVESTIGAR_CAMBIO_MINIMO", su=[(2, 7), (7, 8), (8, 2)])), "> 2 cambios")
        self.assertEqual("NO_PERTINENTE", self.c(fila("INVESTIGAR_CAMBIO_MINIMO", su=[(2, 7)], cat="INC")), "oraculo INC")

    def test_catalogo(self):
        self.assertEqual("REQUIERE_REVISION", self.c(fila("REVISAR_CARGA_Y_RECUPERACION", re_=[(775, "sets", "3", "2")])))
        self.assertEqual("NO_PERTINENTE", self.c(fila("INVESTIGAR_CAMBIO_MINIMO", su=[(775, 3)])), "un fallo no se oculta como revision")


if __name__ == "__main__":
    unittest.main()
