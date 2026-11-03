import sys, re, collections
C = "id src active name_en name_es bp target syn sec eq reqs diff presc pattern iso hi floor axial kneel hang over inv gif ien ies".split()
def load(p):
    r = {}
    for l in open(p):
        f = l.rstrip("\n").split("\t"); d = dict(zip(C, f)); r[int(d["id"])] = d
    return r
cat = load(sys.argv[1])
CASA = {"body weight", "dumbbell", "band"}
def equipo_ok(d):
    if not d["reqs"]: return True
    grupos = collections.defaultdict(set)
    for x in d["reqs"].split(","):
        g, code = x.split(":", 1); grupos[g].add(code)
    return all(v & CASA for v in grupos.values())
def motivos(d):
    m = []
    if d["active"] != "t": m.append("inactivo")
    if int(d["diff"]) > 1: m.append("dificultad %s" % d["diff"])
    if d["hi"] == "t": m.append("alto impacto")
    if not equipo_ok(d): m.append("equipo " + d["reqs"])
    if d["presc"] != "DURATION": m.append("prescripcion " + d["presc"])
    return m
loc = [d for d in cat.values() if d["pattern"] == "LOCOMOTION_CARDIO"]
print("LOCOMOTION_CARDIO total", len(loc), "activos", sum(1 for d in loc if d["active"] == "t"))
ok = [d for d in loc if not motivos(d)]
print("prescribibles principiante casa:", [(d["id"], d["name_en"]) for d in ok])
cnt = collections.Counter(tuple(motivos(d)) for d in loc if motivos(d) and d["active"] == "t")
for k, v in cnt.most_common(): print(v, k)
print("--- activos LOCOMOTION_CARDIO bloqueados solo por 1 motivo")
for d in sorted(loc, key=lambda d: int(d["id"])):
    m = motivos(d)
    if d["active"] == "t" and len(m) == 1:
        print(d["id"], d["name_en"], "|", d["bp"], d["eq"], d["reqs"], "diff", d["diff"], d["presc"], "hi", d["hi"], "floor", d["floor"], "kneel", d["kneel"], "|", m[0])
print("--- no LOCOMOTION_CARDIO con nombre de cardio, activos, dificultad<=2")
pat = re.compile(r"jack|march|high knee|run|jog|skater|skip|shadow box|boxing|step ?up|butt kick|burpee|mountain climber|climber|stepping|walk|jump|hop|punch|squat thrust|star|lunge jump|knee ?up|fast feet|bear crawl|crawl", re.I)
for d in sorted(cat.values(), key=lambda d: int(d["id"])):
    if d["pattern"] != "LOCOMOTION_CARDIO" and d["active"] == "t" and int(d["diff"]) <= 2 and pat.search(d["name_en"]):
        print(d["id"], d["name_en"], "|", d["bp"], d["pattern"], d["eq"], d["reqs"], "diff", d["diff"], d["presc"], "hi", d["hi"], "|", motivos(d))
