package com.bringsalavat.pijon.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bringsalavat.pijon.R;
import com.bringsalavat.pijon.data.PijonDatabase;
import com.bringsalavat.pijon.data.PrefsManager;
import com.bringsalavat.pijon.model.UsageLog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class SettingsFragment extends Fragment {

    private PrefsManager prefsManager;
    private TextView timerValueText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefsManager = new PrefsManager(requireContext());

        // Timer duration
        timerValueText = view.findViewById(R.id.timer_value_text);
        SeekBar timerSeekbar = view.findViewById(R.id.timer_seekbar);

        int currentSeconds = prefsManager.getTimerDurationSeconds();
        int presetIndex = findPresetIndex(currentSeconds);
        timerSeekbar.setProgress(presetIndex);
        updateTimerLabel(presetIndex);

        timerSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateTimerLabel(progress);
                if (fromUser) {
                    int seconds = PrefsManager.TIMER_PRESETS[progress];
                    prefsManager.setTimerDurationSeconds(seconds);
                }
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Sound toggle
        SwitchMaterial switchSound = view.findViewById(R.id.switch_sound);
        switchSound.setChecked(prefsManager.isSoundEnabled());
        switchSound.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefsManager.setSoundEnabled(isChecked));

        // Vibration toggle
        SwitchMaterial switchVibration = view.findViewById(R.id.switch_vibration);
        switchVibration.setChecked(prefsManager.isVibrationEnabled());
        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefsManager.setVibrationEnabled(isChecked));

        // Export data
        MaterialButton exportBtn = view.findViewById(R.id.btn_export_data);
        exportBtn.setOnClickListener(v -> exportAllData());

        // Clear history
        MaterialButton clearBtn = view.findViewById(R.id.btn_clear_history);
        clearBtn.setOnClickListener(v -> confirmClearHistory());
    }

    // ---------------------------------------------------------------
    // Timer helpers
    // ---------------------------------------------------------------

    private void updateTimerLabel(int presetIndex) {
        int seconds = PrefsManager.TIMER_PRESETS[presetIndex];
        String formatted = PrefsManager.formatDuration(seconds);
        timerValueText.setText(getString(R.string.settings_timer_value, formatted));
    }

    private int findPresetIndex(int seconds) {
        int closestIndex = 0;
        int closestDiff = Integer.MAX_VALUE;
        for (int i = 0; i < PrefsManager.TIMER_PRESETS.length; i++) {
            int diff = Math.abs(PrefsManager.TIMER_PRESETS[i] - seconds);
            if (diff < closestDiff) {
                closestDiff = diff;
                closestIndex = i;
            }
        }
        return closestIndex;
    }

    // ---------------------------------------------------------------
    // Clear history
    // ---------------------------------------------------------------

    private void confirmClearHistory() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_clear_confirm_title)
                .setMessage(R.string.settings_clear_confirm_msg)
                .setPositiveButton(R.string.settings_clear_confirm_yes,
                        (dialog, which) -> clearHistory())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void clearHistory() {
        Executors.newSingleThreadExecutor().execute(() -> {
            PijonDatabase.getInstance(requireContext()).usageLogDao().deleteAll();
            showToast(getString(R.string.settings_clear_success));
        });
    }

    // ---------------------------------------------------------------
    // CSV export (all-time, no period filter)
    // ---------------------------------------------------------------

    private void exportAllData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<UsageLog> logs = PijonDatabase.getInstance(requireContext())
                        .usageLogDao().getLogsSince(0L);

                if (logs.isEmpty()) {
                    showToast(getString(R.string.stats_export_empty));
                    return;
                }

                File exportsDir = new File(requireContext().getExternalFilesDir(null), "exports");
                //noinspection ResultOfMethodCallIgnored
                exportsDir.mkdirs();
                File csvFile = new File(exportsDir, "pijon_export.csv");

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
                    writer.println("Timestamp,App Name,Duration (seconds),Feeling,Good Use");
                    for (UsageLog log : logs) {
                        writer.printf("%s,%s,%d,%s,%s%n",
                                sdf.format(new Date(log.getTimestamp())),
                                escapeCsv(log.getAppName()),
                                log.getDurationSeconds(),
                                feelingLabel(log.getFeeling()),
                                goodUseLabel(log.getGoodUse()));
                    }
                }

                Uri uri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".provider", csvFile);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/csv");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isAdded()) return;
                    startActivity(Intent.createChooser(
                            shareIntent, getString(R.string.stats_export_title)));
                });

            } catch (Exception e) {
                showToast(getString(R.string.stats_export_error));
            }
        });
    }

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });
    }

    private static String feelingLabel(int value) {
        if (value ==  1) return "positive";
        if (value == -1) return "negative";
        return "neutral";
    }

    private static String goodUseLabel(int value) {
        if (value ==  1) return "yes";
        if (value == -1) return "no";
        return "unsure";
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
