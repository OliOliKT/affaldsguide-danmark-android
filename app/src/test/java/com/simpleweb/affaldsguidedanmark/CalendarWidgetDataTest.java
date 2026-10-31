package com.simpleweb.affaldsguidedanmark;

import com.google.gson.Gson;
import org.junit.Test;
import java.io.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.Assert.*;

public class CalendarWidgetDataTest {
    @Test public void nextFourExcludePastAndInvalidDatesAndSortDeterministically() {
        List<CalendarApi.Pickup> events=Arrays.asList(
            new CalendarApi.Pickup("2026-10-28","","Past"),new CalendarApi.Pickup("not-a-date","","Invalid"),
            new CalendarApi.Pickup("2026-11-02","","Later"),new CalendarApi.Pickup("2026-10-30","","Pap"),
            new CalendarApi.Pickup("2026-10-29","","Restaffald"),new CalendarApi.Pickup("2026-10-30","","Glas"),
            new CalendarApi.Pickup("2026-10-31","","Plast"));
        List<CalendarApi.Pickup> upcoming=CalendarWidgetData.upcoming(events,LocalDate.of(2026,10,29));
        assertEquals(4,upcoming.size());assertEquals("Restaffald",upcoming.get(0).label);
        assertEquals("Glas",upcoming.get(1).label);assertEquals("Pap",upcoming.get(2).label);assertEquals("Plast",upcoming.get(3).label);
        assertEquals(7,events.size());
    }
    @Test public void cacheOnlyMatchesItsProviderAndAddress() {
        CalendarWidgetData.Cache cache=new CalendarWidgetData.Cache();cache.addressId="21";cache.municipality="Frederiksberg";
        assertTrue(cache.matches(new CalendarApi.Address("Frederiksberg","21","Home")));
        assertFalse(cache.matches(new CalendarApi.Address("København","21","Another home")));
        assertFalse(cache.matches(new CalendarApi.Address("Frederiksberg","22","Another home")));
        assertFalse(cache.matches(null));
    }
    @Test public void sharedCacheRetainsDateKindLabelAndTimestamp() {
        CalendarWidgetData.Cache cache=new CalendarWidgetData.Cache();cache.addressId="21";cache.municipality="Frederiksberg";cache.updatedAt=1234;
        cache.events=Collections.singletonList(new CalendarApi.Pickup("2026-10-29","pap","Pap"));
        Gson gson=new Gson();CalendarWidgetData.Cache restored=gson.fromJson(gson.toJson(cache),CalendarWidgetData.Cache.class);
        assertEquals(1234,restored.updatedAt);assertEquals("pap",restored.events.get(0).kind);assertEquals("Pap",restored.events.get(0).label);
    }
    @Test public void translatedWidgetLabelsMatchIosCompoundAndUnusualNames() throws Exception {
        String[][] names;try(Reader reader=new FileReader("src/main/res/raw/calendar_label_names.json")){names=new Gson().fromJson(reader,String[][].class);}
        CalendarPickupLabels labels=new CalendarPickupLabels(Arrays.asList(names));
        assertEquals("Residual waste/Food waste",labels.translate("Rest/mad","en"));
        assertEquals("Large electronics",labels.translate("Stort electronics","en"));
        assertEquals("Food-waste bag dispenser",labels.translate("Madaffaldsdispenser","en"));
        assertEquals("Pap og papir",labels.translate("Pap og papir","da"));
        assertEquals("النفايات المتبقية/نفايات الطعام",labels.translate("Rest/mad","ar"));
        assertEquals("Custom label 42",labels.translate("Custom label 42","en"));
    }
}
