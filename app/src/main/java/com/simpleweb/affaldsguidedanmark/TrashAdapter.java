package com.simpleweb.affaldsguidedanmark;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TrashAdapter extends RecyclerView.Adapter<TrashAdapter.TrashViewHolder> {
    private List<String> trashNames;

    public void setTrashNames(List<String> trashNames) {
        this.trashNames = trashNames;
    }

    @NonNull
    @Override
    public TrashViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_row_trash, parent, false);
        return new TrashViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrashViewHolder holder, int position) {
        String trashName = trashNames.get(position);
        holder.bind(trashName);
    }

    @Override
    public int getItemCount() {
        return trashNames != null ? trashNames.size() : 0;
    }

    static class TrashViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewTrashName;

        public TrashViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTrashName = itemView.findViewById(R.id.textViewTrashName);
        }

        public void bind(String trashName) {
            textViewTrashName.setText(trashName);
        }
    }
}
