package dev.stow.client.memory;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

/** Conservative plain-text parsing. No project changes happen while resolving a description. */
public final class MaterialListImporter {
    public static final int MAX_TEXT=65536;
    public static final int MAX_LINES=2048;
    public record Item(String id,String name,int stackSize,List<String> aliases) {}
    public record Row(String original,String query,String amount,List<Item> matches) {
        public Item resolved(){return matches.size()==1?matches.getFirst():null;}
    }
    private static final String NUMBER="[0-9]+(?:[,._ ][0-9]{3})*";
    private static final String QUANTITY="(?:"+NUMBER+")(?:\\s*(?:stacks?|stapel)(?:\\s*(?:\\+|and|und)\\s*"+NUMBER+")?)?";
    private static final Pattern LEADING=Pattern.compile("^("+QUANTITY+")\\s*(?:[x×]\\s*|of\\s+|von\\s+|[-:]\\s*|\\s+)(.+)$",Pattern.CASE_INSENSITIVE);
    private static final Pattern TRAILING=Pattern.compile("^(.+?)\\s*(?:[x×:]\\s*|[-–]\\s*|\\(\\s*|\\s+)("+QUANTITY+")\\s*\\)?$",Pattern.CASE_INSENSITIVE);
    private static final Pattern STACKS=Pattern.compile("^("+NUMBER+")\\s*(?:stacks?|stapel)(?:\\s*(?:\\+|and|und)\\s*("+NUMBER+"))?$",Pattern.CASE_INSENSITIVE);
    private static final Pattern EXACT_NUMBER=Pattern.compile(NUMBER),SPACE=Pattern.compile("\\s+"),SEPARATORS=Pattern.compile("[,._ ]"),BULLET=Pattern.compile("^[\\s*•▪●\\-–]+");
    private final Map<String,List<Item>> names=new HashMap<>();
    private record SearchItem(Item item,List<String> terms) {}
    private final List<SearchItem> searchItems;
    private final List<Item> items;
    public MaterialListImporter(List<Item> catalogue){
        items=List.copyOf(catalogue);
        var prepared=new ArrayList<SearchItem>();
        for(var item:items){
            var aliases=new HashSet<>(item.aliases());aliases.add(item.id());aliases.add(item.name());
            var terms=new ArrayList<String>();
            for(var alias:aliases){String key=normalize(alias);terms.add(key);var group=names.computeIfAbsent(key,k->new ArrayList<>());if(!group.contains(item))group.add(item);}
            prepared.add(new SearchItem(item,List.copyOf(terms)));
        }
        searchItems=List.copyOf(prepared);
    }
    public List<Item> resolve(String query){return List.copyOf(names.getOrDefault(normalize(query),List.of()));}
    public List<Item> search(String query){
        String key=normalize(query);if(key.isBlank())return items;
        var matches=new ArrayList<Item>();
        for(var indexed:searchItems)for(var term:indexed.terms)if(term.contains(key)){matches.add(indexed.item);break;}
        return List.copyOf(matches);
    }
    public List<Row> parse(String text){
        if(text.length()>MAX_TEXT)throw new IllegalArgumentException("text-too-long");
        String[] lines=text.split("\\R",-1);if(lines.length>MAX_LINES)throw new IllegalArgumentException("too-many-lines");
        var rows=new ArrayList<Row>();
        for(String original:lines){
            if(original.isBlank())continue;
            String line=BULLET.matcher(original.strip()).replaceAll("").replace("**", "").replace("`", "").strip();
            String query=line,amount="";
            var leading=LEADING.matcher(line);var trailing=TRAILING.matcher(line);
            if(leading.matches()){amount=leading.group(1);query=leading.group(2).strip();}
            else if(trailing.matches()){amount=trailing.group(2);query=trailing.group(1).strip();}
            // Links, headings, dimensions and prose cannot resolve through fuzzy substring matching.
            rows.add(new Row(original,query,amount,amount.isEmpty()?List.of():resolve(query)));
        }
        return List.copyOf(rows);
    }
    public static int quantity(String expression,int stackSize){
        try{
            String value=expression.strip();long count;
            var stacks=STACKS.matcher(value);
            if(stacks.matches()){if(stackSize<1)return 0;count=Math.addExact(Math.multiplyExact(number(stacks.group(1)),stackSize),stacks.group(2)==null?0:number(stacks.group(2)));}
            else if(EXACT_NUMBER.matcher(value).matches())count=number(value);else return 0;
            return count>0&&count<=1_000_000?(int)count:0;
        }catch(ArithmeticException|NumberFormatException e){return 0;}
    }
    private static long number(String text){return Long.parseLong(SEPARATORS.matcher(text).replaceAll(""));}
    private static String normalize(String value){return SPACE.matcher(Normalizer.normalize(value,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).strip().replace('_',' ')).replaceAll(" ");}
}
