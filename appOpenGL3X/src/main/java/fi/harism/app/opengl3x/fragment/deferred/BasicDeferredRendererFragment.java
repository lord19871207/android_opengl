package fi.harism.app.opengl3x.fragment.deferred;

import android.opengl.GLES30;
import android.os.Bundle;
import android.widget.Toast;

import fi.harism.app.opengl3x.R;
import fi.harism.lib.opengl.egl.EglCore;
import fi.harism.lib.opengl.gl.GlFramebuffer;
import fi.harism.lib.opengl.gl.GlProgram;
import fi.harism.lib.opengl.gl.GlRenderbuffer;
import fi.harism.lib.opengl.gl.GlSampler;
import fi.harism.lib.opengl.gl.GlTexture;
import fi.harism.lib.opengl.gl.GlUtils;

public class BasicDeferredRendererFragment extends DeferredRendererFragment {

    private static final String RENDERER_ID = "renderer.deferred.basic";

    private static final String UNIFORMS_SCENE[] = {"uModelViewMatrix", "uModelViewProjMatrix"};
    private final int uniformsScene[] = new int[UNIFORMS_SCENE.length];

    private GlSampler glSamplerNearest;
    private GlTexture glTextureColor;
    private GlTexture glTexturePosition;
    private GlTexture glTextureNormal;
    private GlRenderbuffer glRenderbufferDepth;
    private GlFramebuffer glFramebufferDeferred;
    private GlProgram glProgramScene;
    private GlProgram glProgramOutput;

    @Override
    public String getRendererId() {
        return RENDERER_ID;
    }

    @Override
    public int getTitleStringId() {
        return R.string.renderer_deferred_basic_title;
    }

    @Override
    public int getCaptionStringId() {
        return R.string.renderer_deferred_basic_caption;
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

        glTextureColor = new GlTexture();
        glTextureNormal = new GlTexture();
        glTexturePosition = new GlTexture();
        glRenderbufferDepth = new GlRenderbuffer();
        glFramebufferDeferred = new GlFramebuffer();

        prepareScene();

        try {
            glProgramScene = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/deferred/basic/scene_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/deferred/basic/scene_fs.txt"),
                    null).useProgram();
            glProgramScene.getUniformIndices(UNIFORMS_SCENE, uniformsScene);

            glProgramOutput = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/deferred/basic/output_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/deferred/basic/output_fs.txt"),
                    null).useProgram();
            GLES30.glUniform1i(glProgramOutput.getUniformLocation("sTexturePosition"), 0);
            GLES30.glUniform1i(glProgramOutput.getUniformLocation("sTextureNormal"), 1);
            GLES30.glUniform1i(glProgramOutput.getUniformLocation("sTextureColor"), 2);

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

        glTexturePosition
                .bind(GLES30.GL_TEXTURE_2D)
                .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, width, height, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null)
                .unbind(GLES30.GL_TEXTURE_2D);
        glTextureNormal
                .bind(GLES30.GL_TEXTURE_2D)
                .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, width, height, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null)
                .unbind(GLES30.GL_TEXTURE_2D);
        glTextureColor
                .bind(GLES30.GL_TEXTURE_2D)
                .texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8, width, height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)
                .unbind(GLES30.GL_TEXTURE_2D);

        glRenderbufferDepth
                .bind(GLES30.GL_RENDERBUFFER)
                .storage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT32F, width, height)
                .unbind(GLES30.GL_RENDERBUFFER);

        glFramebufferDeferred
                .bind(GLES30.GL_DRAW_FRAMEBUFFER)
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, glTexturePosition.name(), 0)
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT1, GLES30.GL_TEXTURE_2D, glTextureNormal.name(), 0)
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT2, GLES30.GL_TEXTURE_2D, glTextureColor.name(), 0)
                .renderbuffer(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, glRenderbufferDepth.name())
                .drawBuffers(GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_COLOR_ATTACHMENT1, GLES30.GL_COLOR_ATTACHMENT2)
                .unbind(GLES30.GL_DRAW_FRAMEBUFFER);
    }

    @Override
    public void onRenderFrame() {
        glFramebufferDeferred.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        GLES30.glClearColor(.0f, .0f, .0f, .0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        glProgramScene.useProgram();
        renderScene(uniformsScene[0], uniformsScene[1]);
        glFramebufferDeferred.unbind(GLES30.GL_DRAW_FRAMEBUFFER);

        glProgramOutput.useProgram();
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTexturePosition.bind(GLES30.GL_TEXTURE_2D);
        glSamplerNearest.bind(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        glTextureNormal.bind(GLES30.GL_TEXTURE_2D);
        glSamplerNearest.bind(1);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE2);
        glTextureColor.bind(GLES30.GL_TEXTURE_2D);
        glSamplerNearest.bind(2);
        renderQuad();
    }

    @Override
    public void onSurfaceReleased() {
    }

}
