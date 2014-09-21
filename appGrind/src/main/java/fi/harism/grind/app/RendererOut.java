package fi.harism.grind.app;

import android.content.Context;
import android.graphics.Point;
import android.opengl.GLES30;

import java.nio.ByteBuffer;

import fi.harism.opengl.lib.gl.GlProgram;
import fi.harism.opengl.lib.gl.GlSampler;
import fi.harism.opengl.lib.gl.GlTexture;
import fi.harism.opengl.lib.gl.GlUtils;
import fi.harism.opengl.lib.util.GlRenderer;

public class RendererOut implements GlRenderer {

    private final ByteBuffer mBufferQuad;
    private final GlTexture mTextureIn;
    private final GlTexture mTextureRand;
    private final GlSampler mSampler;
    private final GlProgram mProgramOut;
    private final Point mSurfaceSize = new Point();

    public RendererOut(Context context, ByteBuffer bufferQuad, GlTexture textureIn, GlTexture textureRand) throws Exception {
        mBufferQuad = bufferQuad;
        mTextureIn = textureIn;
        mTextureRand = textureRand;

        mSampler = new GlSampler();
        mSampler.parameter(GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        mSampler.parameter(GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        mSampler.parameter(GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
        mSampler.parameter(GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);

        final String OUT_VS = GlUtils.loadString(context, "shaders/out_vs.txt");
        final String OUT_FS = GlUtils.loadString(context, "shaders/out_fs.txt");
        mProgramOut = new GlProgram(OUT_VS, OUT_FS, null).useProgram();
        GLES30.glUniform1i(mProgramOut.getUniformLocation("sTexture"), 0);
        GLES30.glUniform1i(mProgramOut.getUniformLocation("sTextureRand"), 1);
    }

    @Override
    public void onSurfaceCreated() {
        throw new RuntimeException("Use constructor from parent onSurfaceCreated instead.");
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        mSurfaceSize.x = width;
        mSurfaceSize.y = height;
    }

    @Override
    public void onRenderFrame() {
        mProgramOut.useProgram();
        GLES30.glViewport(0, 0, mSurfaceSize.x, mSurfaceSize.y);
        GLES30.glUniform2f(mProgramOut.getUniformLocation("uTextureRandScale"), mSurfaceSize.x / 1024f, mSurfaceSize.y / 1024f);
        GLES30.glUniform2f(mProgramOut.getUniformLocation("uTextureRandDiff"), (float) (Math.random() * 2), (float) (Math.random() * 2));
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_BYTE, false, 0, mBufferQuad);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        mTextureIn.bind(GLES30.GL_TEXTURE_2D);
        mSampler.bind(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        mTextureRand.bind(GLES30.GL_TEXTURE_2D);
        mSampler.bind(1);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(0);
        mTextureIn.unbind(GLES30.GL_TEXTURE_2D);
        mTextureRand.unbind(GLES30.GL_TEXTURE_2D);
    }

    @Override
    public void onSurfaceReleased() {
    }
}
