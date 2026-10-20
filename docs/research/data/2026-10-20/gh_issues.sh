for r in JackFred2/ChestTracker ponuing/ChestTracker JackFred2/WhereIsIt YaLTeR/MouseTweaks sakura-ryoko/litematica Snownee/Jade MisterPeModder/ShulkerBoxTooltip TerraformersMC/ModMenu jaredlll08/Controlling tom5454/Toms-Storage EngineHub/WorldEdit mezz/JustEnoughItems; do
  f="issues-$(echo $r | tr / _).json"
  gh api -X GET search/issues -f q="repo:$r is:issue" -f sort=comments -f order=desc -f per_page=12 > top-$f 2>/dev/null
  gh api -X GET search/issues -f q="repo:$r is:issue created:>=2026-01-01" -f sort=created -f order=desc -f per_page=8 > recent-$f 2>/dev/null
  gh api repos/$r --jq '{stars:.stargazers_count,open_issues:.open_issues_count,pushed:.pushed_at,default:.default_branch}' 2>/dev/null > meta-$f
  echo "### $r $(cat meta-$f)"
  python3 -c "
import json,sys
for kind in ('top','recent'):
  d=json.load(open('$kind-$f'))
  print(' ',kind,'total',d.get('total_count'))
  for i in d.get('items',[]): print('   ',i['number'],i['state'],i['created_at'][:10],'c=%d'%i['comments'],'r=%d'%i['reactions']['total_count'],i['title'][:95])
"
done
