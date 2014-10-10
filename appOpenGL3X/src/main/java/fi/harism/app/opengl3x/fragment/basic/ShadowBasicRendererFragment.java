package fi.harism.app.opengl3x.fragment.basic;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Size;
import android.widget.Toast;

import fi.harism.app.opengl3x.R;
import fi.harism.lib.opengl.egl.EglCore;
import fi.harism.lib.opengl.gl.GlFramebuffer;
import fi.harism.lib.opengl.gl.GlProgram;
import fi.harism.lib.opengl.gl.GlSampler;
import fi.harism.lib.opengl.gl.GlTexture;
import fi.harism.lib.opengl.gl.GlUtils;
import fi.harism.lib.opengl.model.GlCamera;
import fi.harism.lib.opengl.model.GlLight;

public class ShadowBasicRendererFragment extends BasicRendererFragment {

    private static final String RENDERER_ID = "renderer.basic.shadow";

    private static final float CAMERA_POSITION[] = {0f, 0f, 5f};
    private static final float LIGHT_POSITION[] = {4f, 8f, 2f};
    private static final float LIGHT_DIRECTION[] = {0f, 0f, 0f};

    private static final float MATERIAL_CUBE[] = {0.3f, 0.8f, 0.4f, 8.0f};
    private static final float MATERIAL_CUBE_ENV[] = {0.3f, 0.3f, 0.3f, 8.0f};

    private static final int SHADOWMAP_SIZE = 1024;

    private GlCamera glCamera;
    private GlLight glLight;
    private GlTexture glTextureDepth;
    private GlFramebuffer glFramebufferDepth;
    private GlSampler glSamplerDepth;
    private GlProgram glProgramMain;
    private GlProgram glProgramDepth;

    private long lastRenderTime;
    private Size surfaceSize;

    private final float rotationMat[] = new float[16];
    private final float modelMat[] = new float[16];
    private final float modelViewMat[] = new float[16];
    private final float modelViewProjMat[] = new float[16];

    private final UniformsDepth uniformsDepth = new UniformsDepth();
    private final UniformsMain uniformsMain = new UniformsMain();

    private final class UniformsDepth {
        public int uModelViewProjMat;
    }

    private final class UniformsMain {
        public int sDepth;
        public int uModelViewMat;
        public int uModelViewProjMat;
        public int uShadowMat;
        public int uLightPos;
        public int uMaterial;
        public int uSampleOffset;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEglFlags(EglCore.FLAG_DEPTH_BUFFER);
    }

    @Override
    public String getRendererId() {
        return RENDERER_ID;
    }

    @Override
    public int getTitleStringId() {
        return R.string.renderer_basic_shadow_title;
    }

    @Override
    public int getCaptionStringId() {
        return R.string.renderer_basic_shadow_caption;
    }

