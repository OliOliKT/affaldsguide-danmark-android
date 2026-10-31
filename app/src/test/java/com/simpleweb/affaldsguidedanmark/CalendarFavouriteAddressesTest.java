package com.simpleweb.affaldsguidedanmark;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class CalendarFavouriteAddressesTest {
    private CalendarApi.Address address(String municipality, String id, String label) {
        return new CalendarApi.Address(municipality, id, label);
    }
    @Test public void multipleFavouritesSurviveSerializationInInsertionOrder() {
        CalendarApi.Address home=address("Frederiksberg","21","Kronprinsesse Sofies Vej 21");
        CalendarApi.Address second=address("København","42","Testvej 42");
        List<CalendarApi.Address> entries=CalendarFavouriteAddresses.toggled(Collections.emptyList(),home);
        entries=CalendarFavouriteAddresses.toggled(entries,second);
        List<CalendarApi.Address> loaded=CalendarFavouriteAddresses.read(CalendarFavouriteAddresses.encode(entries));
        assertEquals(2,loaded.size());
        assertEquals(home.label,loaded.get(0).label);
        assertTrue(CalendarFavouriteAddresses.matches(second,loaded.get(1)));
    }
    @Test public void removingOneFavouriteKeepsTheOther() {
        CalendarApi.Address a=address("Frederiksberg","1","Address 1"),b=address("København","2","Address 2");
        List<CalendarApi.Address> result=CalendarFavouriteAddresses.toggled(Arrays.asList(a,b),a);
        assertEquals(1,result.size());assertTrue(CalendarFavouriteAddresses.matches(b,result.get(0)));
        assertEquals(2,CalendarFavouriteAddresses.toggled(result,a).size());
    }
    @Test public void providerIdsAreScopedToTheirMunicipality() {
        CalendarApi.Address a=address("Frederiksberg","1","Same label"),b=address("København","1","Same label");
        assertFalse(CalendarFavouriteAddresses.matches(a,b));
        assertTrue(CalendarFavouriteAddresses.matches(a,address(" frederiksberg ","1","Updated label")));
        assertEquals(2,CalendarFavouriteAddresses.toggled(Collections.singletonList(a),b).size());
    }
    @Test public void readingFiltersIncompleteAndDuplicateRecords() {
        String json="[null,{\"id\":\"1\"},{\"id\":\"1\",\"municipality\":\"Frederiksberg\",\"label\":\"First\"},{\"id\":\"1\",\"municipality\":\"frederiksberg\",\"label\":\"Duplicate\"}]";
        List<CalendarApi.Address> values=CalendarFavouriteAddresses.read(json);
        assertEquals(1,values.size());assertEquals("First",values.get(0).label);
        assertTrue(CalendarFavouriteAddresses.read("[]").isEmpty());
    }
}
