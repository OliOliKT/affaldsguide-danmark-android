package com.simpleweb.affaldsguidedanmark;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Keep;
import com.google.gson.Gson;
import java.time.LocalDate;
import java.util.*;

/** Address-scoped shared cache for the app and calendar widget. */
final class CalendarWidgetData {
    static final String CACHE="pickup_cache";
    @Keep static final class Cache {
        String municipality,addressId;
        long updatedAt;
        List<CalendarApi.Pickup> events;
        boolean matches(CalendarApi.Address address) {
            return address!=null && address.id.equals(addressId) && address.municipality.equals(municipality);
        }
    }
    static SharedPreferences prefs(Context context){return context.getSharedPreferences("collection_calendar",Context.MODE_PRIVATE);}
    static CalendarApi.Address address(Context context) {
        try {
            CalendarApi.Address address=new Gson().fromJson(prefs(context).getString("address",""),CalendarApi.Address.class);
            return address!=null && address.id!=null && !address.id.isEmpty() && address.municipality!=null && !address.municipality.isEmpty() && address.label!=null?address:null;
        } catch(Exception error){return null;}
    }
    static synchronized boolean selectAddress(Context context, CalendarApi.Address selected) {
        if (!prefs(context).edit().putString("address", new Gson().toJson(selected)).remove(CACHE).commit()) return false;
        CalendarReminderWorker.clear(context);
        return true;
    }
    static Cache cache(Context context,CalendarApi.Address address) {
        try {
            Cache cache=new Gson().fromJson(prefs(context).getString(CACHE,""),Cache.class);
            return cache!=null && cache.matches(address) && cache.events!=null?cache:null;
        }catch(Exception error){return null;}
    }
    static synchronized boolean save(Context context,CalendarApi.Address requested,List<CalendarApi.Pickup> events,long startedAt) {
        if(!CalendarFavouriteAddresses.matches(address(context),requested))return false;
        Cache previous=cache(context,requested);
        if(previous!=null && previous.updatedAt>startedAt)return false;
        Cache cache=new Cache();cache.addressId=requested.id;cache.municipality=requested.municipality;cache.events=new ArrayList<>(events);cache.updatedAt=System.currentTimeMillis();
        return prefs(context).edit().putString(CACHE,new Gson().toJson(cache)).commit();
    }
    static List<CalendarApi.Pickup> upcoming(List<CalendarApi.Pickup> events,LocalDate today) {
        List<CalendarApi.Pickup> result = allUpcoming(events, today);
        return new ArrayList<>(result.subList(0, Math.min(4, result.size())));
    }
    static List<CalendarApi.Pickup> allUpcoming(List<CalendarApi.Pickup> events,LocalDate today) {
        List<CalendarApi.Pickup> result=new ArrayList<>();
        for(CalendarApi.Pickup event:events)try {if(!LocalDate.parse(event.date).isBefore(today))result.add(event);}catch(Exception ignored){}
        result.sort((a,b)->{int order=a.date.compareTo(b.date);return order!=0?order:a.label.compareTo(b.label);});
        return result;
    }
}
