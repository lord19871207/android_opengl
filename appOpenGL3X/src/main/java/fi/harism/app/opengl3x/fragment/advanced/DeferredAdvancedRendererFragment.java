package fi.harism.app.opengl3x.fragment.advanced;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import de.greenrobot.event.EventBus;
import fi.harism.app.opengl3x.R;
import fi.harism.lib.opengl.gl.GlFramebuffer;
import fi.harism.lib.opengl.gl.GlProgram;
import fi.harism.lib.opengl.gl.GlSampler;
import fi.harism.lib.opengl.gl.GlTexture;
import fi.harism.lib.opengl.gl.GlUtils;

public class DeferredAdvancedRendererFragment extends AdvancedRendererFragment {

    private static final String RENDERER_ID = "renderer.advanced.deferred";
    private static final String PREFERENCE_LIGHT_SPECULAR = RENDERER_ID + ".light_specular";
    private static final String PREFERENCE_LIGHT_DIFFUSE = RENDERER_ID + ".light_diffuse";
    private static final String PREFERENCE_MATERIAL_ROUGHNESS = RENDERER_ID + ".material_roughness";
    private static final int DEFAULT_LIGHT_SPECULAR = Color.argb(255, 192, 192, 192);
    private static final int DEFAULT_LIGHT_DIFFUSE = Color.argb(255, 128, 128, 128);
    private static final int DEFAULT_MATERIAL_ROUGHNESS = 80;

    private int specularColor;
    private int diffuseColor;
    private float roughness;

    private GlSampler glSamplerNearest;
    private GlTexture glTextureGnormal;
    private GlTexture glTextureGdepth;
    private GlTexture glTextureGlight;
    private GlFramebuffer glFramebufferGnormal;
    private GlFramebuffer glFramebufferGlight;
    private GlProgram glProgramNormal;
    private GlProgram glProgramLight;
    private GlProgram glProgramOutput;

    private Size surfaceSize;

    private final float projMatInv[] = new float[16];

    private final UniformsNormal uniformsNormal = new UniformsNormal();
    private final UniformsLight uniformsLight = new UniformsLight();
    private final UniformsOutput uniformsOutput = new UniformsOutput();

    private final class UniformsNormal {
        public int uModelViewMat;
        public int uModelViewProjMat;
        public int uRoughness;
    }

    private final class UniformsLight {
        public int sNormal;
        public int sDepth;
        public int uDiffuse;
        public int uSpecular;
        public int uProjMatInv;
        public int uSurfaceSizeInv;
    }

    private final class UniformsOutput {
        public int uModelViewMat;
        public int uModelViewProjMat;
        public int sLight;
        public int uSurfaceSizeInv;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs =
                getActivity().getPreferences(Context.MODE_PRIVATE);
        diffuseColor = prefs.getInt(PREFERENCE_LIGHT_DIFFUSE, DEFAULT_LIGHT_DIFFUSE);
        specularColor = prefs.getInt(PREFERENCE_LIGHT_SPECULAR, DEFAULT_LIGHT_SPECULAR);
        roughness = (prefs.getInt(PREFERENCE_MATERIAL_ROUGHNESS, DEFAULT_MATERIAL_ROUGHNESS) + 1) / 256f;
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
        return R.string.renderer_advanced_deferred_title;
    }

    @Override
    public int getCaptionStringId() {
        return R.string.renderer_advanced_deferred_caption;
    }

