package fi.harism.app.opengl3x.fragment.basic;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Size;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import fi.harism.app.opengl3x.R;
import fi.harism.lib.opengl.egl.EglCore;
import fi.harism.lib.opengl.gl.GlFramebuffer;
import fi.harism.lib.opengl.gl.GlProgram;
import fi.harism.lib.opengl.gl.GlRenderbuffer;
import fi.harism.lib.opengl.gl.GlSampler;
import fi.harism.lib.opengl.gl.GlTexture;
import fi.harism.lib.opengl.gl.GlUtils;
import fi.harism.lib.opengl.model.GlCamera;
import fi.harism.lib.opengl.model.GlLight;

public class ShadowBasicRendererFragment extends BasicRendererFragment {

    private static final String RENDERER_ID = "renderer.basic.shadow";

    private static final float CAMERA_POSITION[] = {0f, 0f, 5f};
    private static final float LIGHT_POSITION[] = {0f, 4f, 2f};
    private static final float LIGHT_DIRECTION[] = {0f, 0f, 0f};

    private static final float MATERIAL_CUBE[] = {0.3f, 0.8f, 0.4f, 8.0f};
    private static final float MATERIAL_CUBE_ENV[] = {0.3f, 0.3f, 0.3f, 8.0f};

    private static final int SHADOWMAP_SIZE = 1024;

    private GlCamera glCamera;
    private GlLight glLight;
    private GlTexture glTextureShadow;
    private GlRenderbuffer glRenderbufferShadow;
    private GlFramebuffer glFramebufferShadow;
    private GlSampler glSamplerShadow;
    private GlProgram glProgram;
    private GlProgram glProgramShadow;

    private ByteBuffer verticesQuad;

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
        GLES30.glCullFace(GLES30.GL_BACK);
        GLES30.glFrontFace(GLES30.GL_CCW);

        glCamera = new GlCamera();

        glLight = new GlLight()
                .setPerspective(SHADOWMAP_SIZE, SHADOWMAP_SIZE, 60f, 1f, 100f)
                .setPosition(LIGHT_POSITION)
                .setDirection(LIGHT_DIRECTION);

