package fi.harism.app.opengl3x.fragment.basic;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Toast;

import fi.harism.app.opengl3x.R;
import fi.harism.lib.opengl.egl.EglCore;
import fi.harism.lib.opengl.gl.GlProgram;
import fi.harism.lib.opengl.gl.GlUtils;

public class CubeBasicRendererFragment extends BasicRendererFragment {

    private static final String RENDERER_ID = "renderer.basic.cube";

    private GlProgram glProgram;

    private long lastRenderTime;

    private final float projectionMatrix[] = new float[16];
    private final float lookAtMatrix[] = new float[16];
    private final float rotationMatrix[] = new float[16];
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
        return R.string.renderer_basic_cube_title;
    }

    @Override
    public int getCaptionStringId() {
        return R.string.renderer_basic_cube_caption;
    }

    @Override
    public void onSurfaceCreated() {
        super.onSurfaceCreated();

        try {
            final String SOURCE_VS = GlUtils.loadString(getActivity(), "shaders/basic/cube/shader_vs.txt");
            final String SOURCE_FS = GlUtils.loadString(getActivity(), "shaders/basic/cube/shader_fs.txt");
            glProgram = new GlProgram(SOURCE_VS, SOURCE_FS, null).useProgram();
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
        float aspect = (float) width / height;
        Matrix.perspectiveM(projectionMatrix, 0, 60.0f, aspect, 1f, 100f);
        Matrix.setLookAtM(lookAtMatrix, 0, 0f, 0f, 4f, 0f, 0f, 0f, 0f, 1f, 0f);
        Matrix.setIdentityM(rotationMatrix, 0);
    }

    @Override
    public void onRenderFrame() {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        glProgram.useProgram();

        long time = SystemClock.uptimeMillis();
        float diff = (time - lastRenderTime) / 10f;
        lastRenderTime = time;

        Matrix.rotateM(rotationMatrix, 0, diff, 1f, 1.5f, 0f);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, lookAtMatrix, 0, rotationMatrix, 0);

        GLES30.glUniformMatrix4fv(glProgram.getUniformLocation("uModelViewMatrix"), 1, false, modelViewProjectionMatrix, 0);

        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewProjectionMatrix, 0);

        GLES30.glUniformMatrix4fv(glProgram.getUniformLocation("uModelViewProjectionMatrix"), 1, false, modelViewProjectionMatrix, 0);

        renderCube();
    }

    @Override
    public void onSurfaceReleased() {

    }
}
