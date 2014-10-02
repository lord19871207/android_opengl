package fi.harism.app.opengl3x.fragment;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import de.greenrobot.event.EventBus;
import fi.harism.app.opengl3x.FadeImageView;
import fi.harism.app.opengl3x.FadeTextView;
import fi.harism.app.opengl3x.R;
import fi.harism.app.opengl3x.event.GetProgressEvent;
import fi.harism.app.opengl3x.event.SetProgressEvent;

public class SplashFragment extends Fragment {

    private boolean isRunning;
    private ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_splash, container, false);
        progressBar = (ProgressBar) view.findViewById(R.id.seekbar_progress);

        if (!isRunning) {
            isRunning = true;

            FadeTextView tv = (FadeTextView) view.findViewById(R.id.textview_title);
            tv.setAlphaLeft(0f);
            tv.setAlphaRight(0f);
            tv.setBlurLeft(1f);
            tv.setBlurRight(1f);

            FadeImageView iv = (FadeImageView) view.findViewById(R.id.imageview_icon);
            iv.setAlphaLeft(0f);
            iv.setAlphaRight(0f);
            iv.setBlurLeft(1f);
            iv.setBlurRight(1f);

            AnimatorSet animSet = new AnimatorSet();
            AnimatorSet animSetTv = createFade(tv);
            AnimatorSet animSetIv = createFade(iv);
            animSetIv.setStartDelay(500);
            animSet.playTogether(animSetTv, animSetIv);
            animSet.start();
        }

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void onEvent(SetProgressEvent event) {
        progressBar.setProgress(event.getProgress());
        progressBar.setMax(event.getMaxProgress());
    }

    private AnimatorSet createFade(View v) {
        ObjectAnimator animAlphaL = ObjectAnimator.ofFloat(v, "alphaLeft", .0f, .5f, 1.f, 1.f).setDuration(1000);
        ObjectAnimator animAlphaR = ObjectAnimator.ofFloat(v, "alphaRight", .0f, .0f, .5f, 1.f).setDuration(1000);
        ObjectAnimator animBlurL = ObjectAnimator.ofFloat(v, "blurLeft", 1.f, .5f, 0.f, .0f).setDuration(1000);
        ObjectAnimator animBlurR = ObjectAnimator.ofFloat(v, "blurRight", 1.f, 1.f, .5f, .0f).setDuration(1000);
        animBlurL.setStartDelay(500);
        animBlurR.setStartDelay(500);
        AnimatorSet animSet = new AnimatorSet();
        animSet.play(animBlurL).with(animBlurR).with(animAlphaL).with(animAlphaR);
        return animSet;
    }


}
