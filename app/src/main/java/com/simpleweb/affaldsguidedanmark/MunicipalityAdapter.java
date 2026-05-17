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

    public MunicipalityAdapter(List<Municipality> municipalities, OnItemClickListener onItemClickListener) {
        this.allMunicipalities = new ArrayList<>(municipalities);
        this.visibleMunicipalities = new ArrayList<>(municipalities);
        this.onItemClickListener = onItemClickListener;
    }

    public void filter(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(new Locale("da", "DK"));
        visibleMunicipalities.clear();

        if (normalizedQuery.isEmpty()) {
            visibleMunicipalities.addAll(allMunicipalities);
        } else {
            for (Municipality municipality : allMunicipalities) {
                String searchableText = (municipality.getMunicipality() + " " + municipality.getFullAddress())
                        .toLowerCase(new Locale("da", "DK"));
                if (searchableText.contains(normalizedQuery)) {
                    visibleMunicipalities.add(municipality);
                }
            }
        }

        notifyDataSetChanged();
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
        holder.nameTextView.setText(municipality.getMunicipality());
        holder.addressTextView.setText(municipality.getFullAddress());
        holder.itemView.setOnClickListener(view -> onItemClickListener.onItemClick(municipality));
    }

    @Override
    public int getItemCount() {
        return visibleMunicipalities.size();
    }

    static class MunicipalityViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView addressTextView;

        MunicipalityViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.municipalityName);
            addressTextView = itemView.findViewById(R.id.municipalityAddress);
        }
    }

    interface OnItemClickListener {
        void onItemClick(Municipality municipality);
    }
}
