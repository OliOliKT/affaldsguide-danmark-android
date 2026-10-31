package com.simpleweb.affaldsguidedanmark;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecyclingCentresFragment extends Fragment {
    private List<RecyclingCentreDirectory.Centre> centres = new ArrayList<>();
    private final List<RecyclingCentreDirectory.Centre> visible = new ArrayList<>();
    private String municipality, query = "";
    private boolean onlyLocal = false, loaded = false;
    private Location location;
    private TextView status;
    private Button useLocation;
    private RecyclerView list;
    private ExecutorService worker;
    private LocationManager manager;
    private LocationListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable locationTimeout = this::stopLocationUpdates;
    private final ActivityResultLauncher<String[]> permission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (getView() == null) return;
                if (hasLocationPermission()) findLocation();
                updateList();
            });

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state) {
        municipality = SavedMunicipalityManager.getSavedMunicipalityName(requireContext());
        if (municipality == null || municipality.isEmpty()) onlyLocal = false;
        loaded=false;
        LinearLayout page = MunicipalityScreenViews.page(this, text("municipalityBox.findCentre"));
        android.widget.EditText search = MunicipalityScreenViews.search(requireContext(),text("municipalityBox.searchCentres"),value->{query=value;updateList();});
        search.setText(query);page.addView(search);
        useLocation = new Button(requireContext());
        useLocation.setText(text("municipalityBox.useLocation"));
        useLocation.setTextColor(ContextCompat.getColor(requireContext(),R.color.green_light));
        useLocation.setAllCaps(false);useLocation.setOnClickListener(v->requestLocation(true));
        page.addView(useLocation,new LinearLayout.LayoutParams(-1,-2));
        if (municipality != null && !municipality.isEmpty()) {
            SwitchMaterial local = new SwitchMaterial(requireContext()); local.setText(text("municipalityBox.onlyMyMunicipality"));
            local.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_color));
            local.setMinHeight(dp(48));
            local.setPadding(0, 0, 0, 0);
            local.setChecked(onlyLocal);local.setOnCheckedChangeListener((button,checked)->{onlyLocal=checked;updateList();});
            LinearLayout filterCard = MunicipalityScreenViews.card(requireContext());
            filterCard.setClickable(false);
            filterCard.setFocusable(false);
            filterCard.setForeground(null);
            filterCard.setPadding(dp(16), dp(4), dp(16), dp(4));
            filterCard.addView(local, new LinearLayout.LayoutParams(-1, -2));
            page.addView(filterCard);
        }
        status = MunicipalityScreenViews.label(requireContext(),text("searchview.005"),16,true,true);
        status.setPadding(dp(16),dp(10),dp(16),dp(10));page.addView(status);
        list = new RecyclerView(requireContext()); list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(new CentresAdapter());page.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        return page;
    }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state) {
        worker=Executors.newSingleThreadExecutor();
        android.content.res.Resources resources=getResources();
        worker.execute(()->{
            try {
                List<RecyclingCentreDirectory.Centre> loadedCentres=RecyclingCentreDirectory.load(resources);
                view.post(()->{if(getView()!=view)return;centres=loadedCentres;loaded=true;updateList();});
            } catch(Exception error) {
                view.post(()->{if(getView()==view)status.setText(R.string.nearest_centre_unavailable);});
            }
        });
        requestLocation(false);
    }
    @Override public void onResume() {
        super.onResume();
        if(getView()!=null){if(hasLocationPermission())findLocation();else{location=null;stopLocationUpdates();}updateList();}
    }
    private String text(String key) {return MunicipalityScreenViews.text(requireContext(),key);}
    private int dp(int value){return MunicipalityScreenViews.dp(requireContext(),value);}
    private boolean hasLocationPermission() {
        return getContext()!=null && (ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
                ||ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED);
    }
    private void requestLocation(boolean explicit) {
        if(hasLocationPermission()){findLocation();return;}
        android.content.SharedPreferences prefs=requireContext().getSharedPreferences("RecyclingCentreLocation",Context.MODE_PRIVATE);
        boolean asked=prefs.getBoolean("asked",false);
        if(explicit && asked && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
                && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+requireContext().getPackageName())));
        } else if(explicit || !asked) {
            prefs.edit().putBoolean("asked",true).apply();
            permission.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION});
        }
        updateList();
    }
    private void findLocation() {
        if(!hasLocationPermission()||list==null)return;
        stopLocationUpdates();
        manager=(LocationManager)requireContext().getSystemService(Context.LOCATION_SERVICE);
        try {
            Location best=null;
            for(String provider:manager.getProviders(true)) {
                Location found=manager.getLastKnownLocation(provider);
                if(found!=null&&(best==null||found.getTime()>best.getTime()))best=found;
            }
            if(best!=null){location=best;updateList();}
            String provider=manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)?LocationManager.NETWORK_PROVIDER:LocationManager.GPS_PROVIDER;
            if(!manager.isProviderEnabled(provider))return;
            listener=new LocationListener(){
                public void onLocationChanged(@NonNull Location value){if(list!=null){location=value;updateList();}stopLocationUpdates();}
                public void onProviderEnabled(@NonNull String provider){}
                public void onProviderDisabled(@NonNull String provider){}
            };
            manager.requestSingleUpdate(provider,listener,Looper.getMainLooper());
            handler.postDelayed(locationTimeout,20000);
        }catch(SecurityException|IllegalArgumentException error){stopLocationUpdates();}
    }
    private void stopLocationUpdates() {
        handler.removeCallbacks(locationTimeout);
        if(manager!=null&&listener!=null)try{manager.removeUpdates(listener);}catch(SecurityException ignored){}
        listener=null;
    }
    private Double distance(RecyclingCentreDirectory.Centre centre) {
        if(location==null||!centre.hasCoordinates())return null;
        float[] metres=new float[1];Location.distanceBetween(location.getLatitude(),location.getLongitude(),centre.latitude,centre.longitude,metres);
        return (double)metres[0];
    }
    private void updateList() {
        if(list==null)return;
        boolean permissionGranted=hasLocationPermission();
        if(!permissionGranted)location=null;
        useLocation.setVisibility(permissionGranted?View.GONE:View.VISIBLE);
        visible.clear();
        for(RecyclingCentreDirectory.Centre centre:centres) {
            if(onlyLocal&&!municipality.equals(centre.municipality))continue;
            if(centre.matches(query))visible.add(centre);
        }
        java.text.Collator collator = java.text.Collator.getInstance(Locale.getDefault());
        visible.sort((a,b)->{
            Double first=distance(a),second=distance(b);
            if(first!=null&&second!=null){int order=first.compareTo(second);if(order!=0)return order;}
            else if(first!=null)return -1;else if(second!=null)return 1;
            return collator.compare(a.name,b.name);
        });
        if(loaded)status.setText(visible.isEmpty()?text(query.trim().isEmpty()?(onlyLocal?"municipalityBox.noCentresInMunicipality":"municipalityBox.noCentres"):"municipalityBox.noCentreMatches")
                :text(location==null?"municipalityBox.centres":"municipalityBox.allCentres"));
        list.getAdapter().notifyDataSetChanged();
    }
    private void openMap(RecyclingCentreDirectory.Centre centre) {
        Uri uri=Uri.parse("geo:0,0?q="+Uri.encode(centre.name+", "+centre.address));
        try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}
        catch(android.content.ActivityNotFoundException missing){startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/search/?api=1&query="+Uri.encode(centre.name+", "+centre.address))));}
        AchievementStore.get(requireContext()).recordRecyclingCenterMap(centre.name + "|" + centre.address);
    }
    private class CentresAdapter extends RecyclerView.Adapter<CentreHolder> {
        @NonNull public CentreHolder onCreateViewHolder(@NonNull ViewGroup parent,int type){return new CentreHolder(MunicipalityScreenViews.card(parent.getContext()));}
        public int getItemCount(){return visible.size();}
        public void onBindViewHolder(@NonNull CentreHolder holder,int position){
            RecyclingCentreDirectory.Centre centre=visible.get(position);LinearLayout card=holder.card;card.removeAllViews();
            card.addView(MunicipalityScreenViews.label(card.getContext(),centre.name,17,true,false));
            if(municipality!=null&&municipality.equals(centre.municipality)) {
                TextView local=MunicipalityScreenViews.label(card.getContext(),text("municipalityBox.inYourMunicipality"),12,true,false);
                local.setTextColor(ContextCompat.getColor(card.getContext(),R.color.green_light));card.addView(local);
            }
            card.addView(MunicipalityScreenViews.label(card.getContext(),centre.municipality,13,false,true));
            card.addView(MunicipalityScreenViews.label(card.getContext(),centre.address,14,false,true));
            Double metres=distance(centre);
            if(metres!=null)card.addView(MunicipalityScreenViews.label(card.getContext(),String.format(Locale.getDefault(),"%.1f km",metres/1000),13,false,true));
            else if(!centre.hasCoordinates())card.addView(MunicipalityScreenViews.label(card.getContext(),text("municipalityBox.distanceUnknown"),13,false,true));
            TextView map=MunicipalityScreenViews.label(card.getContext(),getString(R.string.show_on_map),14,true,false);
            map.setTextColor(ContextCompat.getColor(card.getContext(),R.color.green_light));map.setPadding(0,dp(6),0,0);
            android.graphics.drawable.Drawable mapIcon = ContextCompat.getDrawable(card.getContext(), android.R.drawable.ic_dialog_map);
            if (mapIcon != null) mapIcon.setBounds(0,0,dp(18),dp(18));
            map.setCompoundDrawablesRelative(mapIcon,null,null,null);map.setCompoundDrawablePadding(dp(4));
            map.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(card.getContext(),R.color.green_light)));
            card.addView(map);card.setOnClickListener(v->openMap(centre));
        }
    }
    private static class CentreHolder extends RecyclerView.ViewHolder {final LinearLayout card;CentreHolder(LinearLayout view){super(view);card=view;}}
    @Override public void onDestroyView(){stopLocationUpdates();if(worker!=null)worker.shutdownNow();list=null;status=null;useLocation=null;super.onDestroyView();}
}
