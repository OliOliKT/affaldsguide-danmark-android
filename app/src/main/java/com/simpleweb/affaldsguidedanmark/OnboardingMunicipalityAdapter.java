package com.simpleweb.affaldsguidedanmark;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class OnboardingMunicipalityAdapter extends RecyclerView.Adapter<OnboardingMunicipalityAdapter.Holder> {
    interface OnSelectListener {
        void onSelect(Municipality municipality);
    }

    private final List<Municipality> allMunicipalities;
    private final List<Municipality> visibleMunicipalities = new ArrayList<>();
    private final OnSelectListener onSelectListener;
    private String query = "";
    private String selectedMunicipalityName;

    OnboardingMunicipalityAdapter(List<Municipality> municipalities, String selectedMunicipalityName, OnSelectListener listener) {
        this.allMunicipalities = new ArrayList<>(municipalities);
        this.selectedMunicipalityName = selectedMunicipalityName;
        this.onSelectListener = listener;
        rebuild();
    }

    void filter(String query) {
        this.query = query == null ? "" : query;
        rebuild();
    }

    void setSelectedMunicipalityName(String name) {
        selectedMunicipalityName = name;
        rebuild();
    }

    private void rebuild() {
        String needle = query.trim().toLowerCase(new Locale("da", "DK"));
        visibleMunicipalities.clear();
        for (Municipality municipality : allMunicipalities) {
            if (!municipality.getMunicipality().equalsIgnoreCase(selectedMunicipalityName)
                    && matches(municipality, needle)) {
                visibleMunicipalities.add(municipality);
            }
        }
        notifyDataSetChanged();
    }

    private boolean matches(Municipality municipality, String needle) {
        if (needle.isEmpty()
                || municipality.getMunicipality().toLowerCase(new Locale("da", "DK")).contains(needle)
                || municipality.getFullAddress().toLowerCase(new Locale("da", "DK")).contains(needle)) {
            return true;
        }
        if (municipality.getPostalPlaces() != null) {
            for (Municipality.PostalPlace place : municipality.getPostalPlaces()) {
                if (place.getDisplayName().toLowerCase(new Locale("da", "DK")).contains(needle)) {
                    return true;
                }
            }
        }
        return false;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_row_onboarding_municipality, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Municipality municipality = visibleMunicipalities.get(position);
        holder.name.setText(municipality.getMunicipality());
        holder.icon.setImageResource(R.drawable.onboarding_city_icon);
        holder.itemView.setBackgroundResource(R.drawable.list_item_background);
        holder.itemView.setContentDescription(municipality.getMunicipality());
        holder.itemView.setOnClickListener(view -> {
            setSelectedMunicipalityName(municipality.getMunicipality());
            onSelectListener.onSelect(municipality);
        });
    }

    @Override
    public int getItemCount() {
        return visibleMunicipalities.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.onboardingMunicipalityIcon);
            name = itemView.findViewById(R.id.onboardingMunicipalityName);
        }
    }
}
