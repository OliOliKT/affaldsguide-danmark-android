package com.simpleweb.affaldsguidedanmark;

import org.junit.Test;
import java.time.*;
import static org.junit.Assert.*;

public class PickupCalendarDraftTest {
    private final CalendarApi.Address address=new CalendarApi.Address("Frederiksberg","private-provider-id","Private Street 21");
    private PickupCalendarDraft draft(String date) {
        return PickupCalendarDraft.create(address,new CalendarApi.Pickup(date,"restmad","Rest/mad"),
                "Waste collection: Residual waste/Food waste","Frederiksberg Kommune","Residual waste/Food waste","Dates do not update automatically.");
    }
    @Test public void eventsUseExclusiveNextDayAtUtcMidnight() {
        PickupCalendarDraft draft=draft("2026-10-29");
        assertEquals(Instant.parse("2026-10-29T00:00:00Z").toEpochMilli(),draft.beginMillis);
        assertEquals(Instant.parse("2026-10-30T00:00:00Z").toEpochMilli(),draft.endMillis);
    }
    @Test public void allDayDatesStayCorrectAcrossDstAndYearBoundary() {
        for(String date:new String[]{"2026-03-29","2026-10-25","2026-12-31"}) {
            PickupCalendarDraft draft=draft(date);
            assertEquals(86400000L,draft.endMillis-draft.beginMillis);
            assertEquals(LocalDate.parse(date),Instant.ofEpochMilli(draft.beginMillis).atZone(ZoneOffset.UTC).toLocalDate());
        }
    }
    @Test public void rejectsMalformedOrImpossibleDates() {
        for(String date:new String[]{"2026-02-30","2026-02-29","2026-13-01","2026-1-01","not a date"}) {
            try {draft(date);fail("Expected rejection of "+date);}catch(IllegalArgumentException|DateTimeException expected){}
        }
        assertNotNull(draft("2028-02-29"));
    }
    @Test public void duplicateKeysIncludeAddressMunicipalityDateAndWasteType() {
        CalendarApi.Pickup pickup=new CalendarApi.Pickup("2026-10-29","pap","Pap");
        String key=PickupCalendarDraft.key(address,pickup);
        assertEquals(key,PickupCalendarDraft.key(address,pickup));
        assertNotEquals(key,PickupCalendarDraft.key(new CalendarApi.Address("København",address.id,address.label),pickup));
        assertNotEquals(key,PickupCalendarDraft.key(new CalendarApi.Address(address.municipality,"different",address.label),pickup));
        assertNotEquals(key,PickupCalendarDraft.key(address,new CalendarApi.Pickup("2026-10-30","pap","Pap")));
        assertNotEquals(key,PickupCalendarDraft.key(address,new CalendarApi.Pickup("2026-10-29","plast","Plast")));
        assertNotEquals(PickupCalendarDraft.key(new CalendarApi.Address("a|b","c",""),pickup),PickupCalendarDraft.key(new CalendarApi.Address("a","b|c",""),pickup));
    }
    @Test public void eventNotesExcludeStreetAndProviderId() {
        PickupCalendarDraft draft=draft("2026-10-29");
        assertTrue(draft.notes.contains("Frederiksberg Kommune"));
        assertTrue(draft.notes.contains("Residual waste/Food waste"));
        assertTrue(draft.notes.contains("Dates do not update automatically."));
        assertTrue(draft.notes.contains("https://mit-affald.dk"));
        assertFalse(draft.notes.contains(address.label));assertFalse(draft.notes.contains(address.id));
    }
}
