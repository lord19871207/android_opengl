package fi.harism.app.opengl3x.fragment.basic;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import de.greenrobot.event.EventBus;
import fi.harism.app.opengl3x.R;
import fi.harism.lib.opengl.egl.EglCore;
import fi.harism.lib.opengl.gl.GlProgram;
import fi.harism.lib.opengl.gl.GlUtils;

public class CubeBasicRendererFragment extends BasicRendererFragment {

    private static final String RENDERER_ID = "renderer.basic.cube";
    private static final String PREFERENCE_DRAW_OUTLINES = "renderer.basic.cube.draw_outlines";
    private static final boolean DEFAULT_DRAW_OUTLINES = false;

    private GlProgram glProgram;

    private long lastRenderTime;

    private boolean showOutlines;

    private final float projectionMatrix[] = new float[16];
    private final float lookAtMatrix[] = new float[16];
    private final float rotationMatrix[] = new float[16];
    private final float modelViewProjectionMatrix[] = new float[16];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEglFlags(EglCore.FLAG_DEPTH_BUFFER);

        showOutlines = getActivity()
                .getPreferences(Context.MODE_PRIVATE)
                .getBoolean(PREFERENCE_DRAW_OUTLINES, DEFAULT_DRAW_OUTLINES);
    }

    @Override
    public Fragment getSettingsFragment() {
        return new SettingsFragment();
    }

    @Override
    public String getRendererId() {
        return RENDERER_ID;
    }

    @Override
    public int getTitleStringId() {
        return R.string.renderer_basic_cube_title;
    }

    @Override
    public int getCaptionStringId() {
        return R.string.renderer_basic_cube_caption;
    }

    @Override
    public void onSurfaceCreated() {
        super.onSurfaceCreated();

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glCullFace(GLES30.GL_BACK);
        GLES30.glFrontFace(GLES30.GL_CCW);

        try {
            glProgram = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/basic/cube/shader_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/basic/cube/shader_fs.txt"),
                    null);
        } catch (final Exception ex) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        float aspect = (float) width / height;
        Matrix.perspectiveM(projectionMatrix, 0, 60.0f, aspect, 1f, 100f);
        Matrix.setLookAtM(lookAtMatrix, 0, 0f, 0f, 4f, 0f, 0f, 0f, 0f, 1f, 0f);
        Matrix.setIdentityM(rotationMatrix, 0);
    }

    @Override
    public void onRenderFrame() {
        GLES30.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        glProgram.useProgram();

        long time = SystemClock.uptimeMillis();
        float diff = (time - lastRenderTime) / 10f;
        lastRenderTime = time;

        Matrix.rotateM(rotationMatrix, 0, diff, 1f, 1.5f, 0f);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, lookAtMatrix, 0, rotationMatrix, 0);

        GLES30.glUniformMatrix4fv(glProgram.getUniformLocation("uModelViewMatrix"), 1, false, modelViewProjectionMatrix, 0);

        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewProjectionMatrix, 0);

        GLES30.glUniformMatrix4fv(glProgram.getUniformLocation("uModelViewProjectionMatrix"), 1, false, modelViewProjectionMatrix, 0);

        GLES30.glEnable(GLES30.GL_POLYGON_OFFSET_FILL);
        GLES30.glPolygonOffset(1f, 1f);
        GLES30.glUniform3f(glProgram.getUniformLocation("uColor"), 1f, 1f, 1f);
        renderCubeFilled();
        GLES30.glDisable(GLES30.GL_POLYGON_OFFSET_FILL);

        if (showOutlines) {
            GLES30.glLineWidth(4f);
            GLES30.glUniform3f(glProgram.getUniformLocation("uColor"), .4f, .6f, 1f);
            renderCubeOutlines();
        }
    }

    @Override
    public void onSurfaceReleased() {
    }

    public void onEvent(SettingsEvent event) {
        showOutlines = event.getShowOutlines();
    }

    public static class SettingsFragment extends Fragment {

        private final CheckBox.OnCheckedChangeListener onCheckedListener =
                new CheckBox.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                        SharedPreferences.Editor editor =
                                getActivity().getPreferences(Context.MODE_PRIVATE).edit();
                        editor.putBoolean(PREFERENCE_DRAW_OUTLINES, checked);
                        editor.commit();

                        EventBus.getDefault().post(new SettingsEvent(checked));
                    }
                };

        private CheckBox checkBoxShowOutlines;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_settings_basic_cube, container, false);

            checkBoxShowOutlines = (CheckBox) view.findViewById(R.id.checkbox_show_outlines);
            checkBoxShowOutlines.setOnCheckedChangeListener(onCheckedListener);
            checkBoxShowOutlines.setChecked(
                    getActivity()
                            .getPreferences(Context.MODE_PRIVATE)
                            .getBoolean(PREFERENCE_DRAW_OUTLINES, DEFAULT_DRAW_OUTLINES)
            );

            return view;
        }
    }

    public static class SettingsEvent {

        private boolean showOutlines;

        public SettingsEvent(boolean showOutLines) {
            this.showOutlines = showOutLines;
        }

        public boolean getShowOutlines() {
            return showOutlines;
        }

    }
}
