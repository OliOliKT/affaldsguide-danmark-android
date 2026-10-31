package com.simpleweb.affaldsguidedanmark;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import static org.junit.Assert.*;

public class TrashSynonymsTest {
    private List<TrashDB.TrashItem> readItems() throws Exception {
        try (FileReader reader = new FileReader("src/main/res/raw/affald_data.json")) {
            return new Gson().fromJson(reader, new TypeToken<List<TrashDB.TrashItem>>() {}.getType());
        }
    }

    @Test public void realDanishSynonymResolvesToCanonicalSortingItem() throws Exception {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("da"));
            List<TrashDB.TrashItem> items = readItems();
            assertEquals(2240, items.size());
            TrashDB.TrashItem canonical = TrashDB.resolveItem(items, "Pizzabakke", false);
            assertSame(canonical, TrashDB.resolveItem(items, " Pizzaæske ", false));
            assertSame(canonical, TrashDB.resolveItem(items, "PIZZABOKS", false));
            assertTrue(canonical.getSearchNames(false).contains("Pizzaæske"));
            assertNull(TrashDB.resolveItem(items, "definitely not a waste item", false));
        } finally { Locale.setDefault(previous); }
    }

    @Test public void everyLocalizedSynonymResolvesAndCanonicalNamesTakePriority() throws Exception {
        Locale previous = Locale.getDefault();
        try {
            List<TrashDB.TrashItem> items = readItems();
            for (String language : Arrays.asList("da", "en", "ar")) {
                Locale.setDefault(Locale.forLanguageTag(language));
                boolean nonDanish = !language.equals("da");
                for (TrashDB.TrashItem item : items) {
                    assertSame(item, TrashDB.resolveItem(items, item.getDisplayProduct(nonDanish), nonDanish));
                    for (String alias : item.getSearchNames(nonDanish)) {
                        TrashDB.TrashItem found = TrashDB.resolveItem(items, alias, nonDanish);
                        assertNotNull(language + ": " + alias, found);
                        // Some aliases overlap: an actual canonical name must always win.
                        boolean isCanonical = false;
                        for (TrashDB.TrashItem candidate : items) {
                            if (candidate.getDisplayProduct(nonDanish).equalsIgnoreCase(alias)) {
                                assertSame(candidate, found); isCanonical = true; break;
                            }
                        }
                        if (!isCanonical) assertTrue(found.getSearchNames(nonDanish).contains(alias));
                    }
                }
            }
        } finally { Locale.setDefault(previous); }
    }

    @Test public void recognitionIncludesAllLanguagesButUsesOneCanonicalName() throws Exception {
        List<TrashDB.TrashItem> items = readItems();
        TrashDB.TrashItem item = items.stream().filter(x -> "Pizzabakke".equals(x.product)).findFirst().get();
        assertTrue(item.getRecognitionNames().contains("Pizzaæske"));
        assertTrue(item.getRecognitionNames().contains(item.productEn));
        assertTrue(item.getRecognitionNames().contains(item.productAr));
        assertEquals("Pizzabakke", item.product);
    }
}