    @Override
    public void onSurfaceCreated() {
        super.onSurfaceCreated();

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glFrontFace(GLES30.GL_CCW);

        glCamera = new GlCamera();

        glLight = new GlLight()
                .setPerspective(SHADOWMAP_SIZE, SHADOWMAP_SIZE, 60f, 1f, 100f)
                .setPos(LIGHT_POSITION)
                .setDir(LIGHT_DIRECTION);

        glTextureDepth = new GlTexture()
                .bind(GLES30.GL_TEXTURE_2D)
                .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_DEPTH_COMPONENT32F, SHADOWMAP_SIZE, SHADOWMAP_SIZE, 0, GLES30.GL_DEPTH_COMPONENT, GLES30.GL_FLOAT, null)
                .unbind(GLES30.GL_TEXTURE_2D);
        glFramebufferDepth = new GlFramebuffer()
                .bind(GLES30.GL_DRAW_FRAMEBUFFER)
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_TEXTURE_2D, glTextureDepth.name(), 0)
                .drawBuffers(GLES30.GL_COLOR_ATTACHMENT0)
                .drawBuffers(GLES30.GL_NONE)
                .readBuffer(GLES30.GL_NONE)
                .unbind(GLES30.GL_DRAW_FRAMEBUFFER);

        glSamplerDepth = new GlSampler()
                .parameter(GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
                .parameter(GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
                .parameter(GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
                .parameter(GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
                .parameter(GLES30.GL_TEXTURE_COMPARE_MODE, GLES30.GL_COMPARE_REF_TO_TEXTURE)
                .parameter(GLES30.GL_TEXTURE_COMPARE_FUNC, GLES30.GL_LESS);

        try {
            glProgramMain = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/basic/shadow/main_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/basic/shadow/main_fs.txt"),
                    null).useProgram().getUniformIndices(uniformsMain);
            GLES30.glUniform1i(uniformsMain.sDepth, 0);
            glProgramDepth = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/basic/shadow/depth_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/basic/shadow/depth_fs.txt"),
                    null).useProgram().getUniformIndices(uniformsDepth);
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
        glCamera.setPerspective(width, height, 60f, 1f, 100f);
        glCamera.setPos(CAMERA_POSITION);
        Matrix.setIdentityM(rotationMat, 0);
        Matrix.setIdentityM(modelMat, 0);
        Matrix.translateM(modelMat, 0, 0f, -6f, 0f);
        Matrix.scaleM(modelMat, 0, 4f, 4f, 4f);
    }

    @Override
    public void onRenderFrame() {
        long time = SystemClock.uptimeMillis();
        float diff = (time - lastRenderTime) / 1000f;
        lastRenderTime = time;

        Matrix.rotateM(rotationMat, 0, diff * 45f, 1f, 1.5f, 0f);

        glFramebufferDepth.bind(GLES30.GL_DRAW_FRAMEBUFFER);

        GLES30.glCullFace(GLES30.GL_FRONT);
        GLES30.glViewport(0, 0, SHADOWMAP_SIZE, SHADOWMAP_SIZE);
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT);

        glProgramDepth.useProgram();

        Matrix.multiplyMM(modelViewProjMat, 0, glLight.viewProjMat(), 0, rotationMat, 0);
        GLES30.glUniformMatrix4fv(uniformsDepth.uModelViewProjMat, 1, false, modelViewProjMat, 0);
        renderCubeFilled();

        Matrix.multiplyMM(modelViewProjMat, 0, glLight.viewProjMat(), 0, modelMat, 0);
        GLES30.glUniformMatrix4fv(uniformsDepth.uModelViewProjMat, 1, false, modelViewProjMat, 0);
        renderCubeFilled();

        glFramebufferDepth.unbind(GLES30.GL_DRAW_FRAMEBUFFER);

        GLES30.glCullFace(GLES30.GL_BACK);
        GLES30.glViewport(0, 0, surfaceSize.getWidth(), surfaceSize.getHeight());
        GLES30.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        glProgramMain.useProgram();

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureDepth.bind(GLES30.GL_TEXTURE_2D);
        glSamplerDepth.bind(0);

        Matrix.multiplyMM(modelViewMat, 0, glCamera.viewMat(), 0, rotationMat, 0);
        Matrix.multiplyMM(modelViewProjMat, 0, glCamera.projMat(), 0, modelViewMat, 0);

        GLES30.glUniformMatrix4fv(uniformsMain.uModelViewMat, 1, false, modelViewMat, 0);
        GLES30.glUniformMatrix4fv(uniformsMain.uModelViewProjMat, 1, false, modelViewProjMat, 0);
        GLES30.glUniformMatrix4fv(uniformsMain.uShadowMat, 1, false, glLight.shadowMat(glCamera.viewMat()), 0);
        GLES30.glUniform3fv(uniformsMain.uLightPos, 1, LIGHT_POSITION, 0);
        GLES30.glUniform4fv(uniformsMain.uMaterial, 1, MATERIAL_CUBE, 0);
        GLES30.glUniform2f(uniformsMain.uSampleOffset, 1f / SHADOWMAP_SIZE, 1f / SHADOWMAP_SIZE);
        renderCubeFilled();

        Matrix.multiplyMM(modelViewMat, 0, glCamera.viewMat(), 0, modelMat, 0);
        Matrix.multiplyMM(modelViewProjMat, 0, glCamera.projMat(), 0, modelViewMat, 0);

        GLES30.glUniformMatrix4fv(uniformsMain.uModelViewMat, 1, false, modelViewMat, 0);
        GLES30.glUniformMatrix4fv(uniformsMain.uModelViewProjMat, 1, false, modelViewProjMat, 0);
        GLES30.glUniformMatrix4fv(uniformsMain.uShadowMat, 1, false, glLight.shadowMat(glCamera.viewMat()), 0);
        GLES30.glUniform3fv(uniformsMain.uLightPos, 1, LIGHT_POSITION, 0);
        GLES30.glUniform4fv(uniformsMain.uMaterial, 1, MATERIAL_CUBE_ENV, 0);
        GLES30.glUniform2f(uniformsMain.uSampleOffset, 1f / SHADOWMAP_SIZE, 1f / SHADOWMAP_SIZE);
        renderCubeFilled();
    }

    @Override
    public void onSurfaceReleased() {
    }

}
