package com.simpleweb.affaldsguidedanmark;

import org.junit.Test;
import static org.junit.Assert.*;

public class SortingShareLinkTest {
    @Test public void sharesMunicipalityAndCurrentPreviewVersion() {
        assertEquals("https://mit-affald.dk/skrald/madaffald?municipality=K%C3%B8benhavns%20Kommune&share=2",
                SortingShareLink.build("Madaffald", "da", "Københavns Kommune"));
    }
    @Test public void languageChangesRouteButNotCanonicalItemSlug() {
        assertEquals("https://mit-affald.dk/en/skrald/e-cigaret?municipality=&share=2",
                SortingShareLink.build("E-cigaret", "en", null));
        assertEquals("https://mit-affald.dk/ar/skrald/e-cigaret?municipality=&share=2",
                SortingShareLink.build("E-cigaret", "ar", ""));
    }
    @Test public void matchesDanishSlugRulesAndNormalizesUnicode() {
        assertTrue(SortingShareLink.build("  Æske / Øl & Blåbær! ", "da", "").contains("/skrald/aeske-oel-blaabaer?"));
        assertEquals(SortingShareLink.build("Blåbær", "da", ""),
                SortingShareLink.build("Bla\u030Abær", "da", ""));
    }
    @Test public void municipalityCannotInjectAnotherQueryParameter() {
        assertTrue(SortingShareLink.build("Pap", "da", "Test & share=1").endsWith("municipality=Test%20%26%20share%3D1&share=2"));
    }
}
