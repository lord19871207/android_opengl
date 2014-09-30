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
    }

}
