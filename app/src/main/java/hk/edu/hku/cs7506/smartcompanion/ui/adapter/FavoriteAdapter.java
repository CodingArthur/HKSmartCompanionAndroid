package hk.edu.hku.cs7506.smartcompanion.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hk.edu.hku.cs7506.smartcompanion.R;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.util.FormatUtils;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.ViewHolder> {
    public interface OnFavoriteClickListener {
        void onFavoriteClick(RecommendationItem item);
    }

    private final List<RecommendationItem> items = new ArrayList<>();
    private final OnFavoriteClickListener listener;

    public FavoriteAdapter(OnFavoriteClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<RecommendationItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecommendationItem item = items.get(position);
        holder.textTitle.setText(item.getName());
        holder.textMeta.setText(
                FormatUtils.getDisplayTag(holder.itemView.getContext(), item)
                        + " | "
                        + FormatUtils.getPrimaryMetric(holder.itemView.getContext(), item)
        );
        holder.itemView.setOnClickListener(v -> listener.onFavoriteClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textTitle;
        final TextView textMeta;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textMeta = itemView.findViewById(R.id.textMeta);
        }
    }
}
