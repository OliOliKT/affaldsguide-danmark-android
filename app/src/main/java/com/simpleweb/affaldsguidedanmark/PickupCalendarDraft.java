package com.simpleweb.affaldsguidedanmark;

import com.google.gson.Gson;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;

/** Android all-day events use UTC-midnight date values, not a time of collection. */
final class PickupCalendarDraft {
    static final String OPENED_KEY = "calendar_editor_opened_pickups";
    final String key, title, notes;
    final long beginMillis, endMillis;

    private PickupCalendarDraft(String key, String title, String notes, long beginMillis, long endMillis) {
        this.key = key;
        this.title = title;
        this.notes = notes;
        this.beginMillis = beginMillis;
        this.endMillis = endMillis;
    }

    static PickupCalendarDraft create(CalendarApi.Address address, CalendarApi.Pickup pickup,
                                      String title, String municipalityLabel, String translatedLabel, String disclaimer) {
        if (!pickup.date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new IllegalArgumentException("Invalid pickup date");
        LocalDate date = LocalDate.parse(pickup.date);
        String key = key(address, pickup);
        // Deliberately exclude the street address and provider's address ID from the calendar event.
        String notes = municipalityLabel + "\n" + translatedLabel + "\n\n" + disclaimer
                + "\n\nAffaldsguide Danmark · https://mit-affald.dk";
        return new PickupCalendarDraft(key, title, notes,
                date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
    }

    static String key(CalendarApi.Address address, CalendarApi.Pickup pickup) {
        return new Gson().toJson(Arrays.asList(address.municipality, address.id, pickup.date, pickup.kind, pickup.label));
    }
}
