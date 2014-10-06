package fi.harism.app.grind;

import android.content.Context;
import android.graphics.Point;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.SystemClock;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import fi.harism.lib.opengl.gl.GlBuffer;
import fi.harism.lib.opengl.gl.GlFramebuffer;
import fi.harism.lib.opengl.gl.GlProgram;
import fi.harism.lib.opengl.gl.GlUtils;
import fi.harism.lib.opengl.model.GlCamera;
import fi.harism.lib.opengl.model.GlObject;
import fi.harism.lib.opengl.util.GlRenderer;
import fi.harism.lib.utils.MersenneTwisterFast;

public class RendererScene implements GlRenderer {

    private final int DOT_COUNT = 500;

    private final ByteBuffer mBufferQuad;
    private final GlCamera mCamera;
    private final GlFramebuffer mFramebuffer;
    private final FloatBuffer mBufferDots;
    private final GlProgram mProgramDots;
    private final GlProgram mProgramCube;
    private final Point mSurfaceSize = new Point();
    private GlObject mObject;

    private GlBuffer mVboDots;

    public RendererScene(Context context, ByteBuffer bufferQuad, GlCamera camera, GlFramebuffer framebuffer) throws Exception {
        mBufferQuad = bufferQuad;
        mCamera = camera;
        mFramebuffer = framebuffer;

        MersenneTwisterFast rand = new MersenneTwisterFast(7348);
        mBufferDots = ByteBuffer.allocateDirect(4 * 3 * DOT_COUNT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (int i = 0; i < 3 * DOT_COUNT; ++i) {
            mBufferDots.put(rand.nextFloat(true, true) * 100 - 50);
        }
        mBufferDots.position(0);
        mVboDots = new GlBuffer();
        mVboDots.bind(GLES30.GL_ARRAY_BUFFER);
        mVboDots.data(GLES30.GL_ARRAY_BUFFER, 4 * 3 * DOT_COUNT, mBufferDots, GLES30.GL_STATIC_DRAW);
        mVboDots.unbind(GLES30.GL_ARRAY_BUFFER);

        final String DOT_VS = GlUtils.loadString(context, "shaders/dots_vs.txt");
        final String DOT_FS = GlUtils.loadString(context, "shaders/dots_fs.txt");
        mProgramDots = new GlProgram(DOT_VS, DOT_FS, null);

        final String CUBE_VS = GlUtils.loadString(context, "shaders/cube_vs.txt");
        final String CUBE_FS = GlUtils.loadString(context, "shaders/cube_fs.txt");
        mProgramCube = new GlProgram(CUBE_VS, CUBE_FS, null).useProgram();
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
        mFramebuffer.bind(GLES30.GL_DRAW_FRAMEBUFFER);
        GLES30.glViewport(0, 0, mSurfaceSize.x, mSurfaceSize.y);
        GLES30.glClearColor(0.2f, 0.4f, 0.6f, 100.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        renderObject(mObject);
        renderDots();
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        mFramebuffer.unbind(GLES30.GL_DRAW_FRAMEBUFFER);
    }

    @Override
    public void onSurfaceReleased() {
    }

    public void setObject(GlObject object) {
        mObject = object;
    }

    private void renderObject(GlObject object) {
        mProgramCube.useProgram();
        final float angle = SystemClock.uptimeMillis() % 3600 / 10f;
        final float[] modelViewProjM = new float[16];
        Matrix.setIdentityM(modelViewProjM, 0);
        Matrix.rotateM(modelViewProjM, 0, angle, 0, 1, 0);
        GLES30.glUniformMatrix4fv(mProgramCube.getUniformLocation("uViewM"), 1, false, mCamera.viewMatrix(), 0);
        Matrix.multiplyMM(modelViewProjM, 0, mCamera.viewMatrix(), 0, modelViewProjM, 0);
        GLES30.glUniformMatrix4fv(mProgramCube.getUniformLocation("uModelViewM"), 1, false, modelViewProjM, 0);
        Matrix.multiplyMM(modelViewProjM, 0, mCamera.projMatrix(), 0, modelViewProjM, 0);
        GLES30.glUniformMatrix4fv(mProgramCube.getUniformLocation("uModelViewProjM"), 1, false, modelViewProjM, 0);
        GLES30.glUniform3fv(mProgramCube.getUniformLocation("uEyePositionW"), 1, mCamera.position(), 0);

        object.getVertexBuffer().bind(GLES30.GL_ARRAY_BUFFER);
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, 0);
        object.getVertexBuffer().unbind(GLES30.GL_ARRAY_BUFFER);
        GLES30.glEnableVertexAttribArray(0);

        object.getNormalBuffer().bind(GLES30.GL_ARRAY_BUFFER);
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 0, 0);
        object.getNormalBuffer().unbind(GLES30.GL_ARRAY_BUFFER);
        GLES30.glEnableVertexAttribArray(1);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, object.getVertexCount());
        GLES30.glDisableVertexAttribArray(0);
        GLES30.glDisableVertexAttribArray(1);
    }

    private void renderDots() {
        mProgramDots.useProgram();
        GLES30.glUniformMatrix4fv(mProgramDots.getUniformLocation("uProjM"), 1, false, mCamera.projMatrix(), 0);
        GLES30.glUniformMatrix4fv(mProgramDots.getUniformLocation("uViewM"), 1, false, mCamera.viewMatrix(), 0);
        //GLES30.glDepthMask(false);
        //GLES30.glEnable(GLES30.GL_BLEND);
        //GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        mVboDots.bind(GLES30.GL_ARRAY_BUFFER);
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, 0);
        mVboDots.unbind(GLES30.GL_ARRAY_BUFFER);

        //GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, mBufferDots);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribDivisor(0, 1);
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_BYTE, false, 0, mBufferQuad);
        GLES30.glEnableVertexAttribArray(1);
        GLES30.glVertexAttribDivisor(1, 0);
        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLE_STRIP, 0, 4, DOT_COUNT);
        GLES30.glDisableVertexAttribArray(0);
        GLES30.glDisableVertexAttribArray(1);
        GLES30.glVertexAttribDivisor(0, 0);
        //GLES30.glDisable(GLES30.GL_BLEND);
        //GLES30.glDepthMask(true);

    }

}