        glTextureShadow = new GlTexture()
                .bind(GLES30.GL_TEXTURE_2D)
                .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_R32F, SHADOWMAP_SIZE, SHADOWMAP_SIZE, 0, GLES30.GL_RED, GLES30.GL_FLOAT, null)
                .unbind(GLES30.GL_TEXTURE_2D);
        glRenderbufferShadow = new GlRenderbuffer()
                .bind()
                .storage(GLES30.GL_DEPTH_COMPONENT32F, SHADOWMAP_SIZE, SHADOWMAP_SIZE)
                .unbind();
        glFramebufferShadow = new GlFramebuffer()
                .bind(GLES30.GL_DRAW_FRAMEBUFFER)
                .renderbuffer(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, glRenderbufferShadow.name())
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, glTextureShadow.name(), 0)
                .drawBuffers(GLES30.GL_COLOR_ATTACHMENT0)
                .unbind(GLES30.GL_DRAW_FRAMEBUFFER);

        glSamplerShadow = new GlSampler();
        glSamplerShadow.parameter(GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        glSamplerShadow.parameter(GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        glSamplerShadow.parameter(GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        glSamplerShadow.parameter(GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        final byte[] VERTICES = {-1, 1, -1, -1, 1, 1, 1, -1};
        verticesQuad = ByteBuffer.allocateDirect(VERTICES.length).order(ByteOrder.nativeOrder());
        verticesQuad.put(VERTICES).position(0);

        try {
            glProgram = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/basic/shadow/shader_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/basic/shadow/shader_fs.txt"),
                    null).useProgram();
            GLES30.glUniform1i(glProgram.getUniformLocation("sTextureShadow"), 0);

            glProgramShadow = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/basic/shadow/shadow_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/basic/shadow/shadow_fs.txt"),
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
        surfaceSize = new Size(width, height);
        glCamera.setPerspectiveM(width, height, 60f, 1f, 100f);
        glCamera.setLookAtM(CAMERA_POSITION[0], CAMERA_POSITION[1], CAMERA_POSITION[2], 0f, 0f, 0f, 0f, 1f, 0f);
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

        glFramebufferShadow.bind(GLES30.GL_DRAW_FRAMEBUFFER);

        GLES30.glViewport(0, 0, SHADOWMAP_SIZE, SHADOWMAP_SIZE);
        GLES30.glClearColor(1f, 1f, 1f, 1f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        glProgramShadow.useProgram();

        Matrix.multiplyMM(modelViewProjectionMatrix, 0, glLight.viewProjMatrix(), 0, rotationMatrix, 0);
        GLES30.glUniformMatrix4fv(glProgramShadow.getUniformLocation("uModelViewProjectionMatrix"), 1, false, modelViewProjectionMatrix, 0);
        renderCubeFilled();

        Matrix.multiplyMM(modelViewProjectionMatrix, 0, glLight.viewProjMatrix(), 0, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(glProgramShadow.getUniformLocation("uModelViewProjectionMatrix"), 1, false, modelViewProjectionMatrix, 0);
        renderCubeFilled();

        glFramebufferShadow.unbind(GLES30.GL_DRAW_FRAMEBUFFER);

        GLES30.glViewport(0, 0, surfaceSize.getWidth(), surfaceSize.getHeight());
        GLES30.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        glProgram.useProgram();

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureShadow.bind(GLES30.GL_TEXTURE_2D);
        glSamplerShadow.bind(0);

        Matrix.multiplyMM(modelViewMatrix, 0, glCamera.getLookAtM(), 0, rotationMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, glCamera.getPerspectiveM(), 0, modelViewMatrix, 0);

        GLES30.glUniformMatrix4fv(glProgram.getUniformLocation("uModelViewMatrix"), 1, false, modelViewMatrix, 0);
        GLES30.glUniformMatrix4fv(glProgram.getUniformLocation("uModelViewProjectionMatrix"), 1, false, modelViewProjectionMatrix, 0);
        GLES30.glUniformMatrix4fv(glProgram.getUniformLocation("uShadowMatrix"), 1, false, glLight.shadowMatrix(glCamera.getLookAtM()), 0);
        GLES30.glUniform1f(glProgram.getUniformLocation("uNormalFactor"), 1f);
        GLES30.glUniform4fv(glProgram.getUniformLocation("uMaterial"), 1, MATERIAL_CUBE, 0);
        GLES30.glUniform3fv(glProgram.getUniformLocation("uLightPosition"), 1, LIGHT_POSITION, 0);
        renderCubeFilled();

        Matrix.multiplyMM(modelViewMatrix, 0, glCamera.getLookAtM(), 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, glCamera.getPerspectiveM(), 0, modelViewMatrix, 0);

        GLES30.glUniformMatrix4fv(glProgram.getUniformLocation("uModelViewMatrix"), 1, false, modelViewMatrix, 0);
        GLES30.glUniformMatrix4fv(glProgram.getUniformLocation("uModelViewProjectionMatrix"), 1, false, modelViewProjectionMatrix, 0);
        GLES30.glUniformMatrix4fv(glProgram.getUniformLocation("uShadowMatrix"), 1, false, glLight.shadowMatrix(glCamera.getLookAtM()), 0);
        GLES30.glUniform1f(glProgram.getUniformLocation("uNormalFactor"), 1f);
        GLES30.glUniform4fv(glProgram.getUniformLocation("uMaterial"), 1, MATERIAL_CUBE_ENV, 0);
        GLES30.glUniform3fv(glProgram.getUniformLocation("uLightPosition"), 1, LIGHT_POSITION, 0);
        renderCubeFilled();
    }

    @Override
    public void onSurfaceReleased() {
    }

}
