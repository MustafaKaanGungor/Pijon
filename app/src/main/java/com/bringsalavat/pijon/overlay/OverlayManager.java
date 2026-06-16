package com.bringsalavat.pijon.overlay;

import android.content.Context;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.bringsalavat.pijon.R;
import com.bringsalavat.pijon.data.PrefsManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

/**
 * Manages the system overlay pop-up shown when the usage timer expires.
 *
 * Flow:
 *   1. Card animates in; countdown from 5 → 0 plays (each digit fades out/in).
 *   2. Countdown container fades out; questions container fades in.
 *   3. User selects an answer for both questions; Done button activates.
 *   4. Tapping Done fires the callback and dismisses the overlay.
 *
 * Response values use -1 / 0 / 1 (negative / neutral / positive).
 */
public class OverlayManager {

    public interface OnResponseListener {
        /**
         * Called when the user submits both answers.
         *
         * @param feeling  How the session made them feel: 1=positive, 0=neutral, -1=negative
         * @param goodUse  Whether it was good use of time: 1=yes, 0=unsure, -1=no
         */
        void onResponse(int feeling, int goodUse);
    }

    private static final int COUNTDOWN_START = 5;

    private final Context context;
    private final WindowManager windowManager;

    private View overlayView;
    private boolean isShowing = false;

    // Countdown state
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingTick;
    private int countdownValue;

    // Views held across methods
    private TextView countdownNumber;
    private View countdownContainer;
    private View questionsContainer;
    private MaterialButtonToggleGroup toggleQ1;
    private MaterialButtonToggleGroup toggleQ2;
    private MaterialButton btnDone;

    public OverlayManager(Context context) {
        this.context = context.getApplicationContext();
        this.windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
    }

    /**
     * Show the reminder pop-up overlay.
     *
     * @param appName           Display name of the monitored app
     * @param formattedDuration Human-readable duration (e.g. "5 minutes")
     * @param listener          Called with both answers when user taps Done
     */
    public void show(String appName, String formattedDuration, OnResponseListener listener) {
        if (isShowing) return;

        android.view.ContextThemeWrapper themedContext =
                new android.view.ContextThemeWrapper(context, R.style.Theme_Pijon);
        LayoutInflater inflater = LayoutInflater.from(themedContext);
        overlayView = inflater.inflate(R.layout.overlay_popup, null);

        // Bind views
        TextView messageText   = overlayView.findViewById(R.id.overlay_message);
        TextView question1Text = overlayView.findViewById(R.id.question1_text);
        countdownNumber    = overlayView.findViewById(R.id.countdown_number);
        countdownContainer = overlayView.findViewById(R.id.countdown_container);
        questionsContainer = overlayView.findViewById(R.id.questions_container);
        toggleQ1           = overlayView.findViewById(R.id.toggle_q1);
        toggleQ2           = overlayView.findViewById(R.id.toggle_q2);
        btnDone            = overlayView.findViewById(R.id.btn_done);

        // Fill in dynamic text
        messageText.setText(context.getString(R.string.overlay_message, appName, formattedDuration));
        question1Text.setText(context.getString(R.string.overlay_question1, formattedDuration));

        // Random countdown hint
        TextView countdownHint = overlayView.findViewById(R.id.countdown_hint);
        String[] hints = context.getResources().getStringArray(R.array.countdown_hints);
        countdownHint.setText(hints[(int) (System.currentTimeMillis() % hints.length)]);

        // Enable Done only when both questions have an answer
        MaterialButtonToggleGroup.OnButtonCheckedListener selectionWatcher =
                (group, checkedId, isChecked) -> refreshDoneButton();
        toggleQ1.addOnButtonCheckedListener(selectionWatcher);
        toggleQ2.addOnButtonCheckedListener(selectionWatcher);

        btnDone.setOnClickListener(v -> {
            int feeling = scoreFrom(toggleQ1,
                    R.id.btn_q1_positive, R.id.btn_q1_neutral, R.id.btn_q1_negative);
            int goodUse = scoreFrom(toggleQ2,
                    R.id.btn_q2_positive, R.id.btn_q2_neutral, R.id.btn_q2_negative);
            dismiss();
            if (listener != null) listener.onResponse(feeling, goodUse);
        });

        // Window params — full-screen translucent overlay
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.CENTER;

        windowManager.addView(overlayView, params);
        isShowing = true;

        // Card entrance animation
        View card = overlayView.findViewById(R.id.overlay_card);
        Animation enterAnim = AnimationUtils.loadAnimation(themedContext, R.anim.overlay_enter);
        card.startAnimation(enterAnim);

        // Sound / vibration
        PrefsManager prefs = new PrefsManager(context);
        if (prefs.isSoundEnabled()) playNotificationSound();
        if (prefs.isVibrationEnabled()) vibrate();

        // Start the 5-second countdown
        startCountdown();
    }

