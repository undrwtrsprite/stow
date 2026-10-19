package dev.stow.client.util;

import java.util.Locale;
import java.util.regex.Pattern;

/** One bounded, reusable parse for the active search; no regex compilation per item. */
public final class TextQuery {
    private static final Pattern SPACE = Pattern.compile("\\s+");
    private static volatile TextQuery recent;
    private final String source;
    private final String[] words;

    private TextQuery(String source) {
        this.source=source;
        String clean=source.strip().toLowerCase(Locale.ROOT);
        words=clean.isEmpty()?new String[0]:SPACE.split(clean);
    }

    public static TextQuery of(String query) {
        TextQuery found=recent;
        if(found!=null&&found.source.equals(query))return found;
        TextQuery parsed=new TextQuery(query);recent=parsed;return parsed;
    }

    public boolean matches(String name,String id,boolean singularFallback) {
        if(words.length==0)return true;
        name=name.toLowerCase(Locale.ROOT);id=id.toLowerCase(Locale.ROOT);
        for(String word:words){
            if(word.startsWith("@")){
                int colon=id.indexOf(':');
                if(!id.substring(0,colon<0?id.length():colon).contains(word.substring(1)))return false;
            }else if(!name.contains(word)&&!id.contains(word)){
                if(!singularFallback||!word.endsWith("s")||word.length()<=3)return false;
                String singular=word.substring(0,word.length()-1);
                if(!name.contains(singular)&&!id.contains(singular))return false;
            }
        }
        return true;
    }
}
