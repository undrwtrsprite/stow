import json,urllib.request,urllib.parse,datetime
UA={'User-Agent':'stow-redesign-research/1.0 (github.com/undrwtrsprite/stow)'}
def get(url):
    return json.load(urllib.request.urlopen(urllib.request.Request(url,headers=UA),timeout=30))
queries={'inventory':'inventory','sort':'sort','storage':'storage','chest':'chest','schematic':'schematic','building':'building','tooltip':'tooltip','preview':'preview','hud':'hud','config':'config','minimap':'minimap','recipe':'recipe','search':'search','waypoint':'waypoint','shulker':'shulker','backpack':'backpack','keybind':'keybind','durability':'durability'}
out={'fetched_utc':datetime.datetime.utcnow().isoformat()+'Z','queries':{}}
for k,q in queries.items():
    facets=json.dumps([["project_type:mod"]])
    url='https://api.modrinth.com/v2/search?'+urllib.parse.urlencode({'query':q,'index':'downloads','limit':15,'facets':facets})
    d=get(url)
    out['queries'][k]={'url':url,'hits':[{k2:h[k2] for k2 in ('slug','title','downloads','follows','categories','versions','date_modified','project_id')} for h in d['hits']]}
    print('##',k)
    for h in d['hits']:
        print(f"  {h['downloads']:>12,} {h['slug']:<32} 26.3={'26.3' in h['versions']} {','.join(c for c in h['categories'] if c in('fabric','neoforge','forge','quilt'))}")
json.dump(out,open('modrinth-search-2026-10-20.json','w'),indent=1)
