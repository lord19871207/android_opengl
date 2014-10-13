package fi.harism.app.opengl3x.fragment.advanced;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLES30;
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

public class DofAdvancedRendererFragment extends AdvancedRendererFragment {

    private static final String RENDERER_ID = "renderer.advanced.dof";
    private static final String PREFERENCE_APERTURE_DIAMETER = RENDERER_ID + ".aperture_diameter";
    private static final String PREFERENCE_PLANE_IN_FOCUS = RENDERER_ID + ".plane_in_focus";
    private static final int DEFAULT_APERTURE_DIAMETER = 8;
    private static final int DEFAULT_PLANE_IN_FOCUS = 7;

    private float apertureDiameter = 0.5f * 32;
    private float planeInFocus = 5.0f;

    private GlTexture glTextureScene;
    private GlTexture glTextureDepth;
    private GlTexture glTextureDofCoc;
    private GlTexture glTextureDofVert;
    private GlTexture glTextureDofHorz;
    private GlFramebuffer glFramebufferScene;
    private GlFramebuffer glFramebufferDofCoc;
    private GlFramebuffer glFramebufferDofHorz;
    private GlFramebuffer glFramebufferDofVert;
    private GlSampler glSamplerNearest;
    private GlSampler glSamplerLinear;
    private GlProgram glProgramScene;
    private GlProgram glProgramDofCoc;
    private GlProgram glProgramDofBlur;
    private GlProgram glProgramDofOut;

    private Size surfaceSize;

    private final UniformsScene uniformsScene = new UniformsScene();
    private final UniformsDofCoc uniformsDofCoc = new UniformsDofCoc();
    private final UniformsDofBlur uniformsDofBlur = new UniformsDofBlur();
    private final UniformsDofOut uniformsDofOut = new UniformsDofOut();

    private final class UniformsScene {
        public int uModelViewMat;
        public int uModelViewProjMat;
    }

    private final class UniformsDofCoc {
        public int sTexture;
        public int uParams;
    }

    private final class UniformsDofBlur {
        public int sTexture;
        public int sTextureCoc;
        public int uSampleOffset;
        public int uSampleCount;
    }

    private final class UniformsDofOut {
        public int sTexture;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs =
                getActivity().getPreferences(Context.MODE_PRIVATE);
        apertureDiameter = prefs.getInt(PREFERENCE_APERTURE_DIAMETER, DEFAULT_APERTURE_DIAMETER) * 0.5f;
        planeInFocus = prefs.getInt(PREFERENCE_PLANE_IN_FOCUS, DEFAULT_PLANE_IN_FOCUS);
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
        return R.string.renderer_advanced_dof_title;
    }

    @Override
    public int getCaptionStringId() {
        return R.string.renderer_advanced_dof_caption;
    }

