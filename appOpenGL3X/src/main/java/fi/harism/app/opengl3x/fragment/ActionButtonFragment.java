package fi.harism.app.opengl3x.fragment;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Outline;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.greenrobot.event.EventBus;
import fi.harism.app.opengl3x.R;
import fi.harism.app.opengl3x.event.GetSettingsFragmentEvent;
import fi.harism.app.opengl3x.event.SetSettingsFragmentEvent;

public class ActionButtonFragment extends Fragment {

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!isSelected) {
                EventBus.getDefault().post(new GetSettingsFragmentEvent());
            } else {
                getFragmentManager().popBackStack();
            }
        }
    };

    private View actionButton;
    private View actionButtonOverlay;
    private View containerSettings;
    private boolean isSelected = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_action_button, container, false);

        actionButton = view.findViewById(R.id.action_button);
        actionButtonOverlay = view.findViewById(R.id.action_button_overlay);
        containerSettings = view.findViewById(R.id.container_settings);

        int w = actionButton.getLayoutParams().width;
        Outline outline = new Outline();
        outline.setOval(0, 0, w, w);
        actionButton.setOutline(outline);
        actionButton.setClipToOutline(true);
        actionButton.setOnClickListener(onClickListener);

        actionButtonOverlay.setOnClickListener(onClickListener);
        actionButtonOverlay.setClickable(false);
        actionButtonOverlay.setAlpha(0f);

        containerSettings.setVisibility(View.GONE);
        containerSettings.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v,
                                       int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                Outline outline = new Outline();
                outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(), getResources().getDimension(R.dimen.settings_round_radius));
                v.setOutline(outline);
                v.setClipToOutline(true);
            }
        });

        getFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                setActionButtonRotated(isSelected = !isSelected);
            }
        });

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void onEvent(SetSettingsFragmentEvent event) {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment settingsFragment = event.getFragment();
        ft.setCustomAnimations(R.animator.fragment_settings_in, R.animator.fragment_settings_out);
        ft.replace(R.id.container_settings, settingsFragment);
        ft.addToBackStack(null);
        ft.commit();
        fm.executePendingTransactions();
    }

    private void setActionButtonRotated(boolean rotated) {
        int duration = getResources().getInteger(R.integer.action_button_transition_time);
        if (rotated) {
            containerSettings.setAlpha(0f);
            containerSettings.setVisibility(View.VISIBLE);
            containerSettings.animate().alpha(1f).setDuration(duration).start();
            actionButton.animate().rotation(180f).setDuration(duration).start();
            actionButtonOverlay.animate().alpha(1f).setDuration(duration).start();
            actionButtonOverlay.setClickable(true);
        } else {
            containerSettings.setVisibility(View.GONE);
            actionButton.animate().rotation(0f).setDuration(duration).start();
            actionButtonOverlay.animate().alpha(0f).setDuration(duration).start();
            actionButtonOverlay.setClickable(false);
        }

    }

}
