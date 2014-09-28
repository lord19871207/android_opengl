package fi.harism.app.opengl3x;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.graphics.Outline;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import fi.harism.app.opengl3x.list.ListFragment;
import fi.harism.app.opengl3x.renderer.test.RandRendererFragment;

public class MainActivity extends Activity {

    private Fragment fragmentSplash;
    private View viewSeparator;
    private View actionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewSeparator = findViewById(R.id.view_separator);
        viewSeparator.setVisibility(View.GONE);
        viewSeparator.setAlpha(0f);
        actionButton = findViewById(R.id.action_button);
        actionButton.setVisibility(View.GONE);
        actionButton.setAlpha(0f);

        int w = actionButton.getLayoutParams().width;
        Outline outline = new Outline();
        outline.setOval(0, 0, w, w);
        actionButton.setOutline(outline);
        actionButton.setClipToOutline(true);
        actionButton.setOnClickListener(new View.OnClickListener() {
            private boolean isRotated = false;

            @Override
            public void onClick(View view) {
                if (isRotated) {
                    view.animate().rotation(0f).setDuration(300).start();
                } else {
                    view.animate().rotation(180f).setDuration(300).start();
                }
                isRotated = !isRotated;
            }
        });

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.fragment_splash, fragmentSplash = new SplashFragment());
        ft.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("onDestroy", "..called..");
    }

    public void onSplashDone() {
        viewSeparator.setVisibility(View.VISIBLE);
        viewSeparator.animate().alpha(1f).setDuration(getResources()
                .getInteger(R.integer.splash_transition_time)).start();

        actionButton.setVisibility(View.VISIBLE);
        actionButton.animate().alpha(1f).setDuration(getResources()
                .getInteger(R.integer.splash_transition_time)).start();

        FragmentTransaction ft;
        ft = getFragmentManager().beginTransaction();
        ft.setCustomAnimations(0, R.animator.fragment_splash_out);
        ft.remove(fragmentSplash);
        ft.commit();

        ft = getFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.animator.fragment_renderer_in, 0);
        ft.add(R.id.fragment_renderer, new RandRendererFragment());
        ft.commit();

        ft = getFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.animator.fragment_list_in, 0);
        ft.add(R.id.fragment_list, new ListFragment());
        ft.commit();
    }

}
