package com.simpleweb.affaldsguidedanmark;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TrashTypeAdapter extends RecyclerView.Adapter<TrashTypeAdapter.TrashTypeViewHolder> {

    private List<TrashType> trashTypeList;
    private final OnItemClickListener onItemClickListener;

    public TrashTypeAdapter(List<TrashType> trashTypeList, OnItemClickListener onItemClickListener) {
        this.trashTypeList = trashTypeList;
        this.onItemClickListener = onItemClickListener;
    }

    public void setData(List<TrashType> newData) {
        this.trashTypeList = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TrashTypeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_row_trash_types, parent, false);
        return new TrashTypeViewHolder(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull TrashTypeViewHolder holder, int position) {
        TrashType trashType = trashTypeList.get(position);
        holder.bind(trashType);
    }

    @Override
    public int getItemCount() {
        return trashTypeList.size();
    }

    public class TrashTypeViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView nameTextView;

        public TrashTypeViewHolder(@NonNull View itemView, OnItemClickListener onItemClickListener) {
            super(itemView);
            imageView = itemView.findViewById(R.id.affaldsfraktionBillede);
            nameTextView = itemView.findViewById(R.id.affaldsfraktionNavn);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (onItemClickListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            TrashType trashType = trashTypeList.get(position);
                            onItemClickListener.onItemClick(trashType);
                        }
                    }
                }
            });
        }

        public void bind(TrashType trashType) {
            nameTextView.setText(trashType.getNavn());
            imageView.setImageResource(trashType.getImageResId());
        }
    }

    public interface OnItemClickListener {
        void onItemClick(TrashType trashType);
    }

}
