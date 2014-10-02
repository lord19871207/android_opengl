package fi.harism.app.opengl3x;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

import de.greenrobot.event.EventBus;
import fi.harism.app.opengl3x.event.GetProgressEvent;
import fi.harism.app.opengl3x.event.SetProgressEvent;
import fi.harism.app.opengl3x.fragment.MainFragment;
import fi.harism.app.opengl3x.fragment.SplashFragment;

public class MainActivity extends Activity {

    private static final String TAG_SPLASH_FRAGMENT = "TAG_SPLASH_FRAGMENT";
    private static final String TAG_MAIN_FRAGMENT = "TAG_MAIN_FRAGMENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus eventBus = EventBus.getDefault();
        eventBus.register(this);
        eventBus.post(new GetProgressEvent());
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus eventBus = EventBus.getDefault();
        if (eventBus.isRegistered(this)) {
            eventBus.unregister(this);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        EventBus eventBus = EventBus.getDefault();
        if (eventBus.isRegistered(this)) {
            eventBus.unregister(this);
        }
    }

    public void onEvent(SetProgressEvent event) {
        FragmentManager fm = getFragmentManager();
        if (event.getProgress() == event.getMaxProgress() &&
                fm.findFragmentByTag(TAG_MAIN_FRAGMENT) == null) {
            Fragment f = new MainFragment();
            f.setRetainInstance(true);
            getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.animator.fragment_main_in, R.animator.fragment_splash_out)
                    .replace(R.id.fragment_main, f, TAG_MAIN_FRAGMENT)
                    .commit();
            getFragmentManager().executePendingTransactions();
        } else if (event.getProgress() != event.getMaxProgress() &&
                fm.findFragmentByTag(TAG_SPLASH_FRAGMENT) == null) {
            Fragment f = new SplashFragment();
            f.setRetainInstance(true);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(R.id.fragment_main, f, TAG_SPLASH_FRAGMENT);
            ft.commit();
        }
    }

}

