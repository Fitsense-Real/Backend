"""Descarga (GET, gratis) las predicciones de Replicate de una corrida a <corrida>/predicciones_replicate/.

Uso: python3 -I descargar_predicciones.py <carpeta_corrida>

- Los ids salen de las lineas REPLICATE_TIMING de <corrida>/ejecucion_maven.log.
- Ademas lista /v1/predictions y anota las creadas dentro de la ventana de la corrida que no esten en el registro
  (predicciones iniciadas cuya respuesta se perdio): tambien se descargan y se marcan en el indice.
- La credencial la inyecta el proxy del entorno: este script no lee, imprime ni guarda ninguna.
"""
import json
import os
import re
import ssl
import sys
import urllib.request
from datetime import datetime, timedelta, timezone

API = "https://api.replicate.com/v1"


def abrir():
    ctx = ssl.create_default_context(cafile=os.environ.get("SSL_CERT_FILE") or "/root/.ccr/ca-bundle.crt")
    proxy = os.environ.get("HTTPS_PROXY") or os.environ.get("https_proxy")
    handlers = [urllib.request.HTTPSHandler(context=ctx)]
    if proxy:
        handlers.append(urllib.request.ProxyHandler({"https": proxy}))
    return urllib.request.build_opener(*handlers)


def get(op, url):
    with op.open(urllib.request.Request(url, headers={"Accept": "application/json", "User-Agent": "fitsense-laboratorio/1.0"}), timeout=60) as r:
        return json.loads(r.read().decode())


def main(carpeta):
    log = open(os.path.join(carpeta, "ejecucion_maven.log"), encoding="utf-8", errors="replace").read()
    ids = list(dict.fromkeys(re.findall(r"REPLICATE_TIMING .*?prediction_id=(\w+)", log)))
    cfg = open(os.path.join(carpeta, "00_configuracion.txt"), encoding="utf-8").read()
    inicio = datetime.fromisoformat(re.search(r"fecha_ejecucion_utc: (\S+)", cfg).group(1).replace("Z", "+00:00"))
    fin = datetime.fromtimestamp(os.path.getmtime(os.path.join(carpeta, "99_resumen.txt")), tz=timezone.utc)
    op = abrir()
    en_ventana, url = [], API + "/predictions"
    while url:
        page = get(op, url)
        sigue = True
        for p in page.get("results", []):
            c = datetime.fromisoformat(p["created_at"].replace("Z", "+00:00"))
            if c < inicio - timedelta(minutes=1):
                sigue = False
                break
            if c <= fin + timedelta(minutes=1):
                en_ventana.append(p["id"])
        url = page.get("next") if sigue else None
    destino = os.path.join(carpeta, "predicciones_replicate")
    os.makedirs(destino, exist_ok=True)
    indice = []
    for pid in list(dict.fromkeys(ids + en_ventana)):
        p = get(op, f"{API}/predictions/{pid}")
        p.pop("urls", None)
        with open(os.path.join(destino, pid + ".json"), "w", encoding="utf-8") as f:
            json.dump(p, f, ensure_ascii=False, indent=1)
        m = p.get("metrics") or {}
        indice.append({"id": pid, "en_registro": pid in ids, "en_ventana": pid in en_ventana,
                       "estado": p.get("status"), "creada": p.get("created_at"),
                       "tokens_entrada": m.get("token_input_count"), "tokens_salida": m.get("token_output_count"),
                       "predict_time": m.get("predict_time")})
    with open(os.path.join(destino, "indice.json"), "w", encoding="utf-8") as f:
        json.dump({"ventana_utc": [inicio.isoformat(), fin.isoformat()], "predicciones": indice}, f, indent=1)
    print(f"{len(indice)} predicciones ({len(ids)} en el registro, {len(en_ventana)} en la ventana)")


if __name__ == "__main__":
    main(sys.argv[1])
