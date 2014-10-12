package fi.harism.app.opengl3x.fragment.test;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.opengl.GLES30;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import de.greenrobot.event.EventBus;
import fi.harism.app.opengl3x.R;
import fi.harism.app.opengl3x.fragment.RendererFragment;

public class ClearRendererFragment extends RendererFragment {

    private static final String RENDERER_ID = "renderer.test.clear";
    private static final String PREFERENCE_COLOR = "renderer.test.clear.color";
    private static final int DEFAULT_COLOR = Color.WHITE;

    private int clearColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs =
                getActivity().getPreferences(Context.MODE_PRIVATE);
        clearColor = prefs.getInt(PREFERENCE_COLOR, DEFAULT_COLOR);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public String getRendererId() {
        return RENDERER_ID;
    }

    @Override
    public int getTitleStringId() {
        return R.string.renderer_test_clear_title;
    }

    @Override
    public int getCaptionStringId() {
        return R.string.renderer_test_clear_caption;
    }

    @Override
    public Fragment getSettingsFragment() {
        return new SettingsFragment();
    }

    @Override
    public void onSurfaceCreated() {
        setContinuousRendering(true);
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
    }

    @Override
    public void onRenderFrame() {
        float rand = (float) Math.random();
        float r = rand * (Color.red(clearColor) / 255f);
        float g = rand * (Color.green(clearColor) / 255f);
        float b = rand * (Color.blue(clearColor) / 255f);
        GLES30.glClearColor(r, g, b, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
    }

    @Override
    public void onSurfaceReleased() {
    }

    public void onEvent(SettingsEvent event) {
        clearColor = event.color;
    }

    public static class SettingsFragment extends Fragment {

        private final SeekBar.OnSeekBarChangeListener
                seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                SettingsEvent event = new SettingsEvent();
                event.color = Color.rgb(
                        seekBarR.getProgress(),
                        seekBarG.getProgress(),
                        seekBarB.getProgress());
                EventBus.getDefault().post(event);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int color = Color.rgb(
                        seekBarR.getProgress(),
                        seekBarG.getProgress(),
                        seekBarB.getProgress());
                SharedPreferences.Editor editor =
                        getActivity().getPreferences(Context.MODE_PRIVATE).edit();
                editor.putInt(PREFERENCE_COLOR, color);
                editor.apply();
            }
        };

        private SeekBar seekBarR;
        private SeekBar seekBarG;
        private SeekBar seekBarB;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_settings_test_clear, container, false);

            seekBarR = (SeekBar) view.findViewById(R.id.seekbar_red);
            seekBarG = (SeekBar) view.findViewById(R.id.seekbar_green);
            seekBarB = (SeekBar) view.findViewById(R.id.seekbar_blue);

            int color = getActivity().getPreferences(Context.MODE_PRIVATE)
                    .getInt(PREFERENCE_COLOR, DEFAULT_COLOR);
            seekBarR.setProgress(Color.red(color));
            seekBarG.setProgress(Color.green(color));
            seekBarB.setProgress(Color.blue(color));

            seekBarR.setOnSeekBarChangeListener(seekBarChangeListener);
            seekBarG.setOnSeekBarChangeListener(seekBarChangeListener);
            seekBarB.setOnSeekBarChangeListener(seekBarChangeListener);

            return view;
        }

    }

    public static class SettingsEvent {
        public int color;
    }

}
