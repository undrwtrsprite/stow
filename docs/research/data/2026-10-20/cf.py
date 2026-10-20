import json,urllib.request,datetime,sys,time
UA={'User-Agent':'stow-redesign-research/1.0'}
cands={'mouse-tweaks':['mouse-tweaks'],'inventory-profiles-next':['inventory-profiles-next'],'item-scroller':['item-scroller'],'clientsort':['clientsort','client-sort'],
'inventory-management':['inventory-management'],'inventory-essentials':['inventory-essentials'],'mouse-wheelie':['mouse-wheelie'],'sorted':['sorted'],
'chest-tracker':['chest-tracker'],'where-is-it':['where-is-it'],'toms-storage':['toms-storage','toms-simple-storage-mod'],'ae2':['applied-energistics-2'],
'storagedrawers':['storage-drawers'],'sophisticated-backpacks':['sophisticated-backpacks'],'litematica':['litematica'],'material-hud':['material-hud','litematica-material-hud'],'worldedit':['worldedit'],
'shulkerboxtooltip':['shulkerboxtooltip','shulker-box-tooltip'],'jade':['jade'],'appleskin':['appleskin'],'modmenu':['modmenu'],'yacl':['yacl'],'cloth-config':['cloth-config'],
'controlling':['controlling'],'searchables':['searchables'],'inventoryhudplus':['inventory-hud-forge','inventoryhud'],'minihud':['minihud'],'betterf3':['betterf3'],
'ukus-armor-hud':['ukus-armor-hud','ukus-armor-hud-fabric'],'xaeros-minimap':['xaeros-minimap'],'jei':['jei'],'emi':['emi'],'rei':['roughly-enough-items'],'tweakeroo':['tweakeroo'],'quark':['quark']}
out={'fetched_utc':datetime.datetime.now(datetime.UTC).isoformat(),'source':'https://api.cfwidget.com/minecraft/mc-mods/<slug> (third-party mirror of CurseForge public data)','projects':{}}
for k,slugs in cands.items():
    rec=None
    for s in slugs:
        u='https://api.cfwidget.com/minecraft/mc-mods/'+s
        try:
            d=json.load(urllib.request.urlopen(urllib.request.Request(u,headers=UA),timeout=40))
        except Exception as e:
            rec={'slug':s,'error':str(e)};continue
        if 'downloads' not in d: rec={'slug':s,'error':d.get('error') or 'no downloads','raw':str(d)[:200]};continue
        files=d.get('files',[])
        f263=[f for f in files if '26.3' in f.get('versions',[]) and 'Fabric' in f.get('versions',[])]
        a263=[f for f in files if '26.3' in f.get('versions',[])]
        rec={'slug':s,'cf_id':d.get('id'),'title':d.get('title'),'url':d.get('urls',{}).get('curseforge'),'downloads_total':d['downloads'].get('total'),'downloads_monthly_field':d['downloads'].get('monthly'),
             'last_fetch':d.get('last_fetch'),'created':d.get('created_at'),
             'fabric_263':[{'name':f['name'],'display':f.get('display'),'uploaded':f.get('uploaded_at'),'type':f.get('type'),'downloads':f.get('downloads')} for f in f263[:3]],
             'any_263_loaders':sorted({v for f in a263 for v in f.get('versions',[]) if v in('Fabric','NeoForge','Forge','Quilt')})}
        break
    out['projects'][k]=rec
    print(k, rec.get('slug'), rec.get('downloads_total', rec.get('error')), rec.get('last_fetch'), (rec.get('fabric_263') or [{}])[0].get('display'), rec.get('any_263_loaders'))
json.dump(out,open('curseforge-cfwidget-2026-10-20.json','w'),indent=1)
