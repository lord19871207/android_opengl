package fi.harism.app.blackandwhite;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import fi.harism.lib.opengl.gl.GlProgram;
import fi.harism.lib.opengl.gl.GlUtils;
import fi.harism.lib.opengl.util.GlRenderer;

public class EffectRenderer2 implements GlRenderer {

    private static final String TAG = EffectRenderer2.class.getSimpleName();

    private Size size;
    private Context context;
    private GlProgram glProgramTriangle;
    private FloatBuffer bufferTriangle;

    private final float[] projMat = new float[16];
    private final float[] modelViewMat = new float[16];

    private int inPosition;
    private int uModelViewMat;
    private int uProjMat;
    private int uAngle;
    private int uCount;
    private int uTime;

    EffectRenderer2(Context context) {
        this.context = context;
        final float SQRT3_PER3 = (float) Math.sqrt(3.0) / 3f;
        final float[] TRIANGLE = {0f, 2 * SQRT3_PER3, -1f, -SQRT3_PER3, 1f, -SQRT3_PER3};
        bufferTriangle = ByteBuffer.allocateDirect(4 * 6)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(TRIANGLE);
        bufferTriangle.position(0);
    }

    @Override
    public void onSurfaceCreated() {
        try {
            String vs = GlUtils.loadString(context, "shaders/effect2/triangle.vs");
            String fs = GlUtils.loadString(context, "shaders/effect2/triangle.fs");
            glProgramTriangle = new GlProgram(vs, fs, null).useProgram();
            inPosition = glProgramTriangle.getAttribLocation("inPosition");
            uModelViewMat = glProgramTriangle.getUniformLocation("uModelViewMat");
            uProjMat = glProgramTriangle.getUniformLocation("uProjMat");
            uAngle = glProgramTriangle.getUniformLocation("uAngle");
            uCount = glProgramTriangle.getUniformLocation("uCount");
            uTime = glProgramTriangle.getUniformLocation("uTime");
        } catch (Exception ex) {
            Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
            Log.d(TAG, ex.toString());
        }
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        size = new Size(width, height);
        float aspect = (float) height / width;
        Matrix.orthoM(projMat, 0, -1f, 1f, -aspect, aspect, 1f, 100f);
    }

    @Override
    public void onRenderFrame() {
        GLES30.glViewport(0, 0, size.getWidth(), size.getHeight());
        GLES30.glClearColor(1f, 1f, 1f, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);

        glProgramTriangle.useProgram();
        GLES30.glVertexAttribPointer(inPosition, 2, GLES30.GL_FLOAT, false, 0, bufferTriangle);
        GLES30.glEnableVertexAttribArray(inPosition);

        float time = SystemClock.uptimeMillis() % 24986 / 24986f;
        Matrix.setIdentityM(modelViewMat, 0);
        Matrix.rotateM(modelViewMat, 0, 360f * time, 0, 0, 1);
        Matrix.translateM(modelViewMat, 0, 0f, 0f, -2f);
        GLES30.glUniformMatrix4fv(uModelViewMat, 1, false, modelViewMat, 0);
        GLES30.glUniformMatrix4fv(uProjMat, 1, false, projMat, 0);

        time = SystemClock.uptimeMillis() % 89583 / 89583f;
        for (int jj = 0; jj < 4; ++jj) {
            double angle = 2.0 * Math.PI * jj / 4.0;
            GLES30.glUniform1f(uAngle, (float) angle);
            GLES30.glUniform1f(uCount, (float) 400);
            GLES30.glUniform1f(uTime, time);
            GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLE_STRIP, 0, 3, 400);
        }
    }

    @Override
    public void onSurfaceReleased() {

    }

}
