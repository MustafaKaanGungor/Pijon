package com.bringsalavat.pijon.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bringsalavat.pijon.R;
import com.bringsalavat.pijon.adapter.StatsAdapter;
import com.bringsalavat.pijon.data.PijonDatabase;
import com.bringsalavat.pijon.data.PrefsManager;
import com.bringsalavat.pijon.model.AppStats;
import com.bringsalavat.pijon.model.UsageLog;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class StatsFragment extends Fragment {

    private static final int PERIOD_TODAY = 0;
    private static final int PERIOD_WEEK  = 1;
    private static final int PERIOD_ALL   = 2;

    private int currentPeriod = PERIOD_ALL;

    private RecyclerView recycler;
    private TextView emptyState;
    private View summaryCard;
    private TextView totalTimeText;
    private TextView totalSessionsText;
    private StatsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        recycler          = view.findViewById(R.id.stats_recycler);
        emptyState        = view.findViewById(R.id.stats_empty);
        summaryCard       = view.findViewById(R.id.stats_summary);
        totalTimeText     = view.findViewById(R.id.stats_total_time_text);
        totalSessionsText = view.findViewById(R.id.stats_total_sessions_text);

        adapter = new StatsAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        ChipGroup chipGroup = view.findViewById(R.id.period_chip_group);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip_today) currentPeriod = PERIOD_TODAY;
            else if (id == R.id.chip_week) currentPeriod = PERIOD_WEEK;
            else currentPeriod = PERIOD_ALL;
            loadData();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    // ---------------------------------------------------------------
    // Data loading
    // ---------------------------------------------------------------

    private void loadData() {
        long fromMs = getFromMs();
        Executors.newSingleThreadExecutor().execute(() -> {
            List<UsageLog> logs = PijonDatabase.getInstance(requireContext())
                    .usageLogDao().getLogsSince(fromMs);
            List<AppStats> stats = computeStats(logs);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isAdded()) return;
                adapter.setData(stats);
                boolean empty = stats.isEmpty();
                emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
                summaryCard.setVisibility(empty ? View.GONE : View.VISIBLE);
                if (!empty) updateSummary(stats);
            });
        });
    }

    private void updateSummary(List<AppStats> stats) {
        int grandTotalSecs = 0;
        int grandTotalSessions = 0;
        for (AppStats s : stats) {
            grandTotalSecs += s.totalDurationSeconds;
            grandTotalSessions += s.total;
        }
        totalTimeText.setText(PrefsManager.formatDuration(grandTotalSecs));
        totalSessionsText.setText(String.valueOf(grandTotalSessions));
    }

    private long getFromMs() {
        if (currentPeriod == PERIOD_TODAY) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        } else if (currentPeriod == PERIOD_WEEK) {
            return System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
        }
        return 0L;
    }

    private List<AppStats> computeStats(List<UsageLog> logs) {
        Map<String, AppStats> map = new LinkedHashMap<>();
        for (UsageLog log : logs) {
            String pkg = log.getPackageName();
            AppStats s = map.get(pkg);
            if (s == null) {
                s = new AppStats();
                s.packageName = pkg;
                s.appName = log.getAppName();
                map.put(pkg, s);
            }
            s.total++;
            s.totalDurationSeconds += log.getDurationSeconds();
            if      (log.getFeeling() ==  1) s.feelingPos++;
            else if (log.getFeeling() == -1) s.feelingNeg++;
            else                             s.feelingNeu++;
            if      (log.getGoodUse() ==  1) s.goodUseYes++;
            else if (log.getGoodUse() == -1) s.goodUseNo++;
            else                             s.goodUseUnsure++;
        }
        return new ArrayList<>(map.values());
    }
}
