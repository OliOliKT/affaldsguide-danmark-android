package com.simpleweb.affaldsguidedanmark;

import android.content.SharedPreferences;
import com.google.gson.Gson;
import org.junit.Test;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

public class SortingImpactTest {
    private SortingImpact impact() throws Exception {
        try(Reader reader=new FileReader("src/main/res/raw/type_impact_factors.json")) {
            return new SortingImpact(new Gson().fromJson(reader,SortingImpact.Catalogue.class));
        }
    }
    @Test public void factorUsesDestinationAndTypeWithIosProductExceptions() throws Exception {
        SortingImpact impact=impact();Map<String,String> types=new HashMap<>();types.put("Metal","aluminium");
        assertEquals(7.97,impact.factor("Metal",types,"Aluminiumsfolie"),.00001);
        assertEquals(0,impact.factor("Restaffald",types,"Aluminiumsfolie"),0);
        types.put("Metal","staal");assertEquals(.26,impact.factor("Metal",types,"Blikdåse"),.00001);
        types.put("Småt elektronik","computerudstyr");assertEquals(2.69,impact.factor("Småt elektronik",types,"Tastatur"),.00001);
        assertEquals(3.71,impact.factor("Metal",Collections.singletonMap("Metal","unknown"),"Unknown"),.00001);
        assertEquals(0,impact.factor("Genbrugsplads",types,"Tastatur"),0);
    }
    @Test public void supportedDestinationsIgnoreRecyclingCentreWhenMaterialExists() throws Exception {
        SortingImpact impact=impact();
        assertEquals(Collections.singletonList("Småt elektronik"),impact.choices(Arrays.asList("Småt elektronik","Genbrugsplads")));
        assertEquals(Collections.singletonList("Genbrugsplads"),impact.choices(Collections.singletonList("Genbrugsplads")));
        assertTrue(impact.choices(Arrays.asList("Vask","Apotek","Politistation","Genbrug")).isEmpty());
        assertEquals(2,impact.choices(Arrays.asList("Pap","Plast","Genbrugsplads")).size());
    }
    @Test public void splitsWeightEquallyWithoutDoubleCounting() {
        assertEquals(.5*(2.92+2.41)/2,SortingImpact.estimate(500,Arrays.asList(2.92,2.41)),.000001);
        assertEquals(0,SortingImpact.estimate(500,Collections.singletonList(0.0)),0);
    }
    @Test public void customWeightAcceptsDecimalCommaAndValidatesRange() {
        assertEquals(Integer.valueOf(1250),SortingImpact.grams("1,25",true));
        assertEquals(Integer.valueOf(100000),SortingImpact.grams("100",true));
        for(String invalid:Arrays.asList("", "0", "-1", "NaN", "Infinity", "100001"))assertNull(SortingImpact.grams(invalid,false));
    }
    @Test public void persistenceEditingAndDeletingKeepFactorsAndNotifyAchievements() {
        MemoryPreferences prefs=new MemoryPreferences();List<Integer> counts=new ArrayList<>();
        SortingLogStore store=new SortingLogStore(prefs,(n,kg)->counts.add(n));
        LinkedHashMap<String,Double> factors=new LinkedHashMap<>();factors.put("Pap",2.92);factors.put("Plast",2.41);
        store.add("Blandet emballage","Københavns Kommune",100,factors);
        SortingLogStore loaded=new SortingLogStore(prefs,(n,kg)->counts.add(n));
        SortingLogStore.Entry e=loaded.entries().get(0);assertEquals("Blandet emballage",e.product);assertEquals(2,e.factors.size());
        double initial=loaded.total();loaded.edit(e.id,500);assertEquals(initial*5,loaded.total(),.000001);
        assertEquals(SortingImpact.VERSION,loaded.entries().get(0).factorVersion);
        loaded.delete(e.id);assertTrue(new SortingLogStore(prefs,(n,kg)->{}).entries().isEmpty());
        assertEquals(Arrays.asList(1,1,0),counts);
    }
    @Test public void failedWriteDoesNotReportSuccessOrMutateLog() {
        MemoryPreferences prefs=new MemoryPreferences();prefs.fail=true;
        SortingLogStore store=new SortingLogStore(prefs,(n,kg)->fail("Must not notify achievements"));
        LinkedHashMap<String,Double> factors=new LinkedHashMap<>();factors.put("Pap",2.92);
        try{store.add("Pap",null,50,factors);fail("Expected failure");}catch(IllegalStateException expected){}
        assertTrue(store.entries().isEmpty());
    }
    @Test public void csvEscapesQuotesAndSpreadsheetFormulas() {
        assertEquals("\"' =1+1\"",SortingImpact.csvCell(" =1+1"));
        assertEquals("\"A \"\"quoted\"\" item\"",SortingImpact.csvCell("A \"quoted\" item"));
        assertEquals("\"\"",SortingImpact.csvCell(null));
    }
    private static final class MemoryPreferences implements SharedPreferences {
        final Map<String,Object> data=new HashMap<>();boolean fail;
        public Map<String,?> getAll(){return data;}public String getString(String k,String d){return (String)data.getOrDefault(k,d);}
        public Set<String> getStringSet(String k,Set<String>d){return d;}public int getInt(String k,int d){return d;}public long getLong(String k,long d){return d;}public float getFloat(String k,float d){return d;}public boolean getBoolean(String k,boolean d){return d;}public boolean contains(String k){return data.containsKey(k);}
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
        public Editor edit(){return new Editor(){final Map<String,Object> pending=new HashMap<>();public Editor putString(String k,String v){pending.put(k,v);return this;}public Editor putStringSet(String k,Set<String> v){return this;}public Editor putInt(String k,int v){return this;}public Editor putLong(String k,long v){return this;}public Editor putFloat(String k,float v){return this;}public Editor putBoolean(String k,boolean v){return this;}public Editor remove(String k){return this;}public Editor clear(){return this;}public boolean commit(){if(fail)return false;data.putAll(pending);return true;}public void apply(){commit();}};}
    }
}
