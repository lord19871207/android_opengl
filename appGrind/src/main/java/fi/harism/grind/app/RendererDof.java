package fi.harism.grind.app;

import android.content.Context;
import android.graphics.Point;
import android.opengl.GLES30;

import java.nio.ByteBuffer;

import fi.harism.opengl.lib.gl.GlFramebuffer;
import fi.harism.opengl.lib.gl.GlProgram;
import fi.harism.opengl.lib.gl.GlSampler;
import fi.harism.opengl.lib.gl.GlTexture;
import fi.harism.opengl.lib.gl.GlUtils;
import fi.harism.opengl.lib.model.GlCamera;
import fi.harism.opengl.lib.util.GlRenderer;

public class RendererDof implements GlRenderer {

    private final ByteBuffer bufferQuad;
    private final GlCamera glCamera;
    private final GlFramebuffer glFramebufferOut;
    private final GlFramebuffer glFramebufferHalf1;
    private final GlFramebuffer glFramebufferHalf2;
    private final GlTexture glTextureIn;
    private final GlTexture glTextureHalf1;
    private final GlTexture glTextureHalf2;
    private final GlSampler glSampler;
    private final GlProgram glProgramDofCoc;
    private final GlProgram glProgramDofOut;
    private final GlProgram glProgramDofFarVert;
    private final GlProgram glProgramDofFarDiag;

    private final Point surfaceSize = new Point();

    public RendererDof(Context context, ByteBuffer bufferQuad, GlCamera camera,
                       GlFramebuffer framebufferOut,
                       GlFramebuffer framebufferHalf1, GlFramebuffer framebufferHalf2,
                       GlTexture textureIn,
                       GlTexture textureHalf1, GlTexture textureHalf2
    ) throws Exception {
        this.bufferQuad = bufferQuad;
        glCamera = camera;
        glFramebufferOut = framebufferOut;
        glFramebufferHalf1 = framebufferHalf1;
        glFramebufferHalf2 = framebufferHalf2;
        glTextureIn = textureIn;
        glTextureHalf1 = textureHalf1;
        glTextureHalf2 = textureHalf2;

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

        final String DOF_FAR_VERT_VS = GlUtils.loadString(context, "shaders/dof_far_vert_vs.txt");
        final String DOF_FAR_VERT_FS = GlUtils.loadString(context, "shaders/dof_far_vert_fs.txt");
        glProgramDofFarVert = new GlProgram(DOF_FAR_VERT_VS, DOF_FAR_VERT_FS, null).useProgram();
        GLES30.glUniform1i(glProgramDofFarVert.getUniformLocation("sTexture"), 0);

        final String DOF_FAR_DIAG_VS = GlUtils.loadString(context, "shaders/dof_far_diag_vs.txt");
        final String DOF_FAR_DIAG_FS = GlUtils.loadString(context, "shaders/dof_far_diag_fs.txt");
        glProgramDofFarDiag = new GlProgram(DOF_FAR_DIAG_VS, DOF_FAR_DIAG_FS, null).useProgram();
        GLES30.glUniform1i(glProgramDofFarDiag.getUniformLocation("sTexture"), 0);
    }

    @Override
    public void onSurfaceCreated() {
        throw new RuntimeException("Use constructor from parent onSurfaceCreated instead.");
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        surfaceSize.x = width;
        surfaceSize.y = height;
    }

    @Override
    public void onRenderFrame() {

        renderDofCoc();
        renderDofBokeh(1f / (surfaceSize.x / 8), 1f / (surfaceSize.y / 8), 4,
                glFramebufferHalf1, glFramebufferHalf2, glTextureHalf1, glTextureHalf2);
        //renderDofBokeh(1f / (surfaceSize.x / 4), 1f / (surfaceSize.y / 4), 1,
        //        glFramebufferHalf1, glFramebufferHalf2, glTextureHalf1, glTextureHalf2);
        renderDofOut();




        /*

        float pixelX = 1f / (surfaceSize.x / 16);
        float pixelY = 1f / (surfaceSize.y / 16);
        int count = 3;

        glFramebufferHalf1.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        glProgramDofFarVert.useProgram();
        GLES30.glViewport(0, 0, surfaceSize.x / 2, surfaceSize.y / 2);
        GLES30.glUniform2f(glProgramDofFarVert.getUniformLocation("uSampleOffset"), pixelX, 0f);
        GLES30.glUniform1i(glProgramDofFarVert.getUniformLocation("uCount"), count);
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_BYTE, false, 0, bufferQuad);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureIn.bind(GLES30.GL_TEXTURE_2D);
        glSampler.bind(0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(0);
        glTextureIn.unbind(GLES30.GL_TEXTURE_2D);
        glFramebufferHalf1.unbind(GLES30.GL_DRAW_FRAMEBUFFER);

        glFramebufferHalf2.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        glProgramDofFarDiag.useProgram();
        GLES30.glViewport(0, 0, surfaceSize.x / 2, surfaceSize.y / 2);
        GLES30.glUniform2f(glProgramDofFarDiag.getUniformLocation("uSampleOffset1"), 0.53f * pixelX, pixelY);
        GLES30.glUniform2f(glProgramDofFarDiag.getUniformLocation("uSampleOffset2"), -0.53f * pixelX, pixelY);
        GLES30.glUniform1i(glProgramDofFarDiag.getUniformLocation("uCount"), count);
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_BYTE, false, 0, bufferQuad);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureHalf1.bind(GLES30.GL_TEXTURE_2D);
        glSampler.bind(0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(0);
        glTextureHalf1.unbind(GLES30.GL_TEXTURE_2D);
        glFramebufferHalf2.unbind(GLES30.GL_DRAW_FRAMEBUFFER);


        pixelX = 1f / (surfaceSize.x / 4);
        pixelY = 1f / (surfaceSize.y / 4);
        count = 2;

        glFramebufferHalf1.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        glProgramDofFarVert.useProgram();
        GLES30.glViewport(0, 0, surfaceSize.x / 2, surfaceSize.y / 2);
        GLES30.glUniform2f(glProgramDofFarVert.getUniformLocation("uSampleOffset"), pixelX, 0f);
        GLES30.glUniform1i(glProgramDofFarVert.getUniformLocation("uCount"), count);
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_BYTE, false, 0, bufferQuad);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureHalf2.bind(GLES30.GL_TEXTURE_2D);
        glSampler.bind(0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(0);
        glTextureHalf2.unbind(GLES30.GL_TEXTURE_2D);
        glFramebufferHalf1.unbind(GLES30.GL_DRAW_FRAMEBUFFER);

        glFramebufferHalf2.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        glProgramDofFarDiag.useProgram();
        GLES30.glViewport(0, 0, surfaceSize.x / 2, surfaceSize.y / 2);
        GLES30.glUniform2f(glProgramDofFarDiag.getUniformLocation("uSampleOffset1"), 0.53f * pixelX, pixelY);
        GLES30.glUniform2f(glProgramDofFarDiag.getUniformLocation("uSampleOffset2"), -0.53f * pixelX, pixelY);
        GLES30.glUniform1i(glProgramDofFarDiag.getUniformLocation("uCount"), count);
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_BYTE, false, 0, bufferQuad);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureHalf1.bind(GLES30.GL_TEXTURE_2D);
        glSampler.bind(0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(0);
        glTextureHalf1.unbind(GLES30.GL_TEXTURE_2D);
        glFramebufferHalf2.unbind(GLES30.GL_DRAW_FRAMEBUFFER);
        */

    }

