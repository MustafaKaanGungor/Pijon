package com.bringsalavat.pijon;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.bringsalavat.pijon.ui.AppSelectionFragment;
import com.bringsalavat.pijon.ui.HomeFragment;
import com.bringsalavat.pijon.ui.SettingsFragment;
import com.bringsalavat.pijon.ui.StatsFragment;
import com.bringsalavat.pijon.util.PermissionHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Main activity serving as the single-activity host for all fragments.
 * Manages bottom navigation and the initial permission request flow.
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;

    private final HomeFragment homeFragment = new HomeFragment();
    private final AppSelectionFragment appSelectionFragment = new AppSelectionFragment();
    private final StatsFragment statsFragment = new StatsFragment();
    private final SettingsFragment settingsFragment = new SettingsFragment();
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        setupBottomNavigation();
        checkPermissionsOnFirstLaunch();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Add all fragments but hide non-active ones
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, settingsFragment, "settings").hide(settingsFragment)
                .add(R.id.fragment_container, statsFragment, "stats").hide(statsFragment)
                .add(R.id.fragment_container, appSelectionFragment, "apps").hide(appSelectionFragment)
                .add(R.id.fragment_container, homeFragment, "home")
                .commit();

        activeFragment = homeFragment;

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment targetFragment;

            if (itemId == R.id.nav_home) {
                targetFragment = homeFragment;
            } else if (itemId == R.id.nav_apps) {
                targetFragment = appSelectionFragment;
            } else if (itemId == R.id.nav_stats) {
                targetFragment = statsFragment;
            } else if (itemId == R.id.nav_settings) {
                targetFragment = settingsFragment;
            } else {
                return false;
            }

            if (targetFragment != activeFragment) {
                getSupportFragmentManager().beginTransaction()
                        .hide(activeFragment)
                        .show(targetFragment)
                        .commit();
                activeFragment = targetFragment;
            }
            return true;
        });
    }

    /**
     * On first launch, guide the user through granting required permissions.
     * Uses a sequential dialog approach: Usage Stats → Overlay → Notifications.
     */
    private void checkPermissionsOnFirstLaunch() {
        if (!PermissionHelper.hasUsageStatsPermission(this)) {
            showPermissionDialog(
                    getString(R.string.perm_usage_stats_title),
                    getString(R.string.perm_usage_stats_message),
                    () -> {
                        PermissionHelper.requestUsageStatsPermission(this);
                        // After returning, onResume will re-check
                    }
            );
        } else if (!PermissionHelper.hasOverlayPermission(this)) {
            showPermissionDialog(
                    getString(R.string.perm_overlay_title),
                    getString(R.string.perm_overlay_message),
                    () -> PermissionHelper.requestOverlayPermission(this)
            );
        } else if (PermissionHelper.needsNotificationPermission()) {
            requestNotificationPermission();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check permissions after returning from settings
        // (don't show dialog again immediately to avoid loops)
    }

    private void showPermissionDialog(String title, String message, Runnable onGrant) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.perm_btn_grant, (dialog, which) -> onGrant.run())
                .setNegativeButton(R.string.perm_btn_later, null)
                .setCancelable(true)
                .show();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Notification permission result — we proceed either way
    }
}