package fi.harism.app.opengl3x;

import android.app.Activity;
import android.os.Bundle;

import de.greenrobot.event.EventBus;
import fi.harism.app.opengl3x.event.GetRendererFragmentEvent;
import fi.harism.app.opengl3x.event.SetRendererFragmentEvent;

public class RendererActivity extends Activity {

    private static final String TAG_RENDERER_FRAGMENT = "tag.renderer_fragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_renderer);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getFragmentManager().findFragmentByTag(TAG_RENDERER_FRAGMENT) == null) {
            EventBus.getDefault().post(new GetRendererFragmentEvent());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void onEvent(SetRendererFragmentEvent event) {
        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.animator.fragment_renderer_switch_in, R.animator.fragment_renderer_switch_out)
                .replace(R.id.container_renderer, event.getFragment(), TAG_RENDERER_FRAGMENT)
                .commit();
    }

}
