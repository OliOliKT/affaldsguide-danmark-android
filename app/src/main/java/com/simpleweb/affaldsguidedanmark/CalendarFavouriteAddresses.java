package com.simpleweb.affaldsguidedanmark;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;

/** Favourites keep the provider's address ID and municipality, not just a display label. */
final class CalendarFavouriteAddresses {
    static final String KEY = "favourite_addresses";
    private CalendarFavouriteAddresses() {}

    static boolean matches(CalendarApi.Address a, CalendarApi.Address b) {
        return a != null && b != null && a.id.equals(b.id)
                && a.municipality.trim().equalsIgnoreCase(b.municipality.trim());
    }

    static boolean contains(List<CalendarApi.Address> addresses, CalendarApi.Address address) {
        for (CalendarApi.Address candidate : addresses) if (matches(candidate, address)) return true;
        return false;
    }

    static List<CalendarApi.Address> read(String json) {
        List<CalendarApi.Address> decoded = new Gson().fromJson(json,
                new TypeToken<List<CalendarApi.Address>>() {}.getType());
        List<CalendarApi.Address> result = new ArrayList<>();
        if (decoded == null) return result;
        for (CalendarApi.Address address : decoded) {
            if (address != null && address.id != null && !address.id.trim().isEmpty()
                    && address.municipality != null && !address.municipality.trim().isEmpty()
                    && address.label != null && !address.label.trim().isEmpty()
                    && !contains(result, address)) result.add(address);
        }
        return result;
    }

    static List<CalendarApi.Address> toggled(List<CalendarApi.Address> addresses, CalendarApi.Address address) {
        List<CalendarApi.Address> result = new ArrayList<>(addresses);
        if (contains(result, address)) result.removeIf(candidate -> matches(candidate, address));
        else result.add(address);
        return result;
    }

    static String encode(List<CalendarApi.Address> addresses) { return new Gson().toJson(addresses); }
}
