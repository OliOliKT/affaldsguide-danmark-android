package com.simpleweb.affaldsguidedanmark;

import org.junit.Test;
import java.time.*;
import static org.junit.Assert.*;

public class AppReviewPolicyTest {
    private final ZoneId zone=ZoneId.of("Europe/Copenhagen");
    private long at(String date){return LocalDate.parse(date).atTime(12,0).atZone(zone).toInstant().toEpochMilli();}
    private boolean eligible(long now,int days,boolean task,long last,String lastVersion){return AppReviewPolicy.eligible(now,at("2026-01-01"),days,task,last,lastVersion,"3.2",zone);}
    @Test public void needsAWeekThreeActiveDaysAndCompletedTask(){
        assertFalse(eligible(at("2026-01-07"),3,true,0,""));
        assertFalse(eligible(at("2026-01-08"),2,true,0,""));
        assertFalse(eligible(at("2026-01-08"),3,false,0,""));
        assertTrue(eligible(at("2026-01-08"),3,true,0,""));
    }
    @Test public void onlyRequestsOncePerVersion(){
        assertFalse(eligible(at("2026-10-29"),3,true,at("2026-01-10"),"3.2"));
        assertTrue(eligible(at("2026-10-29"),3,true,at("2026-01-10"),"3.1"));
    }
    @Test public void newVersionStillHonours120DayCooldown(){
        long previous=at("2026-06-01");
        long next=Instant.ofEpochMilli(previous).atZone(zone).plusDays(120).toInstant().toEpochMilli();
        assertFalse(eligible(next-1,3,true,previous,"3.1"));
        assertTrue(eligible(next,3,true,previous,"3.1"));
    }
    @Test public void activeDaysDoNotCountRepeatedOpensOnTheSameDay(){
        assertEquals(1,AppReviewPolicy.activeDays(1,100,100));
        assertEquals(2,AppReviewPolicy.activeDays(1,100,101));
        assertEquals(3,AppReviewPolicy.activeDays(3,101,102));
    }
    @Test public void weekUsesCalendarDaysAcrossDaylightSaving(){
        long first=at("2026-03-27"),next=at("2026-04-03");
        assertTrue(AppReviewPolicy.eligible(next,first,3,true,0,"","3.2",zone));
        assertFalse(AppReviewPolicy.eligible(next-1,first,3,true,0,"","3.2",zone));
    }
    @Test public void missingFirstUseNeverTriggersAReview(){
        assertFalse(AppReviewPolicy.eligible(at("2026-10-29"),0,3,true,0,"","3.2",zone));
    }
}
