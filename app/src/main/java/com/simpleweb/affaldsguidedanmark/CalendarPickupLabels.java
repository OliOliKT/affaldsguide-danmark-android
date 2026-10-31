package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** The same longest-match collection-name translations as the iOS calendar/widget. */
final class CalendarPickupLabels {
    private static CalendarPickupLabels shared;
    private final List<String[]> names;
    private final Pattern pattern;
    CalendarPickupLabels(List<String[]> names) {
        this.names = new ArrayList<>(names);
        this.names.sort((a,b) -> Integer.compare(b[0].length(),a[0].length()));
        List<String> alternatives = new ArrayList<>();
        for(String[] name:this.names) alternatives.add(Pattern.quote(name[0]));
        pattern=Pattern.compile("(?<![\\p{L}])(?:"+String.join("|",alternatives)+")(?![\\p{L}])",Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
    }
    static synchronized String display(Context context,String label) {
        String language=LanguageManager.getSavedLanguage(context);
        if(!language.equals("en")&&!language.equals("ar"))return label;
        if(shared==null) {
            try(InputStreamReader reader=new InputStreamReader(context.getResources().openRawResource(R.raw.calendar_label_names),StandardCharsets.UTF_8)) {
                shared=new CalendarPickupLabels(Arrays.asList(new Gson().fromJson(reader,String[][].class)));
            } catch(Exception error) {return label;}
        }
        return shared.translate(label,language);
    }
    String translate(String label,String language) {
        if(!language.equals("en")&&!language.equals("ar"))return label;
        Matcher matcher=pattern.matcher(label);StringBuffer result=new StringBuffer();
        while(matcher.find())for(String[] name:names)if(name[0].equalsIgnoreCase(matcher.group())) {
            matcher.appendReplacement(result,Matcher.quoteReplacement(name[language.equals("ar")?2:1]));break;
        }
        matcher.appendTail(result);return result.toString();
    }
}
