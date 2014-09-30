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
import fi.harism.app.opengl3x.event.SetColorEvent;
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

    public void onEvent(SetColorEvent event) {
        clearColor = event.getColor();
    }

    public static class SettingsFragment extends Fragment {

        private final SeekBar.OnSeekBarChangeListener
                seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int color = Color.rgb(
                        seekBarRed.getProgress(),
                        seekBarGreen.getProgress(),
                        seekBarBlue.getProgress());
                EventBus.getDefault().post(new SetColorEvent(color));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int color = Color.rgb(
                        seekBarRed.getProgress(),
                        seekBarGreen.getProgress(),
                        seekBarBlue.getProgress());
                SharedPreferences.Editor editor =
                        getActivity().getPreferences(Context.MODE_PRIVATE).edit();
                editor.putInt(PREFERENCE_COLOR, color);
                editor.commit();
            }
        };

        private SeekBar seekBarRed;
        private SeekBar seekBarGreen;
        private SeekBar seekBarBlue;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_settings_test_clear, container, false);

            seekBarRed = (SeekBar) view.findViewById(R.id.seekbar_red);
            seekBarGreen = (SeekBar) view.findViewById(R.id.seekbar_green);
            seekBarBlue = (SeekBar) view.findViewById(R.id.seekbar_blue);
            seekBarRed.setOnSeekBarChangeListener(seekBarChangeListener);
            seekBarGreen.setOnSeekBarChangeListener(seekBarChangeListener);
            seekBarBlue.setOnSeekBarChangeListener(seekBarChangeListener);

            int color = getActivity().getPreferences(Context.MODE_PRIVATE)
                    .getInt(PREFERENCE_COLOR, DEFAULT_COLOR);
            seekBarRed.setProgress(Color.red(color));
            seekBarGreen.setProgress(Color.green(color));
            seekBarBlue.setProgress(Color.blue(color));

            return view;
        }

    }

}
