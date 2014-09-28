package fi.harism.app.grind;

import android.content.Context;
import android.graphics.Point;
import android.opengl.GLES30;

import java.nio.ByteBuffer;

import fi.harism.lib.opengl.gl.GlFramebuffer;
import fi.harism.lib.opengl.gl.GlProgram;
import fi.harism.lib.opengl.gl.GlSampler;
import fi.harism.lib.opengl.gl.GlTexture;
import fi.harism.lib.opengl.gl.GlUtils;
import fi.harism.lib.opengl.model.GlCamera;
import fi.harism.lib.opengl.util.GlRenderer;

public class RendererDof implements GlRenderer {

    private final ByteBuffer bufferQuad;
    private final GlCamera glCamera;
    private final GlFramebuffer glFramebufferOut;
    private final GlTexture glTextureIn;
    private final GlTexture glTextureDofCoC;
    private final GlTexture glTextureDofVert;
    private final GlTexture glTextureDofDiag;

    private final GlFramebuffer glFramebufferCoC;
    private final GlFramebuffer glFramebufferDofVert;
    private final GlFramebuffer glFramebufferDofDiag;

    private final GlSampler glSampler;
    private final GlProgram glProgramDofCoc;
    private final GlProgram glProgramDofOut;
    private final GlProgram glProgramDofVert;
    private final GlProgram glProgramDofDiag;

    private final Point surfaceSize = new Point();

