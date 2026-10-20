import json,glob
for f in sorted(glob.glob('top-issues-*.json')):
    r=f[len('top-issues-'):-5]
    print('###',r)
    for kind in ('top','recent'):
        d=json.load(open(f'{kind}-issues-{r}.json'))
        print(' ',kind,'total',d.get('total_count'))
        for i in d.get('items',[]): print('   #%s'%i['number'],i['state'][0],i['created_at'][:10],'c=%d'%i['comments'],'r=%d'%i['reactions']['total_count'],i['title'][:90])
