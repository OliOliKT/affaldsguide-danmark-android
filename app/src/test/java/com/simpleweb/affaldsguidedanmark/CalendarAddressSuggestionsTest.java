package com.simpleweb.affaldsguidedanmark;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class CalendarAddressSuggestionsTest {
    @Test public void streetNamesCollapseHouseNumbersAndUnits() {
        List<CalendarAddressSuggestions.Option> values = CalendarAddressSuggestions.options(Arrays.asList(
            "Kronprinsesse Sofies Vej 6 2, 2000 Frederiksberg",
            "Kronprinsesse Sofies Vej 18 U, 2000 Frederiksberg",
            "Kronprinsesse Sofies Vej 21, 2000 Frederiksberg"), "2000", true);
        assertEquals(1, values.size());
        assertEquals("Kronprinsesse Sofies Vej", values.get(0).value);
        assertEquals(values.get(0).value, values.get(0).label);
    }
    @Test public void houseNumbersKeepLettersButExcludeFloor() {
        List<CalendarAddressSuggestions.Option> values = CalendarAddressSuggestions.options(Arrays.asList(
            "Testvej 21A, 2000 Frederiksberg", "Testvej 21A 2 tv, 2000 Frederiksberg",
            "Testvej 21B, 2000 Frederiksberg"), "2000", false);
        assertEquals(2, values.size());
        assertEquals("21A", values.get(0).value);
        assertEquals("21B", values.get(1).value);
    }
    @Test public void suggestionsAreNotLimitedToEight() {
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        for (int i = 1; i <= 25; i++) labels.add("Testvej " + i + ", 2000 Frederiksberg");
        assertEquals(25, CalendarAddressSuggestions.options(labels, "2000", false).size());
    }
}
