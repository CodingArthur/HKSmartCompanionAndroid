package hk.edu.hku.cs7506.smartcompanion.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import hk.edu.hku.cs7506.smartcompanion.R;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.util.FormatUtils;

public class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.ViewHolder> {
    public interface OnRecommendationClickListener {
        void onRecommendationClick(RecommendationItem item);
    }

    private final List<RecommendationItem> items = new ArrayList<>();
    private final OnRecommendationClickListener listener;

    public RecommendationAdapter(OnRecommendationClickListener listener) {
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recommendation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecommendationItem item = items.get(position);
        holder.textSceneTag.setText(FormatUtils.getDisplayTag(holder.itemView.getContext(), item));
        holder.textTitle.setText(item.getName());
        holder.textReason.setText(item.getReason());
        holder.textPrimaryMetric.setText(FormatUtils.getPrimaryMetric(holder.itemView.getContext(), item));
        holder.textSecondaryMetric.setText(FormatUtils.getSecondaryMetric(holder.itemView.getContext(), item));
        bindOptionalText(holder.textMetaLine, FormatUtils.getMetadataLine(item));
        bindOptionalText(holder.textTransportNote, FormatUtils.getTransportNote(item));
        if (TextUtils.isEmpty(item.getImageUrl())) {
            holder.layoutHero.setVisibility(View.GONE);
            holder.imageHero.setImageDrawable(null);
        } else {
            holder.layoutHero.setVisibility(View.VISIBLE);
            Object imageModel = resolveImageModel(holder.imageHero, item.getImageUrl());
            Glide.with(holder.imageHero)
                    .load(imageModel)
                    .centerCrop()
                    .placeholder(R.drawable.bg_hero_panel)
                    .into(holder.imageHero);
        }
        holder.itemView.setOnClickListener(v -> listener.onRecommendationClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textTitle;
        final TextView textReason;
        final TextView textPrimaryMetric;
        final TextView textSecondaryMetric;
        final TextView textSceneTag;
        final TextView textMetaLine;
        final TextView textTransportNote;
        final ImageView imageHero;
        final View layoutHero;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textSceneTag = itemView.findViewById(R.id.textSceneTag);
            textTitle = itemView.findViewById(R.id.textTitle);
            textReason = itemView.findViewById(R.id.textReason);
            textPrimaryMetric = itemView.findViewById(R.id.textPrimaryMetric);
            textSecondaryMetric = itemView.findViewById(R.id.textSecondaryMetric);
            textMetaLine = itemView.findViewById(R.id.textMetaLine);
            textTransportNote = itemView.findViewById(R.id.textTransportNote);
            imageHero = itemView.findViewById(R.id.imageHero);
            layoutHero = itemView.findViewById(R.id.layoutHero);
        }
    }

    private Object resolveImageModel(ImageView target, String imageUrl) {
        if (imageUrl != null && imageUrl.startsWith("drawable://")) {
            String resourceName = imageUrl.substring("drawable://".length());
            int resourceId = target.getResources().getIdentifier(
                    resourceName,
                    "drawable",
                    target.getContext().getPackageName()
            );
            if (resourceId != 0) {
                return resourceId;
            }
        }
        return imageUrl;
    }

    private void bindOptionalText(TextView view, String value) {
        if (TextUtils.isEmpty(value)) {
            view.setVisibility(View.GONE);
            view.setText("");
            return;
        }
        view.setVisibility(View.VISIBLE);
        view.setText(value);
    }
}
