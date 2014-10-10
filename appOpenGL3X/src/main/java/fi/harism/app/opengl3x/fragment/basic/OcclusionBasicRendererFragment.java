package fi.harism.app.opengl3x.fragment.basic;

import android.app.Fragment;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import fi.harism.app.opengl3x.R;
import fi.harism.lib.opengl.egl.EglCore;
import fi.harism.lib.opengl.gl.GlProgram;
import fi.harism.lib.opengl.gl.GlQuery;
import fi.harism.lib.opengl.gl.GlUtils;
import fi.harism.lib.opengl.model.GlCamera;

public class OcclusionBasicRendererFragment extends BasicRendererFragment {

    private static final String RENDERER_ID = "renderer.basic.occlusion";

    private static final String UNIFORM_NAMES[] = {"uModelViewMatrix", "uModelViewProjMatrix"};
    private final int uniformLocations[] = new int[UNIFORM_NAMES.length];

    private GlCamera glCamera;
    private GlProgram glProgram;

    private Model modelStaticCube;
    private Model modelMovingCube;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEglFlags(EglCore.FLAG_DEPTH_BUFFER);
    }

    @Override
    public Fragment getSettingsFragment() {
        return new SettingsFragment();
    }

    @Override
    public String getRendererId() {
        return RENDERER_ID;
    }

    @Override
    public int getTitleStringId() {
        return R.string.renderer_basic_occlusion_title;
    }

    @Override
    public int getCaptionStringId() {
        return R.string.renderer_basic_occlusion_caption;
    }

    @Override
    public void onSurfaceCreated() {
        super.onSurfaceCreated();

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glCullFace(GLES30.GL_BACK);
        GLES30.glFrontFace(GLES30.GL_CCW);

        glCamera = new GlCamera();

        modelStaticCube = new Model();
        modelMovingCube = new Model();

        try {
            glProgram = new GlProgram(
                    GlUtils.loadString(getActivity(), "shaders/basic/occlusion/shader_vs.txt"),
                    GlUtils.loadString(getActivity(), "shaders/basic/occlusion/shader_fs.txt"),
                    null).useProgram();
            glProgram.getUniformIndices(UNIFORM_NAMES, uniformLocations);
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
    }

    @Override
    public void onRenderFrame() {
        float rotation = (SystemClock.uptimeMillis() % 7000) / 7000f;
        Matrix.setIdentityM(modelStaticCube.modelMatrix, 0);
        Matrix.translateM(modelStaticCube.modelMatrix, 0, 0f, -1f, 0f);
        Matrix.rotateM(modelStaticCube.modelMatrix, 0, rotation * 360, 1f, 0f, 0f);
        Matrix.rotateM(modelStaticCube.modelMatrix, 0, rotation * 360, 0f, 2f, 0f);
        modelStaticCube.multiplyMVP(glCamera.viewMat(), glCamera.projMat());

        Matrix.setIdentityM(modelMovingCube.modelMatrix, 0);
        Matrix.translateM(modelMovingCube.modelMatrix, 0, (float) Math.sin(rotation * Math.PI * 2) * 5f, (float) Math.cos(rotation * Math.PI * 2), 0f);
        Matrix.rotateM(modelMovingCube.modelMatrix, 0, rotation * 360, 0f, 2f, 0f);
        Matrix.rotateM(modelMovingCube.modelMatrix, 0, rotation * 360, 0f, 0f, 1f);
        Matrix.scaleM(modelMovingCube.modelMatrix, 0, 0.3f, 0.3f, 0.3f);
        modelMovingCube.multiplyMVP(glCamera.viewMat(), glCamera.projMat());

        boolean movingCubeCulled = modelMovingCube.isCulled();
        boolean movingCubeOccluded = movingCubeCulled ? false : modelMovingCube.isOccluded();

        if (movingCubeCulled) {
            GLES30.glClearColor(0.3f, 0.1f, 0.1f, 1.0f);
        } else if (movingCubeOccluded) {
            GLES30.glClearColor(0.1f, 0.3f, 0.1f, 1.0f);
        } else {
            GLES30.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        }
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        glProgram.useProgram();

        GLES30.glUniformMatrix4fv(uniformLocations[0], 1, false, modelStaticCube.modelViewMatrix, 0);
        GLES30.glUniformMatrix4fv(uniformLocations[1], 1, false, modelStaticCube.modelViewProjMatrix, 0);
        GLES30.glUniform3f(glProgram.getUniformLocation("uColor"), 1f, 1f, 1f);
        renderCubeFilled();

        GLES30.glUniformMatrix4fv(uniformLocations[0], 1, false, modelMovingCube.modelViewMatrix, 0);
        GLES30.glUniformMatrix4fv(uniformLocations[1], 1, false, modelMovingCube.modelViewProjMatrix, 0);
        GLES30.glUniform3f(glProgram.getUniformLocation("uColor"), 1f, 1f, 1f);

        if (!movingCubeCulled) {
            modelMovingCube.glQuery.begin(GLES30.GL_ANY_SAMPLES_PASSED);
            if (!movingCubeOccluded) {
                renderCubeFilled();
            } else {
                GLES30.glColorMask(false, false, false, false);
                GLES30.glDepthMask(false);
                renderCubeFilled();
                GLES30.glColorMask(true, true, true, true);
                GLES30.glDepthMask(true);
            }
            modelMovingCube.glQuery.end(GLES30.GL_ANY_SAMPLES_PASSED);
        }
    }

    @Override
    public void onSurfaceReleased() {
    }

    private class Model {
        public final float modelMatrix[] = new float[16];
        public final float modelViewMatrix[] = new float[16];
        public final float modelViewProjMatrix[] = new float[16];

        private final int cullResult[] = {0};

        private final GlQuery glQuery = new GlQuery();
        private final int queryResult[] = {0};

        public boolean isCulled() {
            return Visibility.frustumCullSpheres(modelViewProjMatrix, 0,
                    getObjectCube().bsphere(), 0, 1,
                    cullResult, 0, 1) == 0;
        }

        public boolean isOccluded() {
            glQuery.getObjectuiv(GLES30.GL_QUERY_RESULT, queryResult);
            return queryResult[0] == 0;
        }

        public void multiplyMVP(float viewMatrix[], float projMatrix[]) {
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(modelViewProjMatrix, 0, projMatrix, 0, modelViewMatrix, 0);
        }

    }

    public static class SettingsFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_settings_basic_occlusion, container, false);
            return view;
        }

    }

}
