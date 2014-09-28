package fi.harism.app.grind;

import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.opengl.GLES30;

import java.nio.ByteBuffer;

import fi.harism.lib.opengl.gl.GlProgram;
import fi.harism.lib.opengl.gl.GlSampler;
import fi.harism.lib.opengl.gl.GlTexture;
import fi.harism.lib.opengl.gl.GlUtils;
import fi.harism.lib.opengl.util.GlRenderer;

public class RendererOut implements GlRenderer {

    private final ByteBuffer bufferQuad;
    private final GlTexture glTextureIn;
    private final GlTexture glTextureRand;
    private final GlSampler glSampler;
    private final GlProgram glProgramOut;
    private final Point surfaceSize = new Point();
    private final PointF aspectRatio = new PointF();

    public RendererOut(Context context, ByteBuffer bufferQuad, GlTexture textureIn, GlTexture textureRand) throws Exception {
        this.bufferQuad = bufferQuad;
        glTextureIn = textureIn;
        glTextureRand = textureRand;

        glSampler = new GlSampler();
        glSampler.parameter(GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        glSampler.parameter(GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        glSampler.parameter(GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        glSampler.parameter(GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        final String OUT_VS = GlUtils.loadString(context, "shaders/out_vs.txt");
        final String OUT_FS = GlUtils.loadString(context, "shaders/out_fs.txt");
        glProgramOut = new GlProgram(OUT_VS, OUT_FS, null).useProgram();
        GLES30.glUniform1i(glProgramOut.getUniformLocation("sTexture"), 0);
        GLES30.glUniform1i(glProgramOut.getUniformLocation("sTextureRand"), 1);
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
        glProgramOut.useProgram();
        GLES30.glViewport(0, 0, surfaceSize.x, surfaceSize.y);
        GLES30.glUniform2f(glProgramOut.getUniformLocation("uAspectRatio"), aspectRatio.x, aspectRatio.y);
        GLES30.glUniform2f(glProgramOut.getUniformLocation("uTextureRandScale"), surfaceSize.x / 1024f, surfaceSize.y / 1024f);
        GLES30.glUniform2f(glProgramOut.getUniformLocation("uTextureRandDiff"), (float) (Math.random() * 2), (float) (Math.random() * 2));
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_BYTE, false, 0, bufferQuad);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        glTextureIn.bind(GLES30.GL_TEXTURE_2D);
        glSampler.bind(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        glTextureRand.bind(GLES30.GL_TEXTURE_2D);
        glSampler.bind(1);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(0);
        glTextureIn.unbind(GLES30.GL_TEXTURE_2D);
        glTextureRand.unbind(GLES30.GL_TEXTURE_2D);
    }

    @Override
    public void onSurfaceReleased() {
    }

    public void setAspectRatio(float aspectX, float aspectY) {
        aspectRatio.x = aspectX;
        aspectRatio.y = aspectY;
    }
}