    @Override
    public void onSurfaceCreated() {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glCullFace(GLES30.GL_BACK);
        GLES30.glFrontFace(GLES30.GL_CCW);

        glSamplerNearest = new GlSampler()
                .parameter(GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
                .parameter(GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
                .parameter(GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
                .parameter(GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        glTextureGnormal = new GlTexture();
        glTextureGlight = new GlTexture();
        glTextureGdepth = new GlTexture();
        glFramebufferGnormal = new GlFramebuffer();
        glFramebufferGlight = new GlFramebuffer();

        prepareScene();

        try {
            glProgramNormal = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/advanced/deferred/normal_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/advanced/deferred/normal_fs.txt"),
                    null).useProgram().getUniformIndices(uniformsNormal);

            glProgramLight = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/advanced/deferred/light_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/advanced/deferred/light_fs.txt"),
                    null).useProgram().getUniformIndices(uniformsLight);
            GLES30.glUniform1i(uniformsLight.sNormal, 0);
            GLES30.glUniform1i(uniformsLight.sDepth, 1);

            glProgramOutput = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/advanced/deferred/output_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/advanced/deferred/output_fs.txt"),
                    null).useProgram().getUniformIndices(uniformsOutput);
            GLES30.glUniform1i(uniformsOutput.sLight, 0);

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
        surfaceSize = new Size(width, height);
        prepareCamera(width, height);

        Matrix.invertM(projMatInv, 0, getCamera().projMat(), 0);

        glTextureGnormal
                .bind(GLES30.GL_TEXTURE_2D)
                .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, width, height, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null)
                .unbind(GLES30.GL_TEXTURE_2D);
        glTextureGlight
                .bind(GLES30.GL_TEXTURE_2D)
                .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, width, height, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null)
                .unbind(GLES30.GL_TEXTURE_2D);
        glTextureGdepth
                .bind(GLES30.GL_TEXTURE_2D)
                .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_DEPTH_COMPONENT32F, width, height, 0, GLES30.GL_DEPTH_COMPONENT, GLES30.GL_FLOAT, null)
                .unbind(GLES30.GL_TEXTURE_2D);

