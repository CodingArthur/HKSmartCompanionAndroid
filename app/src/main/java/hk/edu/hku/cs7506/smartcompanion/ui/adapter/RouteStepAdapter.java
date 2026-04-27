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
import hk.edu.hku.cs7506.smartcompanion.data.model.RouteStep;

public class RouteStepAdapter extends RecyclerView.Adapter<RouteStepAdapter.ViewHolder> {
    private final List<RouteStep> items = new ArrayList<>();

    public void submitList(List<RouteStep> steps) {
        items.clear();
        items.addAll(steps);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route_step, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RouteStep step = items.get(position);
        holder.textTitle.setText(step.getTitle());
        holder.textBody.setText(step.getBody());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textTitle;
        final TextView textBody;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textBody = itemView.findViewById(R.id.textBody);
        }
    }
}

