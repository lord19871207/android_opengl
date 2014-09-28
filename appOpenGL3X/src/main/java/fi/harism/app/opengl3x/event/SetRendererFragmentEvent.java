package fi.harism.app.opengl3x.event;

import android.app.Fragment;

public class SetRendererFragmentEvent {

    private Fragment rendererFragment;

    public SetRendererFragmentEvent(Fragment rendererFragment) {
        this.rendererFragment = rendererFragment;
    }

    public Fragment getFragment() {
        return rendererFragment;
    }

}
