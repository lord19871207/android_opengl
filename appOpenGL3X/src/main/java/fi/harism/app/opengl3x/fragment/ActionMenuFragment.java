package fi.harism.app.opengl3x.fragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Outline;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;

import de.greenrobot.event.EventBus;
import fi.harism.app.opengl3x.R;

public class ActionMenuFragment extends Fragment {

    private static final String TAG_LIST_FRAGMENT = "tag.list_fragment";

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            setActionMenuVisible(!isMenuVisible);
        }
    };

    private final FragmentManager.OnBackStackChangedListener onBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        @Override
        public void onBackStackChanged() {
            FragmentManager fm = getFragmentManager();
            setActionButtonRotated(isMenuVisible = !isMenuVisible);
            if (fm.findFragmentByTag(TAG_LIST_FRAGMENT) == null) {
                fm.beginTransaction().remove(listFragment).add(listFragment, TAG_LIST_FRAGMENT).commit();
            }
        }
    };

    private Fragment listFragment;
    private View actionButton;
    private View actionButtonOverlay;
    private View containerSettings;
    private boolean isMenuVisible = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getFragmentManager();
        listFragment = fm.findFragmentByTag(TAG_LIST_FRAGMENT);
        if (listFragment == null) {
            listFragment = new ListFragment();
            listFragment.setRetainInstance(true);
            fm.beginTransaction().add(listFragment, TAG_LIST_FRAGMENT).commit();
        }
        fm.addOnBackStackChangedListener(onBackStackChangedListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_action_button, container, false);

        actionButton = view.findViewById(R.id.action_button);
        actionButtonOverlay = view.findViewById(R.id.action_button_overlay);
        containerSettings = view.findViewById(R.id.container_settings);

        actionButton.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        actionButton.setClipToOutline(true);
        actionButton.setOnClickListener(onClickListener);

        actionButtonOverlay.setOnClickListener(onClickListener);
        actionButtonOverlay.setClickable(false);
        actionButtonOverlay.setAlpha(0f);

        containerSettings.setVisibility(View.GONE);
        containerSettings.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), getResources().getDimension(R.dimen.settings_round_radius));
            }
        });
        containerSettings.setClipToOutline(true);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getFragmentManager().removeOnBackStackChangedListener(onBackStackChangedListener);
    }

    private void setActionMenuVisible(boolean visible) {
        if (visible) {
            FragmentManager fm = getFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.setCustomAnimations(R.animator.fragment_settings_in, R.animator.fragment_settings_out);
            ft.remove(listFragment);
            ft.add(R.id.container_settings, listFragment, TAG_LIST_FRAGMENT);
            ft.addToBackStack(null);
            ft.commit();
        } else {
            getFragmentManager().popBackStack();
        }
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
