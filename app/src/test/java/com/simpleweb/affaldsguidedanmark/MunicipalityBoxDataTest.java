package com.simpleweb.affaldsguidedanmark;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;
import java.io.FileReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;

public class MunicipalityBoxDataTest {
    @Test public void localRuleListContainsEachItemOnceAndOnlyForRequestedMunicipality() throws Exception {
        List<MunicipalitySortingRuleDB.Rule> rules;
        try(FileReader reader=new FileReader("src/main/res/raw/kommunesortering_data.json")) {
            rules=new Gson().fromJson(reader,new TypeToken<List<MunicipalitySortingRuleDB.Rule>>(){}.getType());
        }
        MunicipalitySortingRuleDB database=new MunicipalitySortingRuleDB(rules);
        Set<String> municipalities=new HashSet<>();
        for(MunicipalitySortingRuleDB.Rule rule:rules)municipalities.add(rule.municipalityName);
        int total=0;
        for(String municipality:municipalities) {
            Set<String> seen=new HashSet<>();
            for(MunicipalitySortingRuleDB.Rule rule:database.rulesForMunicipality(municipality)) {
                assertEquals(municipality,rule.municipalityName);
                assertTrue("Duplicate rule in list: "+rule.product,seen.add(rule.product));
                assertSame(rule,database.ruleFor(municipality,rule.product));
                total++;
            }
        }
        assertEquals(8265,total);
        assertTrue(database.rulesForMunicipality(null).isEmpty());
        assertTrue(database.rulesForMunicipality("Unknown municipality").isEmpty());
    }

    @Test public void nationwideCentreSearchMatchesNamesTownsPostcodesAndMunicipalities() throws Exception {
        List<RecyclingCentreDirectory.Centre> centres;
        try(FileReader reader=new FileReader("src/main/res/raw/genbrugspladser_data.json")) {
            centres=new Gson().fromJson(reader,new TypeToken<List<RecyclingCentreDirectory.Centre>>(){}.getType());
        }
        assertEquals(352,centres.size());
        RecyclingCentreDirectory.Centre centre=centres.stream().filter(x->x.name.equals("Bispeengen Genbrugsstation")).findFirst().get();
        assertTrue(centre.matches("BISPEENGEN"));
        assertTrue(centre.matches("2000"));
        assertTrue(centre.matches("Frederiksberg"));
        assertFalse(centre.matches("no such place"));
        assertTrue(centre.hasCoordinates());
        centre.latitude=null;assertFalse(centre.hasCoordinates());
        centre.latitude=91.0;assertFalse(centre.hasCoordinates());
    }
}
