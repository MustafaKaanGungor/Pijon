package com.bringsalavat.pijon.ui;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bringsalavat.pijon.R;
import com.bringsalavat.pijon.data.PrefsManager;
import com.bringsalavat.pijon.service.PijonMonitorService;
import com.bringsalavat.pijon.util.PermissionHelper;
import com.google.android.material.button.MaterialButton;

/**
 * Home screen fragment showing Pijon's current status,
 * the number of monitored apps, and a start/stop toggle.
 */
public class HomeFragment extends Fragment {

    private PrefsManager prefsManager;
    private View statusDot;
    private TextView statusText;
    private TextView monitoringCountText;
    private MaterialButton btnToggle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefsManager = new PrefsManager(requireContext());

        statusDot = view.findViewById(R.id.status_dot);
        statusText = view.findViewById(R.id.status_text);
        monitoringCountText = view.findViewById(R.id.monitoring_count_text);
        btnToggle = view.findViewById(R.id.btn_toggle_service);

        btnToggle.setOnClickListener(v -> toggleService());

        updateUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private void toggleService() {
        boolean currentlyEnabled = prefsManager.isGlobalEnabled();

        if (!currentlyEnabled) {
            // Turning ON — check permissions first
            if (!PermissionHelper.hasUsageStatsPermission(requireContext())) {
                PermissionHelper.requestUsageStatsPermission(requireContext());
                return;
            }
            if (!PermissionHelper.hasOverlayPermission(requireContext())) {
                PermissionHelper.requestOverlayPermission(requireContext());
                return;
            }

            // Start the service
            prefsManager.setGlobalEnabled(true);
            Intent serviceIntent = new Intent(requireContext(), PijonMonitorService.class);
            requireContext().startForegroundService(serviceIntent);
        } else {
            // Turning OFF — stop the service
            prefsManager.setGlobalEnabled(false);
            Intent serviceIntent = new Intent(requireContext(), PijonMonitorService.class);
            requireContext().stopService(serviceIntent);
        }

        updateUI();
    }

    private void updateUI() {
        boolean enabled = prefsManager.isGlobalEnabled();
        int appCount = prefsManager.getSelectedApps().size();

        // Status indicator — update the oval drawable color
        int dotColor = ContextCompat.getColor(requireContext(),
                enabled ? R.color.pijon_active_green : R.color.pijon_inactive_gray);
        GradientDrawable dotDrawable = (GradientDrawable) statusDot.getBackground();
        dotDrawable.setColor(dotColor);

        statusText.setText(enabled ? R.string.home_status_active : R.string.home_status_inactive);

        // Monitoring count
        if (appCount > 0) {
            monitoringCountText.setText(getString(R.string.home_monitoring_count, appCount));
        } else {
            monitoringCountText.setText(R.string.home_no_apps);
        }

        // Button
        btnToggle.setText(enabled ? R.string.home_toggle_stop : R.string.home_toggle_start);
    }
}
