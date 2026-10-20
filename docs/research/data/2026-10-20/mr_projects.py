import json,urllib.request,urllib.parse,datetime
UA={'User-Agent':'stow-redesign-research/1.0 (github.com/undrwtrsprite/stow)'}
def get(u): return json.load(urllib.request.urlopen(urllib.request.Request(u,headers=UA),timeout=40))
slugs="""mouse-tweaks inventory-profiles-next item-scroller clientsort inventory-management inventory-essentials mouse-wheelie sorted
chest-tracker chest-tracker-port where-is-it where-is-it-port storage-tracker toms-storage ae2 storagedrawers sophisticated-backpacks
litematica material-hud worldedit shulkerboxtooltip jade appleskin modmenu yacl cloth-config controlling searchables
inventoryhudplus minihud betterf3 ukus-armor-hud xaeros-minimap jei emi rei tweakeroo quark""".split()
projs=get('https://api.modrinth.com/v2/projects?'+urllib.parse.urlencode({'ids':json.dumps(slugs)}))
by={p['slug']:p for p in projs}
print('missing',[s for s in slugs if s not in by])
out={'fetched_utc':datetime.datetime.now(datetime.UTC).isoformat(),'projects':[]}
for s in slugs:
    p=by.get(s)
    if not p: continue
    vs=get(f"https://api.modrinth.com/v2/project/{p['id']}/version")
    f263=[v for v in vs if '26.3' in v['game_versions'] and 'fabric' in v['loaders']]
    fab=[v for v in vs if 'fabric' in v['loaders']]
    any263=[v for v in vs if '26.3' in v['game_versions']]
    def vi(v): return {'version_number':v['version_number'],'date':v['date_published'][:10],'loaders':v['loaders'],'game_versions':v['game_versions'][-4:],'downloads':v['downloads'],'type':v['version_type'],'id':v['id']}
    rec={'slug':s,'title':p['title'],'id':p['id'],'url':f"https://modrinth.com/mod/{s}",'downloads_alltime':p['downloads'],'followers':p['followers'],
      'published':p['published'][:10],'updated':p['updated'][:10],'license':p['license']['id'],'source':p.get('source_url'),'issues':p.get('issues_url'),'wiki':p.get('wiki_url'),
      'loaders':p['loaders'],'n_versions':len(vs),'latest_any':vi(vs[0]) if vs else None,
      'fabric_263':[vi(v) for v in f263[:3]],'latest_fabric':vi(fab[0]) if fab else None,'other_263_loaders':sorted({l for v in any263 for l in v['loaders']}),
      'gallery':len(p.get('gallery') or []),'description':p['description']}
    out['projects'].append(rec)
    f=rec['fabric_263'][0] if rec['fabric_263'] else None
    print(f"{s:<26}{p['downloads']:>12,} upd {rec['updated']} fab26.3: {(f['version_number']+' '+f['date']+' '+f['type']) if f else 'NONE; latest fabric '+(rec['latest_fabric']['version_number']+' '+str(rec['latest_fabric']['game_versions']) if rec['latest_fabric'] else '-')} | 26.3 loaders {rec['other_263_loaders']}")
json.dump(out,open('modrinth-projects-2026-10-20.json','w'),indent=1)
