package fi.harism.app.opengl3x;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;

import de.greenrobot.event.EventBus;
import fi.harism.app.opengl3x.event.SetProgressEvent;
import fi.harism.app.opengl3x.event.SetRendererFragmentEvent;
import fi.harism.app.opengl3x.fragment.MainFragment;
import fi.harism.app.opengl3x.fragment.SplashFragment;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            Fragment f = new SplashFragment();
            f.setRetainInstance(true);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(R.id.fragment_main, f);
            ft.commit();
        }

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void onEvent(SetProgressEvent event) {
        if (event.getProgress() == event.getMaxProgress()) {
            Fragment f = new MainFragment();
            f.setRetainInstance(true);
            getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.animator.fragment_main_in, R.animator.fragment_splash_out)
                    .replace(R.id.fragment_main, f)
                    .commit();
            getFragmentManager().executePendingTransactions();
        }


        /*
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
        */
    }

    public void onEvent(SetRendererFragmentEvent event) {
        /*
        if (splashFragment != null) {
            splashFragment = null;
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.animator.fragment_renderer_in, 0);
            ft.replace(R.id.fragment_renderer, event.getFragment());
            ft.commit();
        } else {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.animator.fragment_renderer_switch_in, R.animator.fragment_renderer_switch_out);
            ft.replace(R.id.fragment_renderer, event.getFragment());
            ft.commit();
        }
        */
    }

}