    public RendererDof(Context context, ByteBuffer bufferQuad, GlCamera camera,
                       GlFramebuffer framebufferOut,
                       GlTexture textureIn,
                       GlTexture textureHalf1, GlTexture textureHalf2, GlTexture textureHalf3
    ) throws Exception {
        this.bufferQuad = bufferQuad;
        glCamera = camera;
        glFramebufferOut = framebufferOut;
        glTextureIn = textureIn;
        glTextureDofCoC = textureHalf1;
        glTextureDofVert = textureHalf2;
        glTextureDofDiag = textureHalf3;

        glFramebufferCoC = new GlFramebuffer();
        glFramebufferDofVert = new GlFramebuffer();
        glFramebufferDofDiag = new GlFramebuffer();

        glSampler = new GlSampler();
        glSampler.parameter(GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        glSampler.parameter(GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        glSampler.parameter(GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        glSampler.parameter(GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        final String DOF_COC_VS = GlUtils.loadString(context, "shaders/dof_coc_vs.txt");
        final String DOF_COC_FS = GlUtils.loadString(context, "shaders/dof_coc_fs.txt");
        glProgramDofCoc = new GlProgram(DOF_COC_VS, DOF_COC_FS, null).useProgram();
        GLES30.glUniform1i(glProgramDofCoc.getUniformLocation("sTexture"), 0);

        final String DOF_OUT_VS = GlUtils.loadString(context, "shaders/dof_out_vs.txt");
        final String DOF_OUT_FS = GlUtils.loadString(context, "shaders/dof_out_fs.txt");
        glProgramDofOut = new GlProgram(DOF_OUT_VS, DOF_OUT_FS, null).useProgram();
        GLES30.glUniform1i(glProgramDofOut.getUniformLocation("sTexture"), 0);

        final String DOF_VERT_VS = GlUtils.loadString(context, "shaders/dof_vert_vs.txt");
        final String DOF_VERT_FS = GlUtils.loadString(context, "shaders/dof_vert_fs.txt");
        glProgramDofVert = new GlProgram(DOF_VERT_VS, DOF_VERT_FS, null).useProgram();
        GLES30.glUniform1i(glProgramDofVert.getUniformLocation("sTexture"), 0);
        GLES30.glUniform1i(glProgramDofVert.getUniformLocation("sTextureCoC"), 1);

        final String DOF_DIAG_VS = GlUtils.loadString(context, "shaders/dof_diag_vs.txt");
        final String DOF_DIAG_FS = GlUtils.loadString(context, "shaders/dof_diag_fs.txt");
        glProgramDofDiag = new GlProgram(DOF_DIAG_VS, DOF_DIAG_FS, null).useProgram();
        GLES30.glUniform1i(glProgramDofDiag.getUniformLocation("sTexture"), 0);
        GLES30.glUniform1i(glProgramDofDiag.getUniformLocation("sTextureCoC"), 1);
    }

    @Override
    public void onSurfaceCreated() {
        throw new RuntimeException("Use constructor from parent onSurfaceCreated instead.");
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        surfaceSize.x = width;
        surfaceSize.y = height;

        glFramebufferCoC.bind(GLES30.GL_DRAW_FRAMEBUFFER)
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, glTextureDofDiag.name(), 0)
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT1, GLES30.GL_TEXTURE_2D, glTextureDofCoC.name(), 0)
                .drawBuffers(GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_COLOR_ATTACHMENT1)
                .unbind(GLES30.GL_DRAW_FRAMEBUFFER);

        glFramebufferDofVert.bind(GLES30.GL_DRAW_FRAMEBUFFER)
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, glTextureDofVert.name(), 0)
                .drawBuffers(GLES30.GL_COLOR_ATTACHMENT0)
                .unbind(GLES30.GL_DRAW_FRAMEBUFFER);

        glFramebufferDofDiag.bind(GLES30.GL_DRAW_FRAMEBUFFER)
                .texture2D(GLES30.GL_DRAW_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, glTextureDofDiag.name(), 0)
                .drawBuffers(GLES30.GL_COLOR_ATTACHMENT0)
                .unbind(GLES30.GL_DRAW_FRAMEBUFFER);
    }

    @Override
    public void onRenderFrame() {
        float aspectRatio = (float) surfaceSize.x / surfaceSize.y;
        renderDofCoc();
        renderDofBokeh(0.008f, aspectRatio * 0.008f, 4);
        renderDofBokeh(0.004f, aspectRatio * 0.004f, 2);
        renderDofOut();
    }

    @Override
    public void onSurfaceReleased() {
    }

    private void renderDofCoc() {

        // ( apertureDiameter * focalLength * (planeInFocus - x) ) /
        // ( x * (planeInFocus - focalLength) * sensorHeight )
        //
        // simplifies to
        //
        // (k4 / x) -  k3

        float k1 = glCamera.getApertureDiameter() * glCamera.getFocalLength();
        float k2 = glCamera.getPlaneInFocus() - glCamera.getFocalLength();
        float k3 = k1 / (k2 * glCamera.getSensorHeight());
        float k4 = k3 * glCamera.getPlaneInFocus();

        glFramebufferCoC.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        glProgramDofCoc.useProgram();
        GLES30.glViewport(0, 0, surfaceSize.x / 2, surfaceSize.y / 2);

        GLES30.glUniform1f(glProgramDofCoc.getUniformLocation("uCamera1"), k4);
        GLES30.glUniform1f(glProgramDofCoc.getUniformLocation("uCamera2"), k3);

        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_BYTE, false, 0, bufferQuad);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureIn.bind(GLES30.GL_TEXTURE_2D);
        glSampler.bind(0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(0);
        glTextureIn.unbind(GLES30.GL_TEXTURE_2D);
        glFramebufferCoC.unbind(GLES30.GL_DRAW_FRAMEBUFFER);
    }

    private void renderDofOut() {
        glFramebufferOut.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        glProgramDofOut.useProgram();
        GLES30.glViewport(0, 0, surfaceSize.x, surfaceSize.y);
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_BYTE, false, 0, bufferQuad);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureDofDiag.bind(GLES30.GL_TEXTURE_2D);
        glSampler.bind(0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(0);
        glTextureDofDiag.unbind(GLES30.GL_TEXTURE_2D);
        glFramebufferOut.unbind(GLES30.GL_DRAW_FRAMEBUFFER);
    }

    private void renderDofBokeh(float pixelDx, float pixelDy, int pixelCount) {
        glFramebufferDofVert.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        glProgramDofVert.useProgram();
        GLES30.glViewport(0, 0, surfaceSize.x / 2, surfaceSize.y / 2);
        GLES30.glUniform2f(glProgramDofVert.getUniformLocation("uSampleOffset"), pixelDx, 0f);
        GLES30.glUniform1i(glProgramDofVert.getUniformLocation("uCount"), pixelCount);
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_BYTE, false, 0, bufferQuad);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureDofDiag.bind(GLES30.GL_TEXTURE_2D);
        glSampler.bind(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        glTextureDofCoC.bind(GLES30.GL_TEXTURE_2D);
        glSampler.bind(0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(0);
        glTextureDofDiag.unbind(GLES30.GL_TEXTURE_2D);
        glTextureDofCoC.unbind(GLES30.GL_TEXTURE_2D);
        glFramebufferDofVert.unbind(GLES30.GL_DRAW_FRAMEBUFFER);

        glFramebufferDofDiag.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        glProgramDofDiag.useProgram();
        GLES30.glViewport(0, 0, surfaceSize.x / 2, surfaceSize.y / 2);
        GLES30.glUniform2f(glProgramDofDiag.getUniformLocation("uSampleOffset1"), 0.53f * pixelDx, pixelDy);
        GLES30.glUniform2f(glProgramDofDiag.getUniformLocation("uSampleOffset2"), -0.53f * pixelDx, pixelDy);
        GLES30.glUniform1i(glProgramDofDiag.getUniformLocation("uCount"), pixelCount);
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_BYTE, false, 0, bufferQuad);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureDofVert.bind(GLES30.GL_TEXTURE_2D);
        glSampler.bind(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        glTextureDofCoC.bind(GLES30.GL_TEXTURE_2D);
        glSampler.bind(0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(0);
        glTextureDofVert.unbind(GLES30.GL_TEXTURE_2D);
        glTextureDofCoC.unbind(GLES30.GL_TEXTURE_2D);
        glFramebufferDofDiag.unbind(GLES30.GL_DRAW_FRAMEBUFFER);
    }

}
