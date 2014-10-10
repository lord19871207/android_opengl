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

    private final static String UNIFORM_NAMES_DEPTH[] = {"uModelViewProjectionMatrix"};
    private final static String UNIFORM_NAMES_MAIN[] = {"uModelViewMatrix",
            "uModelViewProjectionMatrix", "uShadowMatrix", "uLightPosition", "uMaterial",
            "uSampleOffset"};

    private final int uniformsDepth[] = new int[UNIFORM_NAMES_DEPTH.length];
    private final int uniformsMain[] = new int[UNIFORM_NAMES_MAIN.length];

    private GlCamera glCamera;
    private GlLight glLight;
    private GlTexture glTextureDepth;
    private GlFramebuffer glFramebufferDepth;
    private GlSampler glSamplerDepth;
    private GlProgram glProgramMain;
    private GlProgram glProgramDepth;

    private long lastRenderTime;
    private Size surfaceSize;

    private final float rotationMatrix[] = new float[16];
    private final float modelMatrix[] = new float[16];
    private final float modelViewMatrix[] = new float[16];
    private final float modelViewProjectionMatrix[] = new float[16];

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
                    null).useProgram();
            GLES30.glUniform1i(glProgramMain.getUniformLocation("sDepth"), 0);
            glProgramMain.getUniformIndices(UNIFORM_NAMES_MAIN, uniformsMain);

            glProgramDepth = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/basic/shadow/depth_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/basic/shadow/depth_fs.txt"),
                    null);
            glProgramDepth.getUniformIndices(UNIFORM_NAMES_DEPTH, uniformsDepth);
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
        Matrix.setIdentityM(rotationMatrix, 0);
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, 0f, -6f, 0f);
        Matrix.scaleM(modelMatrix, 0, 4f, 4f, 4f);
    }

    @Override
    public void onRenderFrame() {
        long time = SystemClock.uptimeMillis();
        float diff = (time - lastRenderTime) / 1000f;
        lastRenderTime = time;

        Matrix.rotateM(rotationMatrix, 0, diff * 45f, 1f, 1.5f, 0f);

        glFramebufferDepth.bind(GLES30.GL_DRAW_FRAMEBUFFER);

        GLES30.glCullFace(GLES30.GL_FRONT);
        GLES30.glViewport(0, 0, SHADOWMAP_SIZE, SHADOWMAP_SIZE);
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT);

        glProgramDepth.useProgram();

        Matrix.multiplyMM(modelViewProjectionMatrix, 0, glLight.viewProjMat(), 0, rotationMatrix, 0);
        GLES30.glUniformMatrix4fv(uniformsDepth[0], 1, false, modelViewProjectionMatrix, 0);
        renderCubeFilled();

        Matrix.multiplyMM(modelViewProjectionMatrix, 0, glLight.viewProjMat(), 0, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(uniformsDepth[0], 1, false, modelViewProjectionMatrix, 0);
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

        Matrix.multiplyMM(modelViewMatrix, 0, glCamera.viewMat(), 0, rotationMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, glCamera.projMat(), 0, modelViewMatrix, 0);

        GLES30.glUniformMatrix4fv(uniformsMain[0], 1, false, modelViewMatrix, 0);
        GLES30.glUniformMatrix4fv(uniformsMain[1], 1, false, modelViewProjectionMatrix, 0);
        GLES30.glUniformMatrix4fv(uniformsMain[2], 1, false, glLight.shadowMat(glCamera.viewMat()), 0);
        GLES30.glUniform3fv(uniformsMain[3], 1, LIGHT_POSITION, 0);
        GLES30.glUniform4fv(uniformsMain[4], 1, MATERIAL_CUBE, 0);
        GLES30.glUniform2f(uniformsMain[5], 1f / SHADOWMAP_SIZE, 1f / SHADOWMAP_SIZE);
        renderCubeFilled();

        Matrix.multiplyMM(modelViewMatrix, 0, glCamera.viewMat(), 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, glCamera.projMat(), 0, modelViewMatrix, 0);

        GLES30.glUniformMatrix4fv(uniformsMain[0], 1, false, modelViewMatrix, 0);
        GLES30.glUniformMatrix4fv(uniformsMain[1], 1, false, modelViewProjectionMatrix, 0);
        GLES30.glUniformMatrix4fv(uniformsMain[2], 1, false, glLight.shadowMat(glCamera.viewMat()), 0);
        GLES30.glUniform3fv(uniformsMain[3], 1, LIGHT_POSITION, 0);
        GLES30.glUniform4fv(uniformsMain[4], 1, MATERIAL_CUBE_ENV, 0);
        GLES30.glUniform2f(uniformsMain[5], 1f / SHADOWMAP_SIZE, 1f / SHADOWMAP_SIZE);
        renderCubeFilled();
    }

    @Override
    public void onSurfaceReleased() {
    }

}
