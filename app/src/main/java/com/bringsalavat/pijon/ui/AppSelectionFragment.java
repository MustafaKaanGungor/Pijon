package com.bringsalavat.pijon.ui;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bringsalavat.pijon.R;
import com.bringsalavat.pijon.adapter.AppListAdapter;
import com.bringsalavat.pijon.data.PrefsManager;
import com.bringsalavat.pijon.model.MonitoredApp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * Fragment displaying the list of installed (launchable) apps
 * with toggle switches to enable/disable Pijon monitoring for each.
 */
public class AppSelectionFragment extends Fragment implements AppListAdapter.OnAppToggleListener {

    private PrefsManager prefsManager;
    private AppListAdapter adapter;
    private ProgressBar loadingProgress;
    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_app_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefsManager = new PrefsManager(requireContext());

        loadingProgress = view.findViewById(R.id.loading_progress);
        recyclerView = view.findViewById(R.id.app_list_recycler);
        EditText searchEditText = view.findViewById(R.id.search_edit_text);

        // Setup RecyclerView
        adapter = new AppListAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Search filter
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Load apps in background
        loadApps();
    }

    private void loadApps() {
        loadingProgress.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<MonitoredApp> apps = getInstalledLaunchableApps();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    loadingProgress.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.setApps(apps);
                });
            }
        });
    }

    /**
     * Get all installed apps that have a launcher intent (i.e., user-facing apps).
     * Excludes Pijon itself from the list.
     */
    private List<MonitoredApp> getInstalledLaunchableApps() {
        PackageManager pm = requireContext().getPackageManager();
        Set<String> selectedApps = prefsManager.getSelectedApps();

        // Get all apps that have a launcher activity
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<android.content.pm.ResolveInfo> resolveInfos =
                pm.queryIntentActivities(launcherIntent, 0);

        List<MonitoredApp> apps = new ArrayList<>();
        String myPackage = requireContext().getPackageName();

        for (android.content.pm.ResolveInfo info : resolveInfos) {
            String packageName = info.activityInfo.packageName;

            // Skip Pijon itself
            if (packageName.equals(myPackage)) continue;

            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                String appName = pm.getApplicationLabel(appInfo).toString();
                android.graphics.drawable.Drawable icon = pm.getApplicationIcon(appInfo);
                boolean isSelected = selectedApps.contains(packageName);

                apps.add(new MonitoredApp(packageName, appName, icon, isSelected));
            } catch (PackageManager.NameNotFoundException e) {
                // Skip apps we can't find info for
            }
        }

        // Sort alphabetically by app name
        Collections.sort(apps, (a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName()));

        return apps;
    }

    @Override
    public void onAppToggled(MonitoredApp app, boolean enabled) {
        if (enabled) {
            prefsManager.addSelectedApp(app.getPackageName());
        } else {
            prefsManager.removeSelectedApp(app.getPackageName());
        }
    }
}