    @Override
    public void onSurfaceCreated() {
        prepareScene();

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glCullFace(GLES30.GL_BACK);
        GLES30.glFrontFace(GLES30.GL_CCW);

        glTextureScene = new GlTexture();
        glTextureDepth = new GlTexture();
        glTextureDofCoc = new GlTexture();
        glTextureDofVert = new GlTexture();
        glTextureDofHorz = new GlTexture();

        glFramebufferScene = new GlFramebuffer();
        glFramebufferDofCoc = new GlFramebuffer();
        glFramebufferDofHorz = new GlFramebuffer();
        glFramebufferDofVert = new GlFramebuffer();

        glSamplerNearest = new GlSampler()
                .parameter(GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
                .parameter(GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
                .parameter(GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
                .parameter(GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        glSamplerLinear = new GlSampler()
                .parameter(GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
                .parameter(GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
                .parameter(GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
                .parameter(GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        try {
            glProgramScene = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/advanced/dof/scene_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/advanced/dof/scene_fs.txt"),
                    null).useProgram().getUniformIndices(uniformsScene);

            glProgramDofCoc = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/advanced/dof/dof_coc_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/advanced/dof/dof_coc_fs.txt"),
                    null).useProgram().getUniformIndices(uniformsDofCoc);
            GLES30.glUniform1i(uniformsDofCoc.sTexture, 0);

            glProgramDofBlur = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/advanced/dof/dof_blur_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/advanced/dof/dof_blur_fs.txt"),
                    null).useProgram().getUniformIndices(uniformsDofBlur);
            GLES30.glUniform1i(uniformsDofBlur.sTexture, 0);
            GLES30.glUniform1i(uniformsDofBlur.sTextureCoc, 1);

            glProgramDofOut = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/advanced/dof/dof_out_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/advanced/dof/dof_out_fs.txt"),
                    null).useProgram().getUniformIndices(uniformsDofOut);
            GLES30.glUniform1i(uniformsDofOut.sTexture, 0);

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
        prepareCamera(width, height);
        surfaceSize = new Size(width, height);

        glTextureScene.bind(GLES30.GL_TEXTURE_2D)
                .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, width, height, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null)
                .unbind(GLES30.GL_TEXTURE_2D);
        glTextureDepth.bind(GLES30.GL_TEXTURE_2D)
                .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_DEPTH_COMPONENT16, width, height, 0, GLES30.GL_DEPTH_COMPONENT, GLES30.GL_UNSIGNED_SHORT, null)
                .unbind(GLES30.GL_TEXTURE_2D);
        glTextureDofCoc.bind(GLES30.GL_TEXTURE_2D)
                .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RG8, width / 2, height / 2, 0, GLES30.GL_RG, GLES30.GL_UNSIGNED_BYTE, null)
                .unbind(GLES30.GL_TEXTURE_2D);
        glTextureDofHorz.bind(GLES30.GL_TEXTURE_2D)
                .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, width / 2, height / 2, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null)
                .unbind(GLES30.GL_TEXTURE_2D);
        glTextureDofVert.bind(GLES30.GL_TEXTURE_2D)
                .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, width / 2, height / 2, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null)
                .unbind(GLES30.GL_TEXTURE_2D);

        glFramebufferScene.bind(GLES30.GL_DRAW_FRAMEBUFFER)
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, glTextureScene.name(), 0)
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_TEXTURE_2D, glTextureDepth.name(), 0)
                .drawBuffers(GLES30.GL_COLOR_ATTACHMENT0).unbind(GLES30.GL_DRAW_FRAMEBUFFER);
        glFramebufferDofCoc.bind(GLES30.GL_DRAW_FRAMEBUFFER)
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, glTextureDofVert.name(), 0)
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT1, GLES30.GL_TEXTURE_2D, glTextureDofCoc.name(), 0)
                .drawBuffers(GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_COLOR_ATTACHMENT1).unbind(GLES30.GL_DRAW_FRAMEBUFFER);
        glFramebufferDofHorz.bind(GLES30.GL_DRAW_FRAMEBUFFER)
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, glTextureDofHorz.name(), 0)
                .drawBuffers(GLES30.GL_COLOR_ATTACHMENT0).unbind(GLES30.GL_DRAW_FRAMEBUFFER);
        glFramebufferDofVert.bind(GLES30.GL_DRAW_FRAMEBUFFER)
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, glTextureDofVert.name(), 0)
                .drawBuffers(GLES30.GL_COLOR_ATTACHMENT0).unbind(GLES30.GL_DRAW_FRAMEBUFFER);
    }

    @Override
    public void onRenderFrame() {
        glProgramScene.useProgram();
        glFramebufferScene.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        GLES30.glClearColor(0.4f, 0.4f, 0.4f, 0.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        renderScene(uniformsScene.uModelViewMat, uniformsScene.uModelViewProjMat);
        glFramebufferScene.unbind(GLES30.GL_DRAW_FRAMEBUFFER);

        // ( apertureDiameter * focalLength * (planeInFocus - x) ) /
        // ( x * (planeInFocus - focalLength) * sensorHeight )
        //
        // simplifies to
        //
        // (k4 / x) -  k3

        float k1 = apertureDiameter * getCamera().focalLength();
        float k2 = planeInFocus - getCamera().focalLength();
        float k3 = k1 / (k2 * getCamera().sensorHeight());
        float k4 = k3 * planeInFocus;

        glProgramDofCoc.useProgram();
        glFramebufferDofCoc.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        GLES30.glViewport(0, 0, surfaceSize.getWidth() / 2, surfaceSize.getHeight() / 2);
        GLES30.glUniform2f(uniformsDofCoc.uParams, k4, k3);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureScene.bind(GLES30.GL_TEXTURE_2D);
        glSamplerNearest.bind(0);
        renderQuad();

        float aspectRatio = (float) surfaceSize.getWidth() / surfaceSize.getHeight();
        renderDof(0.008f, 0.008f * aspectRatio, 2);
        renderDof(0.002f, 0.002f * aspectRatio, 2);

        glProgramDofOut.useProgram();
        glFramebufferDofVert.unbind(GLES30.GL_DRAW_FRAMEBUFFER);
        GLES30.glViewport(0, 0, surfaceSize.getWidth(), surfaceSize.getHeight());
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureDofVert.bind(GLES30.GL_TEXTURE_2D);
        glSamplerLinear.bind(0);
        renderQuad();
    }

    @Override
    public void onSurfaceReleased() {
    }

    private void renderDof(float sampleDx, float sampleDy, int sampleCount) {
        glProgramDofBlur.useProgram();
        glFramebufferDofHorz.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        GLES30.glUniform2f(uniformsDofBlur.uSampleOffset, sampleDx, 0f);
        GLES30.glUniform1i(uniformsDofBlur.uSampleCount, sampleCount);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureDofVert.bind(GLES30.GL_TEXTURE_2D);
        glSamplerNearest.bind(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        glTextureDofCoc.bind(GLES30.GL_TEXTURE_2D);
        glSamplerNearest.bind(1);
        renderQuad();
        //glFramebufferDofVert.unbind(GLES30.GL_DRAW_FRAMEBUFFER);

        //glProgramDofDiag.useProgram();
        glFramebufferDofVert.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        GLES30.glUniform2f(uniformsDofBlur.uSampleOffset, 0f, sampleDy);
        GLES30.glUniform1i(uniformsDofBlur.uSampleCount, sampleCount);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureDofHorz.bind(GLES30.GL_TEXTURE_2D);
        glSamplerNearest.bind(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        glTextureDofCoc.bind(GLES30.GL_TEXTURE_2D);
        glSamplerNearest.bind(1);
        renderQuad();
        //glFramebufferDofDiag.unbind(GLES30.GL_DRAW_FRAMEBUFFER);
    }

    public void onEvent(SettingsEvent event) {
        apertureDiameter = event.apertureDiameter * 0.5f;
        planeInFocus = event.planeInFocus;
    }

    public static class SettingsFragment extends Fragment {

        private final SeekBar.OnSeekBarChangeListener
                seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                SettingsEvent event = new SettingsEvent();
                event.apertureDiameter = seekBarApertureDiameter.getProgress();
                event.planeInFocus = seekBarPlaneInFocus.getProgress();
                EventBus.getDefault().post(event);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int apertureDiameter = seekBarApertureDiameter.getProgress();
                int planeInFocus = seekBarPlaneInFocus.getProgress();
                SharedPreferences.Editor editor =
                        getActivity().getPreferences(Context.MODE_PRIVATE).edit();
                editor.putInt(PREFERENCE_APERTURE_DIAMETER, apertureDiameter);
                editor.putInt(PREFERENCE_PLANE_IN_FOCUS, planeInFocus);
                editor.apply();
            }
        };

        private SeekBar seekBarApertureDiameter;
        private SeekBar seekBarPlaneInFocus;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_settings_advanced_dof, container, false);

            seekBarApertureDiameter = (SeekBar) view.findViewById(R.id.seekbar_aperture_diameter);
            seekBarPlaneInFocus = (SeekBar) view.findViewById(R.id.seekbar_plane_in_focus);

            SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
            int apertureDiameter = prefs.getInt(PREFERENCE_APERTURE_DIAMETER, DEFAULT_APERTURE_DIAMETER);
            int planeInFocus = prefs.getInt(PREFERENCE_PLANE_IN_FOCUS, DEFAULT_PLANE_IN_FOCUS);

            seekBarApertureDiameter.setProgress(apertureDiameter);
            seekBarPlaneInFocus.setProgress(planeInFocus);

            seekBarApertureDiameter.setOnSeekBarChangeListener(seekBarChangeListener);
            seekBarPlaneInFocus.setOnSeekBarChangeListener(seekBarChangeListener);

            return view;
        }
    }

    public static class SettingsEvent {
        private int apertureDiameter;
        private int planeInFocus;
    }

}