        glFramebufferGnormal
                .bind(GLES30.GL_DRAW_FRAMEBUFFER)
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, glTextureGnormal.name(), 0)
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_TEXTURE_2D, glTextureGdepth.name(), 0)
                .drawBuffers(GLES30.GL_COLOR_ATTACHMENT0)
                .unbind(GLES30.GL_DRAW_FRAMEBUFFER);
        glFramebufferGlight
                .bind(GLES30.GL_DRAW_FRAMEBUFFER)
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, glTextureGlight.name(), 0)
                .drawBuffers(GLES30.GL_COLOR_ATTACHMENT0)
                .unbind(GLES30.GL_DRAW_FRAMEBUFFER);
    }

    @Override
    public void onRenderFrame() {
        glFramebufferGnormal.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        GLES30.glClearColor(.0f, .0f, .0f, .0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        glProgramNormal.useProgram();
        GLES30.glUniform1f(uniformsNormal.uRoughness, roughness);
        renderScene(uniformsNormal.uModelViewMat, uniformsNormal.uModelViewProjMat);
        //glFramebufferGnormal.unbind(GLES30.GL_DRAW_FRAMEBUFFER);

        glFramebufferGlight.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        glProgramLight.useProgram();
        GLES30.glUniform3f(uniformsLight.uDiffuse, Color.red(diffuseColor) / 255f,
                Color.green(diffuseColor) / 255f, Color.blue(diffuseColor) / 255f);
        GLES30.glUniform3f(uniformsLight.uSpecular, Color.red(specularColor) / 255f,
                Color.green(specularColor) / 255f, Color.blue(specularColor) / 255f);
        GLES30.glUniformMatrix4fv(uniformsLight.uProjMatInv, 1, false, projMatInv, 0);
        GLES30.glUniform2f(uniformsLight.uSurfaceSizeInv,
                1f / surfaceSize.getWidth(), 1f / surfaceSize.getHeight());
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureGnormal.bind(GLES30.GL_TEXTURE_2D);
        glSamplerNearest.bind(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        glTextureGdepth.bind(GLES30.GL_TEXTURE_2D);
        glSamplerNearest.bind(1);
        renderQuad();
        glFramebufferGlight.unbind(GLES30.GL_DRAW_FRAMEBUFFER);

        GLES30.glClearColor(0.72f, 0.70f, 0.60f, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        glProgramOutput.useProgram();
        GLES30.glUniform2f(uniformsOutput.uSurfaceSizeInv,
                1f / surfaceSize.getWidth(), 1f / surfaceSize.getHeight());
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureGlight.bind(GLES30.GL_TEXTURE_2D);
        glSamplerNearest.bind(0);
        renderScene(uniformsOutput.uModelViewMat, uniformsOutput.uModelViewProjMat);
        glTextureGlight.unbind(GLES30.GL_TEXTURE_2D);
    }

    @Override
    public void onSurfaceReleased() {
    }

    public void onEvent(SettingsEvent event) {
        diffuseColor = event.diffuseColor;
        specularColor = event.specularColor;
        roughness = (event.roughness + 1) / 256f;
    }

    public static class SettingsFragment extends Fragment {

        private final SeekBar.OnSeekBarChangeListener
                seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                SettingsEvent event = new SettingsEvent();
                event.diffuseColor = Color.rgb(
                        seekBarDiffuseR.getProgress(),
                        seekBarDiffuseG.getProgress(),
                        seekBarDiffuseB.getProgress());
                event.specularColor = Color.rgb(
                        seekBarSpecularR.getProgress(),
                        seekBarSpecularG.getProgress(),
                        seekBarSpecularB.getProgress());
                event.roughness = seekBarRoughness.getProgress();
                EventBus.getDefault().post(event);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int diffuseColor = Color.rgb(
                        seekBarDiffuseR.getProgress(),
                        seekBarDiffuseG.getProgress(),
                        seekBarDiffuseB.getProgress());
                int specularColor = Color.rgb(
                        seekBarSpecularR.getProgress(),
                        seekBarSpecularG.getProgress(),
                        seekBarSpecularB.getProgress());
                int roughness = seekBarRoughness.getProgress();
                SharedPreferences.Editor editor =
                        getActivity().getPreferences(Context.MODE_PRIVATE).edit();
                editor.putInt(PREFERENCE_LIGHT_DIFFUSE, diffuseColor);
                editor.putInt(PREFERENCE_LIGHT_SPECULAR, specularColor);
                editor.putInt(PREFERENCE_MATERIAL_ROUGHNESS, roughness);
                editor.apply();
            }
        };

        private SeekBar seekBarDiffuseR;
        private SeekBar seekBarDiffuseG;
        private SeekBar seekBarDiffuseB;

        private SeekBar seekBarSpecularR;
        private SeekBar seekBarSpecularG;
        private SeekBar seekBarSpecularB;

        private SeekBar seekBarRoughness;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_settings_advanced_deferred, container, false);

            seekBarDiffuseR = (SeekBar) view.findViewById(R.id.seekbar_diffuse_r);
            seekBarDiffuseG = (SeekBar) view.findViewById(R.id.seekbar_diffuse_g);
            seekBarDiffuseB = (SeekBar) view.findViewById(R.id.seekbar_diffuse_b);
            seekBarSpecularR = (SeekBar) view.findViewById(R.id.seekbar_specular_r);
            seekBarSpecularG = (SeekBar) view.findViewById(R.id.seekbar_specular_g);
            seekBarSpecularB = (SeekBar) view.findViewById(R.id.seekbar_specular_b);
            seekBarRoughness = (SeekBar) view.findViewById(R.id.seekbar_roughness);

            SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
            int diffuseColor = prefs.getInt(PREFERENCE_LIGHT_DIFFUSE, DEFAULT_LIGHT_DIFFUSE);
            int specularColor = prefs.getInt(PREFERENCE_LIGHT_SPECULAR, DEFAULT_LIGHT_SPECULAR);
            int roughness = prefs.getInt(PREFERENCE_MATERIAL_ROUGHNESS, DEFAULT_MATERIAL_ROUGHNESS);

            seekBarDiffuseR.setProgress(Color.red(diffuseColor));
            seekBarDiffuseG.setProgress(Color.green(diffuseColor));
            seekBarDiffuseB.setProgress(Color.blue(diffuseColor));
            seekBarSpecularR.setProgress(Color.red(specularColor));
            seekBarSpecularG.setProgress(Color.green(specularColor));
            seekBarSpecularB.setProgress(Color.blue(specularColor));
            seekBarRoughness.setProgress(roughness);

            seekBarDiffuseR.setOnSeekBarChangeListener(seekBarChangeListener);
            seekBarDiffuseG.setOnSeekBarChangeListener(seekBarChangeListener);
            seekBarDiffuseB.setOnSeekBarChangeListener(seekBarChangeListener);
            seekBarSpecularR.setOnSeekBarChangeListener(seekBarChangeListener);
            seekBarSpecularG.setOnSeekBarChangeListener(seekBarChangeListener);
            seekBarSpecularB.setOnSeekBarChangeListener(seekBarChangeListener);
            seekBarRoughness.setOnSeekBarChangeListener(seekBarChangeListener);

            return view;
        }
    }

    public static class SettingsEvent {
        private int diffuseColor;
        private int specularColor;
        private int roughness;
    }
}