    public void dismiss() {
        cancelCountdown();
        if (isShowing && overlayView != null) {
            // Cancel any running view animations
            if (countdownNumber != null) countdownNumber.animate().cancel();
            if (questionsContainer != null) questionsContainer.animate().cancel();

            try {
                windowManager.removeView(overlayView);
            } catch (IllegalArgumentException e) {
                // View was already removed
            }
            overlayView = null;
            isShowing = false;
        }
    }

    public boolean isShowing() {
        return isShowing;
    }

    // ---------------------------------------------------------------
    // Countdown
    // ---------------------------------------------------------------

    private void startCountdown() {
        countdownValue = COUNTDOWN_START;
        countdownNumber.setText(String.valueOf(countdownValue));
        countdownNumber.setAlpha(1f);
        scheduleNextTick();
    }

    private void scheduleNextTick() {
        pendingTick = () -> {
            countdownValue--;
            if (countdownValue <= 0) {
                // Fade out the countdown number, then reveal questions
                countdownNumber.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction(this::revealQuestions)
                        .start();
            } else {
                // Fade out → update text → fade in
                countdownNumber.animate()
                        .alpha(0f)
                        .setDuration(180)
                        .withEndAction(() -> {
                            countdownNumber.setText(String.valueOf(countdownValue));
                            countdownNumber.animate()
                                    .alpha(1f)
                                    .setDuration(180)
                                    .start();
                        })
                        .start();
                scheduleNextTick();
            }
        };
        handler.postDelayed(pendingTick, 1000);
    }

    private void cancelCountdown() {
        if (pendingTick != null) {
            handler.removeCallbacks(pendingTick);
            pendingTick = null;
        }
    }

    // ---------------------------------------------------------------
    // Transition: countdown → questions
    // ---------------------------------------------------------------

    private void revealQuestions() {
        if (!isShowing) return;

        // Fade out the whole countdown container, then swap visibility
        countdownContainer.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    countdownContainer.setVisibility(View.GONE);

                    questionsContainer.setVisibility(View.VISIBLE);
                    questionsContainer.setAlpha(0f);
                    questionsContainer.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start();
                })
                .start();
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void refreshDoneButton() {
        boolean bothAnswered = toggleQ1.getCheckedButtonId() != View.NO_ID
                && toggleQ2.getCheckedButtonId() != View.NO_ID;
        btnDone.setEnabled(bothAnswered);
        btnDone.setAlpha(bothAnswered ? 1f : 0.4f);
    }

    /**
     * Map a toggle group's checked button to a score (-1 / 0 / 1).
     */
    private int scoreFrom(MaterialButtonToggleGroup group,
                          int positiveId, int neutralId, int negativeId) {
        int checked = group.getCheckedButtonId();
        if (checked == positiveId) return 1;
        if (checked == negativeId) return -1;
        return 0; // neutral or nothing checked
    }

    private void playNotificationSound() {
        try {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            android.media.Ringtone ringtone = RingtoneManager.getRingtone(context, soundUri);
            if (ringtone != null) {
                ringtone.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                ringtone.play();
            }
        } catch (Exception e) {
            // Non-critical — silently ignore
        }
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            VibrationEffect effect = VibrationEffect.createOneShot(
                    200, VibrationEffect.DEFAULT_AMPLITUDE);
            vibrator.vibrate(effect);
        }
    }
}
