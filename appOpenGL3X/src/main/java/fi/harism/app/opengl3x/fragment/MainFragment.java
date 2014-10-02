package fi.harism.app.opengl3x.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.greenrobot.event.EventBus;
import fi.harism.app.opengl3x.R;
import fi.harism.app.opengl3x.event.GetRendererFragmentEvent;
import fi.harism.app.opengl3x.event.SetRendererFragmentEvent;

public class MainFragment extends Fragment {

    private Fragment listFragment;
    private Fragment rendererFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        if (listFragment == null) {
            listFragment = new ListFragment();
            getFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.animator.fragment_list_in, 0)
                    .replace(R.id.fragment_list, listFragment)
                    .commit();
        } else {
            getFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(0, 0)
                    .replace(R.id.fragment_list, listFragment)
                    .commit();
        }

        if (rendererFragment != null) {
            getFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(0, 0)
                    .replace(R.id.fragment_renderer, rendererFragment)
                    .commit();
        }

        return view;
    }

    public void onStart() {
        super.onStart();
        if (rendererFragment == null) {
            EventBus.getDefault().post(new GetRendererFragmentEvent());
        }
    }

    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void onEvent(SetRendererFragmentEvent event) {
        if (rendererFragment == null) {
            getFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.animator.fragment_renderer_in, 0)
                    .replace(R.id.fragment_renderer, event.getFragment())
                    .commit();
        } else {
            getFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.animator.fragment_renderer_switch_in, R.animator.fragment_renderer_switch_out)
                    .replace(R.id.fragment_renderer, event.getFragment())
                    .commit();
        }
        rendererFragment = event.getFragment();
    }

}
