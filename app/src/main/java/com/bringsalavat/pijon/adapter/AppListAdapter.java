package com.bringsalavat.pijon.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bringsalavat.pijon.R;
import com.bringsalavat.pijon.model.MonitoredApp;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the app selection list.
 * Displays installed apps with their icon, name, and a toggle switch.
 */
public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppViewHolder> {

    /**
     * Callback interface for toggle changes.
     */
    public interface OnAppToggleListener {
        void onAppToggled(MonitoredApp app, boolean enabled);
    }

    private final List<MonitoredApp> allApps = new ArrayList<>();
    private final List<MonitoredApp> filteredApps = new ArrayList<>();
    private final OnAppToggleListener listener;

    public AppListAdapter(OnAppToggleListener listener) {
        this.listener = listener;
    }

    public void setApps(List<MonitoredApp> apps) {
        allApps.clear();
        allApps.addAll(apps);
        filteredApps.clear();
        filteredApps.addAll(apps);
        notifyDataSetChanged();
    }

    /**
     * Filter the app list by a search query.
     */
    public void filter(String query) {
        filteredApps.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredApps.addAll(allApps);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (MonitoredApp app : allApps) {
                if (app.getAppName().toLowerCase().contains(lowerQuery)) {
                    filteredApps.add(app);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        MonitoredApp app = filteredApps.get(position);
        holder.bind(app);
    }

    @Override
    public int getItemCount() {
        return filteredApps.size();
    }

    class AppViewHolder extends RecyclerView.ViewHolder {

        private final ImageView iconView;
        private final TextView nameView;
        private final SwitchMaterial switchView;

        AppViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.app_icon);
            nameView = itemView.findViewById(R.id.app_name);
            switchView = itemView.findViewById(R.id.app_switch);
        }

        void bind(MonitoredApp app) {
            iconView.setImageDrawable(app.getIcon());
            nameView.setText(app.getAppName());

            // Remove listener before setting state to avoid triggering it
            switchView.setOnCheckedChangeListener(null);
            switchView.setChecked(app.isEnabled());

            switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                app.setEnabled(isChecked);
                if (listener != null) {
                    listener.onAppToggled(app, isChecked);
                }
            });
        }
    }
}
