package fi.harism.app.opengl3x;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SplashFragment extends Fragment {

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_splash, null);

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

        return view;
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

    @Override
    public void onResume() {
        super.onResume();
        CountDownTimer cdt = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                ((MainActivity) getActivity()).onSplashDone();
            }
        };
        cdt.start();
    }

}
