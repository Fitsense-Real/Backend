"""Reaplica el piso de V20 (redondeado frente a exacto) a las propuestas REALES guardadas de Replicate.
Solo mide la duracion con la formula del estimador (calentamiento 300 s, 3 s/rep, transicion 60 s);
no llama a nada. Uso: replay_v20.py <llamadas.jsonl>..."""
import json, re, sys, math, collections
def seg(w):
    ex = w.get("exercises") or []
    if not ex: return 0
    t = 300 + 60 * (len(ex) - 1)
    for e in ex:
        s = e.get("planned_sets") or 1
        if e.get("prescription_type") == "DURATION": t += s * (e.get("planned_duration_seconds") or 0)
        else: t += s * (e.get("planned_reps") or 0) * 3
        t += max(0, s - 1) * (e.get("rest_seconds") if e.get("rest_seconds") is not None else 60)
    return t
tot = collections.Counter(); casos = []
for f in sys.argv[1:]:
    for l in open(f):
        r = json.loads(l)
        if not r.get("salida"): continue
        try: o = json.loads(r["salida"])
        except Exception: continue
        m = re.search(r"/(\d+)/", r["etiqueta"])
        if not m or not (o.get("workouts") or o.get("sessions")): continue
        minutos = int(m.group(1)); piso_ent = minutos * 70 // 100; piso_exacto = math.ceil(minutos * 60 * 0.7)
        tot["propuestas_" + r["tipo"]] += 1
        nuevas = []
        for w in (o.get("workouts") or o.get("sessions")):
            s = seg(w)
            if s == 0: continue
            tot["sesiones"] += 1
            redondeado_ok = max(1, round(s / 60)) >= piso_ent
            exacto_ok = s >= piso_exacto
            if redondeado_ok and not exacto_ok: nuevas.append((w.get("scheduled_date"), s, piso_exacto))
        if nuevas:
            tot["propuestas_con_rechazo_nuevo_" + r["tipo"]] += 1
            tot["sesiones_rechazo_nuevo"] += len(nuevas)
            casos.append((f.split("/")[-2], r["etiqueta"], r["usuario"], r["intento"], nuevas))
for k, v in sorted(tot.items()): print(k, v)
for c in casos: print(*c)
