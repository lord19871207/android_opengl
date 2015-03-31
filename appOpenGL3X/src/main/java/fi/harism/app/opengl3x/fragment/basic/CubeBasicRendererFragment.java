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
import fi.harism.lib.opengl.model.GlCamera;

public class CubeBasicRendererFragment extends BasicRendererFragment {

    private static final String RENDERER_ID = "renderer.basic.cube";

    private static final boolean showOutlines = true;

    private GlCamera glCamera;
    private GlProgram glProgram;

    private long lastRenderTime;

    private final float rotationMat[] = new float[16];
    private final float modelViewMat[] = new float[16];
    private final float modelViewProjMat[] = new float[16];

    private final Uniforms uniforms = new Uniforms();

    private final class Uniforms {
        public int uModelViewMat;
        public int uModelViewProjMat;
        public int uColor;
    }

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

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glCullFace(GLES30.GL_BACK);
        GLES30.glFrontFace(GLES30.GL_CCW);

        glCamera = new GlCamera();

        try {
            glProgram = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/basic/cube/shader_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/basic/cube/shader_fs.txt"),
                    null).useProgram().getUniformIndices(uniforms);
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
        glCamera.setPerspective(width, height, 60.0f, 1f, 100f);
        glCamera.setPos(new float[]{0f, 0f, 4f});
        Matrix.setIdentityM(rotationMat, 0);
    }

    @Override
    public void onRenderFrame() {
        GLES30.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        glProgram.useProgram();

        long time = SystemClock.uptimeMillis();
        float diff = (time - lastRenderTime) / 1000f;
        lastRenderTime = time;

        Matrix.rotateM(rotationMat, 0, diff * 45f, 1f, 1.5f, 0f);
        Matrix.multiplyMM(modelViewMat, 0, glCamera.viewMat(), 0, rotationMat, 0);
        Matrix.multiplyMM(modelViewProjMat, 0, glCamera.projMat(), 0, modelViewMat, 0);

        GLES30.glUniformMatrix4fv(uniforms.uModelViewMat, 1, false, modelViewMat, 0);
        GLES30.glUniformMatrix4fv(uniforms.uModelViewProjMat, 1, false, modelViewProjMat, 0);

        GLES30.glEnable(GLES30.GL_POLYGON_OFFSET_FILL);
        GLES30.glPolygonOffset(1f, 1f);
        GLES30.glUniform3f(uniforms.uColor, 1f, 1f, 1f);
        renderCubeFilled();
        GLES30.glDisable(GLES30.GL_POLYGON_OFFSET_FILL);

        if (showOutlines) {
            GLES30.glLineWidth(4f);
            GLES30.glUniform3f(uniforms.uColor, .4f, .6f, 1f);
            renderCubeOutlines();
        }
    }

    @Override
    public void onSurfaceReleased() {
    }

}
