package com.bringsalavat.pijon.adapter;

import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bringsalavat.pijon.R;
import com.bringsalavat.pijon.data.PrefsManager;
import com.bringsalavat.pijon.model.AppStats;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.ViewHolder> {

    private List<AppStats> data = new ArrayList<>();

    public void setData(List<AppStats> newData) {
        data = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_stat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(data.get(position));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView appIcon;
        private final TextView appName;
        private final TextView sessionCount;
        private final TextView totalTime;

        private final LinearProgressIndicator barFeelingPos;
        private final LinearProgressIndicator barFeelingNeu;
        private final LinearProgressIndicator barFeelingNeg;
        private final TextView pctFeelingPos;
        private final TextView pctFeelingNeu;
        private final TextView pctFeelingNeg;

        private final LinearProgressIndicator barUseYes;
        private final LinearProgressIndicator barUseUnsure;
        private final LinearProgressIndicator barUseNo;
        private final TextView pctUseYes;
        private final TextView pctUseUnsure;
        private final TextView pctUseNo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon      = itemView.findViewById(R.id.stat_app_icon);
            appName      = itemView.findViewById(R.id.stat_app_name);
            sessionCount = itemView.findViewById(R.id.stat_session_count);
            totalTime    = itemView.findViewById(R.id.stat_total_time);

            barFeelingPos = itemView.findViewById(R.id.bar_feeling_pos);
            barFeelingNeu = itemView.findViewById(R.id.bar_feeling_neu);
            barFeelingNeg = itemView.findViewById(R.id.bar_feeling_neg);
            pctFeelingPos = itemView.findViewById(R.id.pct_feeling_pos);
            pctFeelingNeu = itemView.findViewById(R.id.pct_feeling_neu);
            pctFeelingNeg = itemView.findViewById(R.id.pct_feeling_neg);

            barUseYes    = itemView.findViewById(R.id.bar_use_yes);
            barUseUnsure = itemView.findViewById(R.id.bar_use_unsure);
            barUseNo     = itemView.findViewById(R.id.bar_use_no);
            pctUseYes    = itemView.findViewById(R.id.pct_use_yes);
            pctUseUnsure = itemView.findViewById(R.id.pct_use_unsure);
            pctUseNo     = itemView.findViewById(R.id.pct_use_no);
        }

        void bind(AppStats s) {
            // App icon
            PackageManager pm = itemView.getContext().getPackageManager();
            try {
                appIcon.setImageDrawable(pm.getApplicationIcon(s.packageName));
            } catch (PackageManager.NameNotFoundException e) {
                appIcon.setImageDrawable(pm.getDefaultActivityIcon());
            }

            appName.setText(s.appName);
            sessionCount.setText(
                    itemView.getContext().getString(R.string.stats_check_ins, s.total));
            totalTime.setText(
                    itemView.getContext().getString(R.string.stats_app_total_time,
                            PrefsManager.formatDuration(s.totalDurationSeconds)));

            setBar(barFeelingPos, pctFeelingPos, s.pct(s.feelingPos));
            setBar(barFeelingNeu, pctFeelingNeu, s.pct(s.feelingNeu));
            setBar(barFeelingNeg, pctFeelingNeg, s.pct(s.feelingNeg));

            setBar(barUseYes,    pctUseYes,    s.pct(s.goodUseYes));
            setBar(barUseUnsure, pctUseUnsure, s.pct(s.goodUseUnsure));
            setBar(barUseNo,     pctUseNo,     s.pct(s.goodUseNo));
        }

        private void setBar(LinearProgressIndicator bar, TextView pct, int progress) {
            bar.setProgressCompat(progress, false);
            pct.setText(progress + "%");
        }
    }
}
