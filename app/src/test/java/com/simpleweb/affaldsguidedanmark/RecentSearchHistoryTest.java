package com.simpleweb.affaldsguidedanmark;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class RecentSearchHistoryTest {
    private RecentSearchHistory.Row row(String canonical, String name, String alias, String fraction) {
        return new RecentSearchHistory.Row(canonical,name,Arrays.asList(name,alias),Collections.singleton(fraction));
    }
    @Test public void searchFindsSynonymsAndIgnoresCaseAndAccents() {
        List<RecentSearchHistory.Row> history=Arrays.asList(
                row("E-cigaret","E-cigarette","Electronic cigarette","Småt elektronik"),
                row("Caféfilter","Café filter","Coffee filter","Madaffald"));
        assertEquals("E-cigaret",RecentSearchHistory.visible(history,"ELECTRONIC", "",0,Locale.UK).get(0).canonical);
        assertEquals("Caféfilter",RecentSearchHistory.visible(history,"cafe", "",0,Locale.UK).get(0).canonical);
    }
    @Test public void combinesTextAndDestinationFilter() {
        List<RecentSearchHistory.Row> history=Arrays.asList(row("A","Plastic cup","Cup","Restaffald"),row("B","Plastic tray","Tray","Plast"));
        assertEquals(1,RecentSearchHistory.visible(history,"plastic","Restaffald",0,Locale.UK).size());
        assertTrue(RecentSearchHistory.visible(history,"cup","Plast",0,Locale.UK).isEmpty());
    }
    @Test public void oldestSortReversesVisibleItemsWithoutChangingHistory() {
        List<RecentSearchHistory.Row> history=Arrays.asList(row("A","Newer","","Pap"),row("B","Older","","Pap"));
        assertEquals("A",RecentSearchHistory.visible(history,"","",0,Locale.UK).get(0).canonical);
        assertEquals("B",RecentSearchHistory.visible(history,"","",1,Locale.UK).get(0).canonical);
        assertEquals("A",history.get(0).canonical);
    }
    @Test public void alphabeticalSortUsesLocaleAndNumericOrder() {
        List<RecentSearchHistory.Row> history=Arrays.asList(row("A","Item 10","","Pap"),row("B","Item 2","","Pap"),row("C","Apple","","Pap"));
        List<RecentSearchHistory.Row> visible=RecentSearchHistory.visible(history,"","",2,Locale.UK);
        assertEquals("Apple",visible.get(0).name);assertEquals("Item 2",visible.get(1).name);assertEquals("Item 10",visible.get(2).name);
    }
    @Test public void mergedSynonymsAppearOnceAtTheirLatestPosition() {
        List<RecentSearchHistory.Row> history=Arrays.asList(row("A","Pizza box","Pizza carton","Pap"),row("B","Paper","","Papir"),row("A","Pizza box","Pizza carton","Pap"));
        assertEquals(2,RecentSearchHistory.visible(history,"","",0,Locale.UK).size());
        assertEquals(1,RecentSearchHistory.visible(history,"pizza carton","",0,Locale.UK).size());
    }
}