    @Override
    public void onSurfaceReleased() {
    }

    private void renderDofCoc() {
        glFramebufferHalf1.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        glProgramDofCoc.useProgram();
        GLES30.glViewport(0, 0, surfaceSize.x / 2, surfaceSize.y / 2);

        GLES30.glUniform1f(glProgramDofCoc.getUniformLocation("uApertureDiameter"), glCamera.getApertureDiameter());
        GLES30.glUniform1f(glProgramDofCoc.getUniformLocation("uFocalLength"), glCamera.getFocalLength());
        GLES30.glUniform1f(glProgramDofCoc.getUniformLocation("uPlaneInFocus"), glCamera.getPlaneInFocus());
        GLES30.glUniform1f(glProgramDofCoc.getUniformLocation("uSensorHeight"), glCamera.getSensorHeight());

        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_BYTE, false, 0, bufferQuad);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureIn.bind(GLES30.GL_TEXTURE_2D);
        glSampler.bind(0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(0);
        glTextureIn.unbind(GLES30.GL_TEXTURE_2D);
        glFramebufferHalf1.unbind(GLES30.GL_DRAW_FRAMEBUFFER);
    }

    private void renderDofOut() {
        glFramebufferOut.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        glProgramDofOut.useProgram();
        GLES30.glViewport(0, 0, surfaceSize.x, surfaceSize.y);
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_BYTE, false, 0, bufferQuad);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureHalf1.bind(GLES30.GL_TEXTURE_2D);
        glSampler.bind(0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(0);
        glTextureHalf1.unbind(GLES30.GL_TEXTURE_2D);
        glFramebufferOut.unbind(GLES30.GL_DRAW_FRAMEBUFFER);
    }

    private void renderDofBokeh(float pixelDx, float pixelDy, int pixelCount,
                                GlFramebuffer framebuffer1, GlFramebuffer framebuffer2,
                                GlTexture texture1, GlTexture texture2) {
        framebuffer2.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        glProgramDofFarVert.useProgram();
        GLES30.glViewport(0, 0, surfaceSize.x / 2, surfaceSize.y / 2);
        GLES30.glUniform2f(glProgramDofFarVert.getUniformLocation("uSampleOffset"), pixelDx, 0f);
        GLES30.glUniform1i(glProgramDofFarVert.getUniformLocation("uCount"), pixelCount);
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_BYTE, false, 0, bufferQuad);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        texture1.bind(GLES30.GL_TEXTURE_2D);
        glSampler.bind(0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(0);
        texture1.unbind(GLES30.GL_TEXTURE_2D);
        framebuffer1.unbind(GLES30.GL_DRAW_FRAMEBUFFER);

        framebuffer1.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        glProgramDofFarDiag.useProgram();
        GLES30.glViewport(0, 0, surfaceSize.x / 2, surfaceSize.y / 2);
        GLES30.glUniform2f(glProgramDofFarDiag.getUniformLocation("uSampleOffset1"), 0.53f * pixelDx, pixelDy);
        GLES30.glUniform2f(glProgramDofFarDiag.getUniformLocation("uSampleOffset2"), -0.53f * pixelDx, pixelDy);
        GLES30.glUniform1i(glProgramDofFarDiag.getUniformLocation("uCount"), pixelCount);
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_BYTE, false, 0, bufferQuad);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        texture2.bind(GLES30.GL_TEXTURE_2D);
        glSampler.bind(0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(0);
        texture2.unbind(GLES30.GL_TEXTURE_2D);
        glFramebufferHalf2.unbind(GLES30.GL_DRAW_FRAMEBUFFER);
    }

}
