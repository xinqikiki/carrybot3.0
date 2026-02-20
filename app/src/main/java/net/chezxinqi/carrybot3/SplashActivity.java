package net.chezxinqi.carrybot3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private boolean finished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiPrefs.setLang(this, UiStrings.LANG_FR);
        UiPrefs.setTtsEnabled(this, false);
        TtsManager.warmUp(this);
        setContentView(R.layout.activity_splash);
        ContrastHelper.apply(findViewById(R.id.splashRoot), UiPrefs.isContrastEnabled(this));

        ImageView logo = findViewById(R.id.imgSplashLogo);
        TextView title = findViewById(R.id.txtSplashTitle);

        PropertyValuesHolder scaleX = PropertyValuesHolder.ofKeyframe(View.SCALE_X,
                Keyframe.ofFloat(0f, 1f),
                Keyframe.ofFloat(0.18f, 1.08f),
                Keyframe.ofFloat(0.32f, 0.96f),
                Keyframe.ofFloat(0.5f, 1.06f),
                Keyframe.ofFloat(0.7f, 0.98f),
                Keyframe.ofFloat(0.85f, 1.03f),
                Keyframe.ofFloat(1f, 1f)
        );
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofKeyframe(View.SCALE_Y,
                Keyframe.ofFloat(0f, 1f),
                Keyframe.ofFloat(0.18f, 1.08f),
                Keyframe.ofFloat(0.32f, 0.96f),
                Keyframe.ofFloat(0.5f, 1.06f),
                Keyframe.ofFloat(0.7f, 0.98f),
                Keyframe.ofFloat(0.85f, 1.03f),
                Keyframe.ofFloat(1f, 1f)
        );

        ObjectAnimator bounce = ObjectAnimator.ofPropertyValuesHolder(logo, scaleX, scaleY);
        bounce.setDuration(900);

        AnimatorSet set = new AnimatorSet();
        set.play(bounce);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                goNext();
            }
        });
        set.start();

        playTypewriter(title, "CarryBot", 70);
    }

    private void goNext() {
        if (finished) {
            return;
        }
        finished = true;
        startActivity(new Intent(this, DeviceSelectActivity.class));
        finish();
    }

    private void playTypewriter(TextView view, String text, long intervalMs) {
        view.setText("");
        view.postDelayed(new Runnable() {
            int index = 0;

            @Override
            public void run() {
                if (index <= text.length()) {
                    view.setText(text.substring(0, index));
                    index++;
                    view.postDelayed(this, intervalMs);
                }
            }
        }, intervalMs);
    }
}
