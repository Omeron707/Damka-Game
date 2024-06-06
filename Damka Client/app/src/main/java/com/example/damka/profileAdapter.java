package com.example.damka;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;

import java.util.Map;

public class profileAdapter extends RecyclerView.Adapter<profileAdapter.ItemViewHolder> {
    private final Map<String, String> itemMap;

    public profileAdapter(Map<String, String> itemMap) {
        this.itemMap = itemMap;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.profile_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        String key = (String) itemMap.keySet().toArray()[position];
        String value = itemMap.get(key);
        holder.itemTitle.setText(key);
        holder.itemContent.setText(value);
    }

    @Override
    public int getItemCount() {
        return itemMap.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        public TextView itemTitle;
        public TextView itemContent;

        public ItemViewHolder(View itemView) {
            super(itemView);
            itemTitle = itemView.findViewById(R.id.item_title);
            itemContent = itemView.findViewById(R.id.item_content);
        }
    }
}
