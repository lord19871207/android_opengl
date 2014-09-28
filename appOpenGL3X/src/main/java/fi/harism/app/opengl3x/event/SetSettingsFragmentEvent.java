package fi.harism.app.opengl3x.event;

import android.app.Fragment;

public class SetSettingsFragmentEvent {

    private Fragment settingsFragment;

    public SetSettingsFragmentEvent(Fragment settingsFragment) {
        this.settingsFragment = settingsFragment;
    }

    public Fragment getFragment() {
        return settingsFragment;
    }

}
