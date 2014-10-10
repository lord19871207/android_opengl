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
import fi.harism.lib.opengl.gl.GlSampler;
import fi.harism.lib.opengl.gl.GlTexture;
import fi.harism.lib.opengl.gl.GlUtils;
import fi.harism.lib.opengl.model.GlCamera;

public class CubemapBasicRendererFragment extends BasicRendererFragment {

    private static final String RENDERER_ID = "renderer.basic.cubemap";
    private static final String PREFERENCE_ENABLE_LIGHTNING = "renderer.basic.cubemap.enable_lightning";
    private static final boolean DEFAULT_ENABLE_LIGHTNING = true;

    private static final float MATERIAL_LIGHTNING_DISABLED[] = {1f, 0f, 0f, 1f};
    private static final float MATERIAL_LIGHTNING_ENABLED[] = {0.4f, 1.0f, 1.0f, 8.0f};

    private GlSampler glSamplerLinear;
    private GlTexture glTextureCubemap;
    private GlProgram glProgram;
    private GlCamera glCamera;

    private long lastRenderTime;

    private boolean enableLightning;

    private final float rotationMatrix[] = new float[16];
    private final float modelMatrix[] = new float[16];
    private final float modelViewMatrix[] = new float[16];
    private final float modelViewProjectionMatrix[] = new float[16];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEglFlags(EglCore.FLAG_DEPTH_BUFFER);

        enableLightning = getActivity()
                .getPreferences(Context.MODE_PRIVATE)
                .getBoolean(PREFERENCE_ENABLE_LIGHTNING, DEFAULT_ENABLE_LIGHTNING);
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
        return R.string.renderer_basic_cubemap_title;
    }

    @Override
    public int getCaptionStringId() {
        return R.string.renderer_basic_cubemap_caption;
    }

    @Override
    public void onSurfaceCreated() {
        super.onSurfaceCreated();

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glCullFace(GLES30.GL_BACK);
        GLES30.glFrontFace(GLES30.GL_CCW);

        glSamplerLinear = new GlSampler();
        glSamplerLinear.parameter(GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        glSamplerLinear.parameter(GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        glSamplerLinear.parameter(GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        glSamplerLinear.parameter(GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        glCamera = new GlCamera();

        try {
            glTextureCubemap = readCubeMap();
            glProgram = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/basic/cubemap/shader_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/basic/cubemap/shader_fs.txt"),
                    null).useProgram();
            GLES30.glUniform1i(glProgram.getUniformLocation("uTextureCube"), 0);
            setContinuousRendering(true);
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
        glCamera.setPerspective(width, height, 60.0f, 1f, 100f);
        glCamera.setPos(new float[] {0f, 0f, 40f});
        Matrix.setIdentityM(rotationMatrix, 0);
    }

    @Override
    public void onRenderFrame() {
        GLES30.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        glProgram.useProgram();

        long time = SystemClock.uptimeMillis();
        float diff = (time - lastRenderTime) / 1000f;
        lastRenderTime = time;

        Matrix.rotateM(rotationMatrix, 0, diff * 45f, 1f, 1.5f, 0f);
        Matrix.scaleM(modelMatrix, 0, rotationMatrix, 0, 10f, 10f, 10f);
        Matrix.multiplyMM(modelViewMatrix, 0, glCamera.viewMat(), 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, glCamera.projMat(), 0, modelViewMatrix, 0);

        GLES30.glUniformMatrix4fv(glProgram.getUniformLocation("uModelViewMatrix"), 1, false, modelViewMatrix, 0);
        GLES30.glUniformMatrix4fv(glProgram.getUniformLocation("uModelViewProjectionMatrix"), 1, false, modelViewProjectionMatrix, 0);
        GLES30.glUniform3fv(glProgram.getUniformLocation("uCameraPosition"), 1, glCamera.pos(), 0);

        final float[] material = enableLightning ? MATERIAL_LIGHTNING_ENABLED : MATERIAL_LIGHTNING_DISABLED;
        GLES30.glUniform4fv(glProgram.getUniformLocation("uMaterial"), 1, material, 0);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureCubemap.bind(GLES30.GL_TEXTURE_CUBE_MAP);
        glSamplerLinear.bind(0);
        renderCubeFilled();
    }

    @Override
    public void onSurfaceReleased() {
    }

    public void onEvent(SettingsEvent event) {
        enableLightning = event.getEnableLightning();
    }

    public static class SettingsFragment extends Fragment {

        private final CheckBox.OnCheckedChangeListener onCheckedListener =
                new CheckBox.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                        SharedPreferences.Editor editor =
                                getActivity().getPreferences(Context.MODE_PRIVATE).edit();
                        editor.putBoolean(PREFERENCE_ENABLE_LIGHTNING, checked);
                        editor.commit();

                        EventBus.getDefault().post(new SettingsEvent(checked));
                    }
                };

        private CheckBox checkBoxEnableLightning;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_settings_basic_cubemap, container, false);

            checkBoxEnableLightning = (CheckBox) view.findViewById(R.id.checkbox_enable_lightning);
            checkBoxEnableLightning.setChecked(
                    getActivity()
                            .getPreferences(Context.MODE_PRIVATE)
                            .getBoolean(PREFERENCE_ENABLE_LIGHTNING, DEFAULT_ENABLE_LIGHTNING)
            );
            checkBoxEnableLightning.setOnCheckedChangeListener(onCheckedListener);

            return view;
        }
    }

    public static class SettingsEvent {

        private boolean enableLightning;

        public SettingsEvent(boolean enableLightning) {
            this.enableLightning = enableLightning;
        }

        public boolean getEnableLightning() {
            return enableLightning;
        }

    }
}
