package fi.harism.app.opengl3x;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import de.greenrobot.event.EventBus;
import fi.harism.app.opengl3x.event.GetRendererFragmentEvent;
import fi.harism.app.opengl3x.event.SetRendererFragmentEvent;
import fi.harism.app.opengl3x.event.SplashDoneEvent;
import fi.harism.app.opengl3x.list.ListFragment;

public class MainActivity extends Activity {

    private Fragment splashFragment;
    private View viewSeparator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewSeparator = findViewById(R.id.view_separator);
        viewSeparator.setVisibility(View.GONE);
        viewSeparator.setAlpha(0f);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.fragment_splash, splashFragment = new SplashFragment());
        ft.commit();

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void onEvent(SplashDoneEvent event) {
        viewSeparator.setVisibility(View.VISIBLE);
        viewSeparator.animate().alpha(1f).setDuration(getResources()
                .getInteger(R.integer.splash_transition_time)).start();

        FragmentTransaction ft;
        ft = getFragmentManager().beginTransaction();
        ft.setCustomAnimations(0, R.animator.fragment_splash_out);
        ft.remove(splashFragment);
        ft.commit();

        ft = getFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.animator.fragment_list_in, 0);
        ft.add(R.id.fragment_list, new ListFragment());
        ft.commit();

        ft = getFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.animator.fragment_action_button_in, 0);
        ft.add(R.id.fragment_list, new ActionButtonFragment());
        ft.commit();

        getFragmentManager().executePendingTransactions();
        EventBus.getDefault().post(new GetRendererFragmentEvent());
    }

    public void onEvent(SetRendererFragmentEvent event) {
        if (splashFragment != null) {
            splashFragment = null;
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.animator.fragment_renderer_in, 0);
            ft.replace(R.id.fragment_renderer, event.getFragment());
            ft.commit();
        } else {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.replace(R.id.fragment_renderer, event.getFragment());
            ft.commit();
        }
    }

}
