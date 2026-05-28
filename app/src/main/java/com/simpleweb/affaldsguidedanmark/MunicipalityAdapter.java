package com.simpleweb.affaldsguidedanmark;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MunicipalityAdapter extends RecyclerView.Adapter<MunicipalityAdapter.MunicipalityViewHolder> {
    private final List<Municipality> allMunicipalities;
    private final List<Municipality> visibleMunicipalities;
    private final OnItemClickListener onItemClickListener;
    private String currentQuery = "";
    private String savedMunicipalityName;

    public MunicipalityAdapter(List<Municipality> municipalities, String savedMunicipalityName, OnItemClickListener onItemClickListener) {
        this.allMunicipalities = new ArrayList<>(municipalities);
        this.visibleMunicipalities = new ArrayList<>();
        this.savedMunicipalityName = savedMunicipalityName;
        this.onItemClickListener = onItemClickListener;
        rebuildVisibleMunicipalities();
    }

    public void setSavedMunicipalityName(String savedMunicipalityName) {
        this.savedMunicipalityName = savedMunicipalityName;
        rebuildVisibleMunicipalities();
    }

    public void filter(String query) {
        currentQuery = query == null ? "" : query;
        rebuildVisibleMunicipalities();
    }

    private void rebuildVisibleMunicipalities() {
        String normalizedQuery = currentQuery.trim().toLowerCase(new Locale("da", "DK"));
        visibleMunicipalities.clear();

        Municipality savedMunicipality = null;
        for (Municipality municipality : allMunicipalities) {
            if (!matchesQuery(municipality, normalizedQuery)) {
                continue;
            }

            if (isSavedMunicipality(municipality)) {
                savedMunicipality = municipality;
            } else {
                visibleMunicipalities.add(municipality);
            }
        }

        if (savedMunicipality != null) {
            visibleMunicipalities.add(0, savedMunicipality);
        }

        notifyDataSetChanged();
    }

    private boolean matchesQuery(Municipality municipality, String normalizedQuery) {
        if (normalizedQuery.isEmpty()) {
            return true;
        }

        String searchableText = (municipality.getMunicipality() + " " + municipality.getFullAddress())
                .toLowerCase(new Locale("da", "DK"));
        return searchableText.contains(normalizedQuery);
    }

    private boolean isSavedMunicipality(Municipality municipality) {
        return savedMunicipalityName != null
                && !savedMunicipalityName.trim().isEmpty()
                && municipality.getMunicipality().equalsIgnoreCase(savedMunicipalityName);
    }

    @NonNull
    @Override
    public MunicipalityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_row_municipality, parent, false);
        return new MunicipalityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MunicipalityViewHolder holder, int position) {
        Municipality municipality = visibleMunicipalities.get(position);
        boolean isSaved = isSavedMunicipality(municipality);
        holder.nameTextView.setText(municipality.getMunicipality());
        holder.addressTextView.setText(municipality.getFullAddress());
        holder.savedLabelTextView.setVisibility(isSaved ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(view -> onItemClickListener.onItemClick(municipality));
    }

    @Override
    public int getItemCount() {
        return visibleMunicipalities.size();
    }

    static class MunicipalityViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView addressTextView;
        private final TextView savedLabelTextView;

        MunicipalityViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.municipalityName);
            addressTextView = itemView.findViewById(R.id.municipalityAddress);
            savedLabelTextView = itemView.findViewById(R.id.municipalitySavedLabel);
        }
    }

    interface OnItemClickListener {
        void onItemClick(Municipality municipality);
    }
}
