package com.grace.eden_smart;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.VideoView;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.view.View;
import android.graphics.Color;

import androidx.appcompat.app.AppCompatActivity;

public class IntroActivity extends AppCompatActivity {
    private FullScreenVideoView videoView;
    private boolean navigated = false;

    // Mesure la vidéo en conservant ses proportions et en couvrant tout l'écran.
    private static class FullScreenVideoView extends VideoView {
        private int videoWidth;
        private int videoHeight;

        FullScreenVideoView(android.content.Context context) { super(context); }

        void setVideoSize(int width, int height) {
            videoWidth = width;
            videoHeight = height;
            requestLayout();
        }

        @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            if (videoWidth > 0 && videoHeight > 0 && width > 0 && height > 0) {
                // Recul du cadrage (15 %) pour éviter de couper les textes intégrés à la vidéo,
                // tout en gardant la vidéo centrée et ses proportions.
                float scale = Math.max((float) width / videoWidth, (float) height / videoHeight) * 0.85f;
                setMeasuredDimension(Math.round(videoWidth * scale), Math.round(videoHeight * scale));
            } else {
                setMeasuredDimension(width, height);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(18, 31, 79));
        getWindow().setNavigationBarColor(Color.rgb(18, 31, 79));
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(18, 31, 79));
        videoView = new FullScreenVideoView(this);
        FrameLayout.LayoutParams videoParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER);
        root.addView(videoView, videoParams);

        TextView fallback = new TextView(this);
        fallback.setText("EDEN FAMILY SCHOOL\nPreparing your attendance...");
        boolean night = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        fallback.setTextColor(night ? getColor(R.color.eden_gold) : Color.WHITE);
        fallback.setTextSize(20);
        fallback.setGravity(Gravity.CENTER);
        fallback.setVisibility(View.GONE);
        root.addView(fallback, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        int video = getResources().getIdentifier("intro_app", "raw", getPackageName());
        if (video == 0) {
            fallback.setVisibility(View.VISIBLE);
            root.postDelayed(this::openAuthentication, 1800);
            return;
        }
        videoView.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + video));
        videoView.setOnPreparedListener(player -> {
            player.setLooping(false);
            player.setVolume(1f, 1f);
            player.setOnVideoSizeChangedListener((mp, width, height) -> videoView.setVideoSize(width, height));
            if (player.getVideoWidth() > 0 && player.getVideoHeight() > 0) {
                videoView.setVideoSize(player.getVideoWidth(), player.getVideoHeight());
            }
            // Crop the video to fill the entire screen instead of leaving empty bands.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            }
            videoView.start();
        });
        videoView.setOnCompletionListener(player -> openAuthentication());
        videoView.setOnErrorListener((player, what, extra) -> {
            openAuthentication();
            return true;
        });
        // Never trap the user on the intro if the media file or decoder is unavailable.
        root.postDelayed(this::openAuthentication, 60000);
    }

    private void openAuthentication() {
        if (navigated || isFinishing()) return;
        navigated = true;
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onStop() {
        if (videoView != null && videoView.isPlaying()) videoView.pause();
        super.onStop();
    }
}